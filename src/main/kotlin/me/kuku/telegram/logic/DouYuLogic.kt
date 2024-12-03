package me.kuku.telegram.logic

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.module.kotlin.contains
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import me.kuku.telegram.entity.DouYuEntity
import me.kuku.telegram.entity.DouYuService
import me.kuku.telegram.exception.qrcodeNotScanned
import me.kuku.telegram.utils.*
import org.springframework.stereotype.Service

@Service
class DouYuLogic(
    private val douYuService: DouYuService
) {

    private val referer = "https://passport.douyu.com/index/login?passport_reg_callback=PASSPORT_REG_SUCCESS_CALLBACK&passport_login_callback=PASSPORT_LOGIN_SUCCESS_CALLBACK&passport_close_callback=PASSPORT_CLOSE_CALLBACK&passport_dp_callback=PASSPORT_DP_CALLBACK&type=login&client_id=1&state=https%3A%2F%2Fwww.douyu.com%2F"

    suspend fun getQrcode(): DouYuQrcode {
        val jsonNode = client.submitForm("https://passport.douyu.com/scan/generateCode",
            parameters {
                append("client_id", "1")
                append("isMultiAccount", "0")
            }) {
            referer(referer)
        }.body<JsonNode>()
        if (jsonNode["error"].asInt() != 0) error(jsonNode["data"].asText())
        val data = jsonNode["data"]
        val url = data["url"].asText()
        val code = data["code"].asText()
        return DouYuQrcode(url, code)
    }


    suspend fun checkQrcode(douYuQrcode: DouYuQrcode): DouYuEntity {
        val checkResponse =
            client.get("https://passport.douyu.com/japi/scan/auth?time=${System.currentTimeMillis()}&code=${douYuQrcode.code}") {
                headers {
                    referer(referer)
                }
            }
        val jsonNode = checkResponse.body<JsonNode>()
        return when (jsonNode["error"].asInt()) {
            -2,1 -> qrcodeNotScanned()
            0 -> {
                val url = jsonNode["data"]["url"].asText()
                val response = client.get("${url}&callback=appClient_json_callback&_=${System.currentTimeMillis()}") {
                    referer("https://passport.douyu.com/")
                }
                val cookie = response.setCookie().renderCookieHeader() + checkResponse.setCookie().renderCookieHeader()
                DouYuEntity().also { it.cookie = cookie }
            }
            else -> error(jsonNode["data"].asText())
        }
    }

    private suspend fun renewCookie(douYuEntity: DouYuEntity) {
        var cookie = douYuEntity.cookie
        val jsonNode = client.get("https://www.douyu.com/member/panel/userInfo/getInfo") {
            headers {
                cookieString(cookie)
            }
        }.body<JsonNode>()
        if (jsonNode["error"].asInt() != 300002) return
        val response = client.get("https://passport.douyu.com/member/login?state=https%3A%2F%2Fwww.douyu.com%2Fmember%2Fcp") {
            headers {
                cookieString(cookie)
            }
        }
        if (response.status == HttpStatusCode.OK) error("登陆已失效，请重新登陆")
        val url = "https:${response.headers["location"]!!}"
        val authResponse = client.get(url) {
            headers {
                cookieString(cookie)
                referer("https://www.douyu.com/")
            }
        }
        val cookies = authResponse.setCookie()
        for (obj in cookies) {
            val name = obj.name
            val queryValue = cookies.find { it.name == name }?.value
            if (queryValue == null) cookie += "$name=${obj.value}; "
            else {
                cookie = cookie.replace(queryValue, obj.value)
            }
        }
        douYuEntity.cookie = cookie
        douYuService.save(douYuEntity)
    }

    suspend fun room(douYuEntity: DouYuEntity): List<DouYuRoom> {
        renewCookie(douYuEntity)
        var i = 1
        val resultList = mutableListOf<DouYuRoom>()
        while (true) {
            val jsonNode = client.get("https://www.douyu.com/wgapi/livenc/liveweb/follow/list?sort=0&cid1=0&page=${i++}") {
                cookieString(douYuEntity.cookie)
            }.body<JsonNode>()
            if (jsonNode["error"].asInt() == 0) {
                val list = jsonNode["data"]["list"] ?: break
                if (list is NullNode) break
                if (list.isEmpty) break
                for (singleJsonNode in list) {
                    val douYuRoom = DouYuRoom(singleJsonNode["room_name"].asText(), singleJsonNode["nickname"].asText(),
                        "https://www.douyu.com${singleJsonNode["url"].asText()}", singleJsonNode["game_name"].asText(), singleJsonNode["show_status"].asInt() == 1 && singleJsonNode["videoLoop"].asInt() == 0 ,
                        singleJsonNode["online"].asText(), singleJsonNode["room_id"].asLong(), singleJsonNode["room_src"].asText().replace("/dy1", ""))
                    resultList.add(douYuRoom)
                }
            } else error(jsonNode["msg"].asText())
        }
        return resultList
    }

    private suspend fun yuBaCookie(douYuEntity: DouYuEntity): String {
        val loginResponse = client.get("https://passport.douyu.com/lapi/passport/iframe/safeAuth?callback=jQuery111309004936224711857_1671594747590&client_id=5&did=&t=1671594747991&_=${System.currentTimeMillis()}") {
            headers {
                cookieString(douYuEntity.cookie)
                referer("https://yuba.douyu.com/")
            }
        }
        if (loginResponse.status != HttpStatusCode.Found) error("鱼吧签到失败，cookie已失效，请重新登录")
        val newUrl = loginResponse.headers["location"]!!
        val authResponse = client.get("https:$newUrl") {
            headers {
                cookieString(douYuEntity.cookie)
                referer("https://yuba.douyu.com/")
            }
        }
        return authResponse.setCookie().renderCookieHeader()
    }

    suspend fun fishGroup(douYuEntity: DouYuEntity) {
        val cookie = douYuEntity.cookie + yuBaCookie(douYuEntity)
        val jsonNode= client.get("https://yuba.douyu.com/wbapi/web/group/myFollow?page=1&limit=30&official=1&timestamp=${timestamp()}") {
            referer("https://yuba.douyu.com/allclassify/featurelist")
            cookieString(cookie)
        }.body<JsonNode>()
        val data = jsonNode["data"]
        if (jsonNode["status_code"].asInt() != 200) error(data.asText())
        for (node in data["list"]) {
            val id = node["group_id"].asInt()
            val infoResponse = client.get("https://yuba.douyu.com/wbapi/web/group/head?group_id=$id&timestamp=${timestamp()}") {
                headers {
                    cookieString(cookie)
                }
            }
            val infoNode = infoResponse.body<JsonNode>()
            val exp = infoNode["data"]["group_exp"].asInt()
            val isSign = infoNode["data"]["is_signed"].asInt()
            val setCookie = infoResponse.setCookie()
            val infoCookie = setCookie.renderCookieHeader()
            if (isSign == 0) {
                val signNode = client.submitForm("https://yuba.douyu.com/ybapi/topic/sign?timestamp=${timestamp()}",
                    parameters {
                        append("group_id", id.toString())
                        append("cur_exp", exp.toString())
                    }) {
                    headers {
                        cookieString(cookie + infoCookie)
                        referer("https://yuba.douyu.com/group/$id")
                        append("x-csrf-token", setCookie.find { it.name == "acf_yb_t" }?.value ?: "")
                    }
                }.bodyAsText().toJsonNode()
                if (signNode["status_code"].asInt() != 200) error(signNode["data"].asText())
            }
            delay(5000)
        }

    }

    private fun timestamp(): String {
        return System.currentTimeMillis().toString().substring(0, 8)
    }

    suspend fun focusFishGroup(douYuEntity: DouYuEntity): List<DouYuFish> {
        val yuBaCookie = yuBaCookie(douYuEntity)
        val jsonNode = client.get("https://yuba.douyu.com/wbapi/web/followfeed?last_id=0&pagesize=20&timestamp=${timestamp()}") {
            headers {
                cookieString(douYuEntity.cookie + yuBaCookie)
                referer("https://yuba.douyu.com/homepage/main")
            }
        }.body<JsonNode>()
        if (jsonNode["status_code"].asInt() != 200) error(jsonNode["message"].asText())
        val dataNode = jsonNode["data"]["list"]
        val list = mutableListOf<DouYuFish>()
        for (node in dataNode) {
            val id = node["feed_id"].asLong()
            val url = node["share_url"].asText()
            val nickname = node["nick_name"].asText()
            val uid = node["uid"].asLong()
            val ownerContent = node["content"].asText()
            val douYuFish = DouYuFish(id, url, nickname, uid, ownerContent)
            var post: JsonNode? = null
            if (node.contains("post")) {
                post = node["post"]
            }
            if (node.has("source_feed")) {
                val feed = node["source_feed"]
                post = feed["post"]
            }
            if (post != null) {
                val title = post["title"].asText()
                val content = post["content"].asText()
                douYuFish.title = title
                douYuFish.content = content
                if (post.has("imglist")) {
                    post["imglist"].forEach { douYuFish.image.add(it["url"].asText()) }
                }
                if (post.has("video")) {
                    post["video"].forEach { douYuFish.image.add(it["player"].asText()) }
                }
            } else douYuFish.content = douYuFish.ownerContent
            list.add(douYuFish)
        }
        return list
    }

}

data class DouYuQrcode(val url: String, val code: String)

data class DouYuRoom(val name: String, val nickName: String, val url: String, val gameName: String, val showStatus: Boolean, val online: String, val roomId: Long, val imageUrl: String)

data class DouYuFish(val id: Long, val url: String, val nickname: String, val uid: Long, val ownerContent: String,
                     var title: String = "", var content: String = "",
                     val image: MutableList<String> = mutableListOf(), val video: MutableList<String> = mutableListOf()
)
