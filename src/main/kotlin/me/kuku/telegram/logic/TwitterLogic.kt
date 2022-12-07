@file:Suppress("DuplicatedCode")

package me.kuku.telegram.logic

import com.fasterxml.jackson.databind.JsonNode
import me.kuku.telegram.entity.TwitterEntity
import me.kuku.utils.MyUtils
import me.kuku.utils.OkHttpKtUtils
import me.kuku.utils.OkUtils
import org.jsoup.Jsoup

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
            val ct0 = OkUtils.cookie(ctCookie, "ct0")!!
            val resCookie = cookie.replace("ct0=$ct;", "ct0=$ct0;")
            TwitterEntity().also {
                it.cookie = resCookie
                it.tId = id
                it.tRestId = restId
                it.csrf = ct0
            }
        } else error(loginJsonNode["errors"][0]["message"].asText())
    }

    private fun Map<String, String>.auth(twitterEntity: TwitterEntity): MutableMap<String, String> {
        val map = auth()
        map["cookie"] = twitterEntity.cookie
        map["x-csrf-token"] = twitterEntity.csrf
        return map
    }

    private fun convert(jsonNode: JsonNode): TwitterPojo? {
        val content = jsonNode["content"]["itemContent"]
        if (content?.get("socialContext") != null) return null
        val result = content?.get("tweet_results")?.get("result") ?: return null
        val legacy = result["legacy"] ?: return null
        val createdAt = legacy["created_at"].asText()
        val text = legacy["full_text"].asText()
        val userid = legacy["user_id_str"].asLong()
        val userResult = result["core"]["user_results"]["result"]
        val userLegacy = userResult["legacy"]
        val username = userLegacy["name"].asText()
        val screenName = userLegacy["screen_name"].asText()
        val source = Jsoup.parse(legacy["source"].asText()).text()
        val id = legacy["id_str"].asLong()
        val pojo = TwitterPojo(id, createdAt, text, userid, username, screenName, source)
        legacy["extended_entities"]?.get("media")?.let {
            for (node in it) {
                val type = node["type"].asText()
                if (type == "photo") {
                    pojo.photoList.add(node["media_url_https"].asText())
                }
                if (type == "video") {
                    val url = node["video_info"]["variants"][0]["url"].asText()
                    pojo.videoList.add(url)
                }
            }
        }
        result["quoted_status_result"]?.let {
            pojo.forward = true
            val quotedResult = it["result"]
            val forwardId = quotedResult["rest_id"].asLong()
            val forwardUserResult = quotedResult["core"]["user_results"]["result"]
            val forwardUserid = forwardUserResult["rest_id"].asLong()
            val forwardUserLegacy = forwardUserResult["legacy"]
            val forwardUsername = forwardUserLegacy["name"].asText()
            val forwardScreenName = forwardUserLegacy["screen_name"].asText()
            val quotedLegacy = quotedResult["legacy"]
            val forwardText = quotedLegacy["full_text"].asText()
            val forwardCreatedAt = quotedLegacy["created_at"].asText()
            val forwardSource = Jsoup.parse(quotedLegacy["source"].asText()).text()
            pojo.forwardId = forwardId
            pojo.forwardCreatedAt = forwardCreatedAt
            pojo.forwardText = forwardText
            pojo.forwardUserid = forwardUserid
            pojo.forwardUsername = forwardUsername
            pojo.forwardScreenName = forwardScreenName
            pojo.forwardSource = forwardSource
            quotedLegacy["extended_entities"]?.get("media")?.let { media ->
                for (node in media) {
                    val type = node["type"].asText()
                    if (type == "photo") {
                        pojo.forwardPhotoList.add(node["media_url_https"].asText())
                    }
                    if (type == "video") {
                        val url = node["video_info"]["variants"][0]["url"].asText()
                        pojo.forwardVideoList.add(url)
                    }
                }
            }
        }
        return pojo
    }

    suspend fun friendTweet(twitterEntity: TwitterEntity): List<TwitterPojo> {
        val response = OkHttpKtUtils.get("https://twitter.com/i/api/graphql/xDH0v9kM5QTSTNtbAke9TQ/HomeTimeline?variables=%7B%22count%22%3A20%2C%22includePromotedContent%22%3Afalse%2C%22latestControlAvailable%22%3Atrue%2C%22withCommunity%22%3Atrue%2C%22withSuperFollowsUserFields%22%3Atrue%2C%22withDownvotePerspective%22%3Afalse%2C%22withReactionsMetadata%22%3Afalse%2C%22withReactionsPerspective%22%3Afalse%2C%22withSuperFollowsTweetFields%22%3Atrue%7D&features=%7B%22responsive_web_graphql_timeline_navigation_enabled%22%3Afalse%2C%22unified_cards_ad_metadata_container_dynamic_card_content_query_enabled%22%3Afalse%2C%22dont_mention_me_view_api_enabled%22%3Atrue%2C%22responsive_web_uc_gql_enabled%22%3Atrue%2C%22vibe_api_enabled%22%3Atrue%2C%22responsive_web_edit_tweet_api_enabled%22%3Atrue%2C%22graphql_is_translatable_rweb_tweet_is_translatable_enabled%22%3Afalse%2C%22standardized_nudges_misinfo%22%3Atrue%2C%22tweet_with_visibility_results_prefer_gql_limited_actions_policy_enabled%22%3Afalse%2C%22interactive_text_enabled%22%3Atrue%2C%22responsive_web_text_conversations_enabled%22%3Afalse%2C%22responsive_web_enhance_cards_enabled%22%3Atrue%7D",
            mapOf<String, String>().auth(twitterEntity)
        )
        if (response.code != 200) error("cookie已失效")
        val jsonNode = OkUtils.json(response)
        val list = mutableListOf<TwitterPojo>()
        val arrayNode = jsonNode["data"]["home"]["home_timeline_urt"]["instructions"][0]["entries"]
        for (node in arrayNode) {
            convert(node)?.let {
                list.add(it)
            }
        }
        return list
    }

    fun convertStr(twitterPojo: TwitterPojo): String {
        val sb = StringBuilder()
        sb.appendLine("#${twitterPojo.username}")
            .appendLine("发布时间：${twitterPojo.createdAt}")
            .appendLine("内容：${twitterPojo.text}")
            .append("链接：${twitterPojo.url()}")
        if (twitterPojo.forward) {
            sb.appendLine()
                .appendLine("转发自：#${twitterPojo.forwardUsername}")
                .appendLine("发布时间：${twitterPojo.forwardCreatedAt}")
                .appendLine("内容：${twitterPojo.forwardText}")
                .append("链接：${twitterPojo.forwardUrl()}")
        }
        return sb.toString()
    }

}

data class TwitterPojo(val id: Long, val createdAt: String, val text: String, val userid: Long, val username: String, val screenName: String, val source: String,
                       var forward: Boolean = false, var forwardId: Long = 0, var forwardCreatedAt: String = "", var forwardText: String = "",
                       var forwardUserid: Long = 0, var forwardUsername: String = "", var forwardScreenName: String = "", var forwardSource: String = "") {
    val photoList = mutableListOf<String>()
    val videoList = mutableListOf<String>()
    val forwardPhotoList = mutableListOf<String>()
    val forwardVideoList = mutableListOf<String>()

    fun url() = "https://twitter.com/$screenName/status/$id"

    fun forwardUrl() = "https://twitter.com/$forwardScreenName/status/$forwardId"
}
