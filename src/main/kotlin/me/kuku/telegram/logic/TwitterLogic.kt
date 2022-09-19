package me.kuku.telegram.logic

import me.kuku.telegram.entity.TwitterEntity
import me.kuku.utils.MyUtils
import me.kuku.utils.OkHttpKtUtils
import me.kuku.utils.OkUtils

object TwitterLogic {

    private fun Map<String, String>.auth(): MutableMap<String, String> {
        val map = this.toMutableMap()
        map["authorization"] = "Bearer AAAAAAAAAAAAAAAAAAAAANRILgAAAAAAnNwIzUejRCOuH5E6I8xnZz4puTs%3D1Zv7ttfk8LF81IUq16cHjhLTvJu4FA33AGWWjCpTnA"
        map["accept-language"] = "zh-CN,zh;q=0.9"
        return map
    }

    suspend fun login(username: String, password: String): TwitterEntity {
        val guestJsonNode = OkHttpKtUtils.postJson("https://api.twitter.com/1.1/guest/activate.json", mapOf(),
            mapOf<String, String>().auth())
        val guestToken = guestJsonNode["guest_token"].asText()
        val headers = mapOf("x-guest-token" to guestToken).auth()
        val flowJsonNode = OkHttpKtUtils.postJson("https://twitter.com/i/api/1.1/onboarding/task.json?flow_name=login", OkUtils.json("""
            {"input_flow_data":{"flow_context":{"debug_overrides":{},"start_location":{"location":"unknown"}}},"subtask_versions":{"action_list":2,"alert_dialog":1,"app_download_cta":1,"check_logged_in_account":1,"choice_selection":3,"contacts_live_sync_permission_prompt":0,"cta":7,"email_verification":2,"end_flow":1,"enter_date":1,"enter_email":2,"enter_password":5,"enter_phone":2,"enter_recaptcha":1,"enter_text":5,"enter_username":2,"generic_urt":3,"in_app_notification":1,"interest_picker":3,"js_instrumentation":1,"menu_dialog":1,"notifications_permission_prompt":2,"open_account":2,"open_home_timeline":1,"open_link":1,"phone_verification":4,"privacy_options":1,"security_key":3,"select_avatar":4,"select_banner":2,"settings_list":7,"show_code":1,"sign_up":2,"sign_up_review":4,"tweet_selection_urt":1,"update_users":1,"upload_media":1,"user_recommendations_list":4,"user_recommendations_urt":1,"wait_spinner":3,"web_modal":1}}
        """.trimIndent()), headers)
        var flowToken = flowJsonNode["flow_token"].asText()
        val str = OkHttpKtUtils.getStr("https://twitter.com/i/js_inst?c_name=ui_metrics")
        val json = "{" + MyUtils.regex("return \\{", ";\\};", str)
        val taskNode = OkHttpKtUtils.postJson("https://twitter.com/i/api/1.1/onboarding/task.json", OkUtils.json("""
            {"flow_token":"$flowToken","subtask_inputs":[{"subtask_id":"LoginJsInstrumentationSubtask","js_instrumentation":{"response":"$json","link":"next_link"}}]}
        """.trimIndent()), headers)
        flowToken = taskNode["flow_token"].asText()
        val usernameJsonNode = OkHttpKtUtils.postJson("https://twitter.com/i/api/1.1/onboarding/task.json", OkUtils.json("""
            {"flow_token":"$flowToken","subtask_inputs":[{"subtask_id":"LoginEnterUserIdentifierSSO","settings_list":{"setting_responses":[{"key":"user_identifier","response_data":{"text_data":{"result":"$username"}}}],"link":"next_link"}}]}
        """.trimIndent()), headers)
        flowToken = usernameJsonNode["flow_token"].asText()
        val loginJsonNode = OkHttpKtUtils.postJson("https://twitter.com/i/api/1.1/onboarding/task.json", OkUtils.json("""
            {"flow_token":"$flowToken","subtask_inputs":[{"subtask_id":"LoginEnterPassword","enter_password":{"password":"$password","link":"next_link"}}]}
        """.trimIndent()), headers)
        return if (loginJsonNode["status"]?.asText() == "success") {
            flowToken = loginJsonNode["flow_token"].asText()
            val response = OkHttpKtUtils.post("https://twitter.com/i/api/1.1/onboarding/task.json", OkUtils.json("""
                {"flow_token":"$flowToken","subtask_inputs":[{"subtask_id":"AccountDuplicationCheck","check_logged_in_account":{"link":"AccountDuplicationCheck_false"}}]}
            """.trimIndent()), headers)
            response.close()
            val cookie = OkUtils.cookie(response)
            println(cookie)
            val ct = OkUtils.cookie(cookie, "ct0")!!
            val checkResponse = OkHttpKtUtils.get("https://twitter.com/i/api/graphql/4jeP7HyKpQUitFUTWedrqA/Viewer?variables=%7B%22withCommunitiesMemberships%22%3Atrue%2C%22withCommunitiesCreation%22%3Atrue%2C%22withSuperFollowsUserFields%22%3Atrue%7D&features=%7B%22responsive_web_graphql_timeline_navigation_enabled%22%3Afalse%7D",
                headers.also {
                    it["cookie"] = cookie
                    it["x-csrf-token"] = ct
                })
            val checkJsonNode = OkUtils.json(checkResponse)
            val resultJsonNode = checkJsonNode["data"]["viewer"]["user_results"]["result"]
            val id = resultJsonNode["id"].asText()
            val restId = resultJsonNode["rest_id"].asText()
            val ctCookie = OkUtils.cookie(checkResponse)
            val resCookie = cookie.replace("ct0=$ct;", "ct0=${OkUtils.cookie(ctCookie, "ct0")}")
            TwitterEntity().also {
                it.cookie = resCookie
                it.tId = id
                it.tRestId = restId
            }
        } else error(loginJsonNode["errors"][0]["message"].asText())
    }

}