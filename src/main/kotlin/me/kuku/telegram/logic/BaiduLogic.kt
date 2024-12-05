package me.kuku.telegram.logic

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.delay
import me.kuku.telegram.entity.BaiduEntity
import me.kuku.telegram.entity.BaiduService
import me.kuku.telegram.exception.qrcodeNotScanned
import me.kuku.telegram.exception.qrcodeScanned
import me.kuku.telegram.utils.*
import org.jsoup.Jsoup
import org.springframework.stereotype.Service
import java.net.SocketTimeoutException
import java.util.*

@Service
class BaiduLogic (
    private val baiduService: BaiduService
) {

    suspend fun getQrcode(): BaiduQrcode {
        val uuid = UUID.randomUUID().toString()
        val jsonNode = client.get("https://passport.baidu.com/v2/api/getqrcode?lp=pc&qrloginfrom=pc&gid=$uuid&callback=tangram_guid_${System.currentTimeMillis()}&apiver=v3&tt=${System.currentTimeMillis()}&tpl=mn&logPage=traceId:pc_loginv5_1653405990,logPage:loginv5&_=${System.currentTimeMillis()}")
            .bodyAsText().jsonpToJsonNode()
        val url = "https://" + jsonNode["imgurl"].asText()
        val sign = jsonNode["sign"].asText()
        return BaiduQrcode(url, sign, uuid)
    }

    suspend fun checkQrcode(baiduQrcode: BaiduQrcode): BaiduEntity {
        val jsonNode = try {
            client.get("https://passport.baidu.com/channel/unicast?channel_id=${baiduQrcode.sign}&gid=${baiduQrcode.uuid}&tpl=mn&_sdkFrom=1&callback=tangram_guid_${System.currentTimeMillis()}&apiver=v3&tt=${System.currentTimeMillis()}&_=${System.currentTimeMillis()}")
                .bodyAsText().jsonpToJsonNode()
        } catch (e: SocketTimeoutException) {
            qrcodeNotScanned()
        }
        return when (jsonNode["errno"].asInt()) {
            1 -> qrcodeNotScanned()
            0 -> {
                val ss = jsonNode["channel_v"].asText().toJsonNode()
                if (ss["status"].asInt() == 0) {
                    val v = ss["v"].asText()
                    val response = client.get("https://passport.baidu.com/v3/login/main/qrbdusslogin?v=${System.currentTimeMillis()}&bduss=$v")
                    val cookie = response.setCookie().renderCookieHeader()
                    BaiduEntity().also {
                        it.cookie = cookie
                    }
                } else qrcodeScanned()
            }
            else -> error("未知错误")
        }
    }

    private fun HttpMessageBuilder.ybbDefaultHeader() {
        val map = mutableMapOf<String, String>()
        map["X-Channel-Name"] = "xiaomi"
        map["X-Device-Name"] = "android"
        map["X-Client-Version"] = "2.3.14"
        map["X-System-Version"] = "31"
        map["X-Auth-Timestamp"] = System.currentTimeMillis().toString()
        map.forEach { headers.append(it.key, it.value) }
    }

    suspend fun ybbWatchAd(baiduEntity: BaiduEntity, version: String = "v2") {
        val preJsonNode = client.get("https://api-gt.baidu.com/v1/server/task?version=$version") {
            ybbDefaultHeader()
            cookieString(baiduEntity.cookie)
        }.body<JsonNode>()
        if (!preJsonNode["success"].asBoolean()) error(preJsonNode["errors"]["message_cn"].asText())
        val preResult = preJsonNode["result"]
        val ll = preResult.filter { it["name"].asText() in listOf("看视频送时长", "看视频送积分") }
        if (ll.isEmpty()) error("没有这个任务")
        val sign = ll[0]["sign"].asText()
        val time = System.currentTimeMillis()
        val tenTime = time / 1000
        val jsonNode = Jackson.createObjectNode()
        jsonNode.put("end_time", tenTime)
        jsonNode.put("start_time", tenTime)
        jsonNode.put("task", ll[0]["id"].asInt())
        jsonNode.put("sign", sign)
        val resultJsonObject = client.post("https://api-gt.baidu.com/v1/server/task${if (version.contains("v3")) "?version=v3" else ""}") {
            setJsonBody(jsonNode)
            ybbDefaultHeader()
            cookieString(baiduEntity.cookie)
        }.body<JsonNode>()
        if (!resultJsonObject["success"].asBoolean()) error(resultJsonObject["errors"]["message_cn"].asText())
    }

    suspend fun ybbSign(baiduEntity: BaiduEntity) {
        val jsonNode = client.post("https://ybb.baidu.com/api/v1/server/scores") {
            setJsonBody("""{"type": "daily"}""")
            ybbDefaultHeader()
            cookieString(baiduEntity.cookie)
            referer("https://ybb.baidu.com/m/pages/h5/sign-activity?channel=xiaomi&device=android&appversion=2.3.14&cuid=8D795D0D8C8AB781BD0E0B807B0B1B0F%7CVCUIVQGDM&systemversion=31")
            userAgent("Mozilla/5.0 (Linux; Android 12; M2007J3SC Build/SKQ1.211006.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/100.0.4896.79 Mobile Safari/537.36 com.baidu.ybb/2.3.14")
        }.body<JsonNode>()
        if (!jsonNode["success"].asBoolean()) error(jsonNode["errors"]["message_cn"].asText())
    }

    private fun JsonNode.check() {
        if (!this["success"].asBoolean()) error(this["errors"]["message_cn"].asText())
    }

    suspend fun ybbExchangeVip(baiduEntity: BaiduEntity) {
        val jsonNode = client.post("https://api-gt.baidu.com/v1/server/reward_records") {
            setJsonBody("""{"award_id":48}""")
            ybbDefaultHeader()
            cookieString(baiduEntity.cookie)
        }.body<JsonNode>()
        jsonNode.check()
    }

    private suspend fun getSToken(baiduEntity: BaiduEntity, url: String): String {
        val cookie = baiduEntity.cookie
        val headers = mapOf("cookie" to cookie, "user-agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.67 Safari/537.36")
        val response = client.get("https://wappass.baidu.com/v3/login/api/auth?jump=&notjump=1&return_type=3&tpl=tb&u=${url.toUrlEncode()}") {
            headers {
                headers.forEach { append(it.key, it.value) }
            }
        }
        if (response.status.value !in listOf(302, 301)) throw RuntimeException("您的百度cookie已失效！")
        val firstUrl = response.headers["location"]!!
        val firstResponse = client.get(firstUrl) {
            headers {
                headers.forEach { append(it.key, it.value) }
            }
        }
        return firstResponse.setCookie().find { it.name == "STOKEN" }?.value ?: error("获取sToken失败")
    }

    private suspend fun saveSToken(baiduEntity: BaiduEntity, url: String): String {
        val sToken = getSToken(baiduEntity, url)
        baiduEntity.tieBaSToken = sToken
        baiduService.save(baiduEntity)
        return sToken
    }

    suspend fun tieBaSign(baiduEntity: BaiduEntity) {
        val sToken = baiduEntity.tieBaSToken
        val url = "https://tieba.baidu.com/f/like/mylike?v=${System.currentTimeMillis()}"
        if (sToken.isEmpty()) saveSToken(baiduEntity, url)
        val headers = mapOf("user-agent" to "", "cookie" to baiduEntity.teiBaCookie())
        val likeHtml = client.get(url) { headers { headers.forEach { append(it.key, it.value) } } }.bodyAsText()
        if (likeHtml.isEmpty()) saveSToken(baiduEntity, url)
        val trElements = Jsoup.parse(likeHtml).getElementsByTag("tr")
        val list = mutableListOf<String>()
        for (tr in trElements) {
            val a = tr.getElementsByTag("a")
            if (a.isNotEmpty()) list.add(a[0].attr("title"))
        }
        for (s in list) {
            delay(5000)
            val html =
                client.get("https://tieba.baidu.com/f?kw=${s.toUrlEncode()}&fr=index") { headers { headers.forEach { append(it.key, it.value) } } }.bodyAsText()
            val tbs = RegexUtils.extract(html, "'tbs': \"", "\"")!!
            val jsonObject = client.submitForm("https://tieba.baidu.com/sign/add",
                parameters {
                    mapOf("ie" to "utf-8", "kw" to s, "tbs" to tbs).forEach { append(it.key, it.value) }
                }) {
                headers {
                    headers.forEach { append(it.key, it.value) }
                }
            }.bodyAsText().toJsonNode()
            if (!arrayOf(1101, 0).contains(jsonObject["no"].asInt())) error(jsonObject["error"].asText())
        }
    }
}

data class BaiduQrcode(val image: String, val sign: String, val uuid: String)
