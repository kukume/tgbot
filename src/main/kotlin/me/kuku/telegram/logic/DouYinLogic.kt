package me.kuku.telegram.logic

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import me.kuku.pojo.CommonResult
import me.kuku.telegram.config.api
import me.kuku.telegram.entity.DouYinEntity
import me.kuku.utils.*
import okhttp3.Response

object DouYinLogic {

    private suspend fun request(url: String, cookie: String = ""): Response {
        val jsonNode = OkHttpKtUtils.postJson("$api/exec/douYin", mapOf("url" to url))
        val newUrl = jsonNode["url"].asText()
        val userAgent = jsonNode["userAgent"].asText()
        return OkHttpKtUtils.get(newUrl, mapOf("user-agent" to userAgent, "referer" to "https://www.douyin.com/user", "cookie" to cookie))
    }

    private suspend fun requestJson(url: String, cookie: String = ""): JsonNode {
        val response = request(url, cookie)
        return OkUtils.json(response)
    }

    suspend fun qrcode(): DouYinQrcode {
        val response = OkHttpKtUtils.get("https://www.douyin.com").apply { close() }
        var cookie = OkUtils.cookie(response)
        val nonce = OkUtils.cookie(cookie, "__ac_nonce")!!
        val signJsonNode = OkHttpKtUtils.postJson("$api/exec/douYinSign", mapOf("nonce" to nonce))
        val sign = signJsonNode["sign"].asText()
        cookie += "__ac_signature=$sign; "
        val ssResponse = OkHttpKtUtils.get("https://www.douyin.com", OkUtils.cookie(cookie)).apply { close() }
        val ssCookie = OkUtils.cookie(ssResponse)
        cookie += ssCookie
        val jsonNode =
            requestJson("https://sso.douyin.com/get_qrcode/?service=https%3A%2F%2Fwww.douyin.com&need_logo=false&need_short_url=false&device_platform=web_app&aid=6383&account_sdk_source=sso&sdk_version=2.1.3&language=zh&verifyFp=verify_l7wwibte_GVTKXe6k_en2r_4afu_BBfz_ShKF17XQqjyq",
                cookie)
        val data = jsonNode["data"]
        val base = data["qrcode"].asText()
        val token = data["token"].asText()
        return DouYinQrcode(base, token, cookie)
    }

    suspend fun checkQrcode(douYinQrcode: DouYinQrcode): CommonResult<DouYinEntity> {
        val response =
            request("https://sso.douyin.com/check_qrconnect/?service=https%3A%2F%2Fwww.douyin.com&token=${douYinQrcode.token}&need_logo=false&is_frontier=false&need_short_url=true&device_platform=web_app&aid=6383&account_sdk_source=sso&sdk_version=2.1.3&language=zh&verifyFp=verify_l7wwibte_GVTKXe6k_en2r_4afu_BBfz_ShKF17XQqjyq",
                douYinQrcode.cookie)
        val firstCookie = OkUtils.cookie(response)
        val jsonNode = OkUtils.json(response)
        val data = jsonNode["data"]
        return when (data["status"].asInt()) {
            3 -> {
                val redirectUrl = data["redirect_url"].asText()
                val secondResponse = OkHttpKtUtils.get(redirectUrl, OkUtils.cookie(firstCookie)).apply { close() }
                val thirdUrl = secondResponse.header("location")!!
                val thirdResponse = OkHttpKtUtils.get(thirdUrl, OkUtils.cookie(firstCookie)).apply { close() }
                val resCookie = OkUtils.cookie(thirdResponse)
                val html = OkHttpKtUtils.getStr(
                    "https://www.douyin.com/?is_new_connect=0&is_new_user=0",
                    OkUtils.headers(resCookie + douYinQrcode.cookie, "https://www.douyin.com/",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                )
                val secUserid = MyUtils.regex("//www.douyin.com/user/", "\\?", html)
                val userid = MyUtils.regex("uid%22%3A%22", "%22%2C%22sec", html)
                val entity = DouYinEntity().also {
                    it.cookie = resCookie + douYinQrcode.cookie
                    it.userid = userid!!.toLong()
                    it.secUserid = secUserid!!
                }
                CommonResult.success(entity)
            }
            5 -> CommonResult.fail("二维码已失效")
            1, 2 -> CommonResult.fail(code = 0, message = "等待扫描或已扫描")
            else -> CommonResult.fail("未知错误")
        }
    }

    suspend fun follow(douYinEntity: DouYinEntity, offset: Int = 0, size: Int = 20): List<DouYinUser> {
        val jsonNode = requestJson("https://www.douyin.com/aweme/v1/web/user/following/list/?device_platform=webapp&aid=6383&channel=channel_pc_web&user_id=${douYinEntity.userid}&sec_user_id=${douYinEntity.secUserid}&offset=$offset&min_time=0&max_time=0&count=$size&source_type=4&gps_access=0&address_book_access=0&is_top=1&pc_client_type=1&version_code=170400&version_name=17.4.0&cookie_enabled=true&screen_width=1707&screen_height=1067&browser_language=zh-CN&browser_platform=Win32&browser_name=Chrome&browser_version=105.0.0.0&browser_online=true&engine_name=Blink&engine_version=105.0.0.0&os_name=Windows&os_version=10&cpu_core_num=20&device_memory=8&platform=PC&downlink=1.45&effective_type=4g&round_trip_time=150&webid=7141988625902159372",
            douYinEntity.cookie)
        val list = mutableListOf<DouYinUser>()
        val followJsonNode = jsonNode["followings"]
        for (node in followJsonNode) {
            val name = node["nickname"].asText()
            val secUid = node["sec_uid"].asText()
            val uid = node["uid"].asLong()
            list.add(DouYinUser(uid, secUid, name))
        }
        return list
    }

    private suspend fun allFollow(douYinEntity: DouYinEntity): List<DouYinUser> {
        val list = mutableListOf<DouYinUser>()
        var i = 0
        while (true) {
            val followList = follow(douYinEntity, i++)
            list.addAll(followList)
            if (followList.size < 20) break
        }
        return list
    }

    suspend fun work(douYinEntity: DouYinEntity, douYinUser: DouYinUser): List<DouYinWork> {
        val html = OkHttpKtUtils.getStr("https://www.douyin.com/user/${douYinUser.secUid}", OkUtils.cookie(douYinEntity.cookie))
        val jsonNode = MyUtils.regex("application/json\">", "</sc", html)?.toUrlDecode()?.toJsonNode() ?: error("访问频繁")
        val out = jsonNode.iterator().run {
            next()
            next()
        }
        val data = out["post"]["data"]
        val list = mutableListOf<DouYinWork>()
        for (node in data) {
            val desc = node.getString("desc")
            val videoList = node["video"]["playAddr"].map { "https:" + it.getString("src") }
            val coverList = node["video"]["coverUrlList"].map { it.asText() }
            val musicList = node["music"]["playUrl"]["urlList"].map { it.asText() }
            val id = node.getLong("awemeId")
            val time = "${node["createTime"].asText()}000".toLong()
            val nickname = node["authorInfo"]["nickname"].asText()
            list.add(DouYinWork(desc, id, nickname, time, coverList, videoList, musicList))
        }
        return list
    }

    suspend fun followWork(douYinEntity: DouYinEntity): List<DouYinWork> {
        val followList = allFollow(douYinEntity)
        val list = mutableListOf<DouYinWork>()
        for (douYinUser in followList) {
            delay(1000)
            val work = work(douYinEntity, douYinUser)
            list.addAll(work)
        }
        list.sortBy { -it.id }
        return list
    }

    suspend fun recommend(douYinEntity: DouYinEntity): List<DouYinWork> {
        val jsonNode = requestJson("https://www.douyin.com/aweme/v1/web/tab/feed/?device_platform=webapp&aid=6383&channel=channel_pc_web&tag_id=&ug_source=&creative_id=&count=10&refresh_index=1&video_type_select=1&aweme_pc_rec_raw_data=%7B%7D&globalwid=&version_code=170400&version_name=17.4.0&cookie_enabled=true&screen_width=1707&screen_height=1067&browser_language=zh-CN&browser_platform=Win32&browser_name=Chrome&browser_version=105.0.0.0&browser_online=true&engine_name=Blink&engine_version=105.0.0.0&os_name=Windows&os_version=10&cpu_core_num=20&device_memory=8&platform=PC&downlink=10&effective_type=4g&round_trip_time=200&pc_client_type=1",
            douYinEntity.cookie)
//        val jsonNode = OkHttpKtUtils.getJson("https://www.douyin.com/aweme/v1/web/tab/feed/?device_platform=webapp&aid=6383&channel=channel_pc_web&tag_id=&ug_source=&creative_id=&count=10&refresh_index=1&video_type_select=1&aweme_pc_rec_raw_data=%7B%7D&globalwid=&version_code=170400&version_name=17.4.0&cookie_enabled=true&screen_width=1707&screen_height=1067&browser_language=zh-CN&browser_platform=Win32&browser_name=Chrome&browser_version=105.0.0.0&browser_online=true&engine_name=Blink&engine_version=105.0.0.0&os_name=Windows&os_version=10&cpu_core_num=20&device_memory=8&platform=PC&downlink=10&effective_type=4g&round_trip_time=200&pc_client_type=1&msToken=OQFr2AHDHWg261GyBauEzSZL_Vihal1zSEBz6MNrjMVy1fDxGpcZrc1HuGDPZm7NED5h7L1HUmFWw_MtermCZoP0Wo-HjPmcLoLx8jPIKYiKKnFD52jD9sWBYFhUiF433g==&X-Bogus=DFSzswVYyfzANaOPSQP/RM9WX7jP",
//            OkUtils.headers(douYinEntity.cookie, "https://www.douyin.com/?enter=guide", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36"))
        val we = jsonNode["aweme_list"]
        val list = mutableListOf<DouYinWork>()
        for (node in we) {
            val desc = node["desc"].asText()
            val id = node.getLong("aweme_id")
            val musicList = node?.get("music")?.get("play_url")?.get("url_list")?.map { it.asText() } ?: listOf()
            val videoList = node["video"]["bit_rate"][0]["play_addr"]["url_list"].map { it.asText() }
            val coverUrlList = node["video"]["cover"]["url_list"].map { it.asText() }
            val time = "${node["createTime"].asText()}000".toLong()
            val nickname = node["authorInfo"]["nickname"].asText()
            list.add(DouYinWork(desc, id, nickname, time, coverUrlList, videoList, musicList))
        }
        return list
    }

    suspend fun saveLoginInfo(douYinEntity: DouYinEntity): DouYinSaveLoginInfo {
        val infoNode = client.get("https://www.douyin.com/passport/user/web_record_status/set/?user_web_record_status=1&aid=6383&account_sdk_source=web&sdk_version=2.2.4-rc.1&verifyFp=verify_lbud0svf_odWpkMXY_pUAP_4A1j_AhjQ_xvyRGbpXusYm&fp=verify_lbud0svf_odWpkMXY_pUAP_4A1j_AhjQ_xvyRGbpXusYm") {
            headers {
                cookieString(douYinEntity.cookie)
            }
        }.body<JsonNode>()
        val infoDataNode = infoNode["data"]
        val errCode = infoDataNode["error_code"].asInt()
        if (errCode != 2046) error(infoDataNode["description"].asText())
        val ticket = infoDataNode["verify_ticket"].asText()
        val mobile = infoDataNode["verify_ways"][0]["mobile"].asText()
        val sendNode = client.get("https://www.douyin.com/passport/web/send_code/?verify_ticket=$ticket&language=zh&aid=6383&type=22&timestamp=${System.currentTimeMillis()}") {
            headers {
                cookieString(douYinEntity.cookie)
            }
        }.body<JsonNode>()
        if (sendNode["message"].asText() != "success") error("发送短信验证码失败（${sendNode["data"]["description"].asText()}），请重试")
        return DouYinSaveLoginInfo(ticket, mobile)
    }

    suspend fun verifySaveLoginInfo(douYinEntity: DouYinEntity, douYinSaveLoginInfo: DouYinSaveLoginInfo, code: String) {
        val response = client.get("https://www.douyin.com/passport/web/validate_code/?verify_ticket=${douYinSaveLoginInfo.ticket}&language=zh&aid=6383&code=$code&type=22&timestamp=${System.currentTimeMillis()}") {
            headers {
                cookieString(douYinEntity.cookie)
            }
        }
        val jsonNode = response.body<JsonNode>()
        if (jsonNode["message"].asText() != "success") error(jsonNode["data"]["description"].asText())
        val cookie = response.cookie()
        val resNode = client.get("https://www.douyin.com/passport/user/web_record_status/set/?user_web_record_status=1&aid=6383&account_sdk_source=web&sdk_version=2.2.4-rc.1&verifyFp=verify_lbud0svf_odWpkMXY_pUAP_4A1j_AhjQ_xvyRGbpXusYm&fp=verify_lbud0svf_odWpkMXY_pUAP_4A1j_AhjQ_xvyRGbpXusYm") {
            headers {
                cookieString(douYinEntity.cookie + cookie)
            }
        }.body<JsonNode>()
        if (resNode["message"].asText() != "success") error(resNode["data"]["description"].asText())
    }

}

data class DouYinQrcode(val baseImage: String, val token: String, val cookie: String)

data class DouYinUser(val uid: Long, val secUid: String, val name: String)

data class DouYinWork(val desc: String, val id: Long, val nickname: String, val createTime: Long, val coverUrlList: List<String>, val videoUrlList: List<String>, val musicUrlList: List<String>) {
    val url = "https://www.douyin.com/video/$id"
}

data class DouYinSaveLoginInfo(val ticket: String, val mobile: String)
