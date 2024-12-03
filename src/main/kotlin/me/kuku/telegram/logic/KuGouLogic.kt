package me.kuku.telegram.logic

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import me.kuku.telegram.config.api
import me.kuku.telegram.entity.KuGouEntity
import me.kuku.telegram.exception.qrcodeExpire
import me.kuku.telegram.exception.qrcodeNotScanned
import me.kuku.telegram.utils.*
import org.springframework.stereotype.Service
import java.util.*

data class KuGouQrcode (
    var url: String,
    var qrcode: String,
    var mid: String
)

@Service
class KuGouLogic {

    private fun e(): String {
        return Integer.toHexString(((65536 * (1 + Math.random())).toInt()))
    }

    fun mid(): String{
        val s = e() + e() + "-" + e() + "-" + e() + "-" + e() + "-" + e() + e() + e()
        return s.md5()
    }

    private fun clientTime(): Int{
        return (System.currentTimeMillis() / 1e3).toInt()
    }

    private fun signature(map: MutableMap<String, String>): String{
        val list = mutableListOf<String>()
        val sb = StringBuilder()
        for ((k, v) in map){
            list.add(v)
            sb.append("$k=$v&")
        }
        list.sort()
        val s = list.joinToString("")
        val signature = s.md5()
        sb.append("signature=$signature")
        return sb.toString()
    }

    private fun signature(ss: String, map: MutableMap<String, String>, other: String = ""): String {
        val list = LinkedList<String>()
        val sb = StringBuilder()
        for ((k, v) in map){
            list.add("$k=$v")
            sb.append("$k=$v&")
        }
        list.sort()
        list.addFirst(ss)
        list.add(other)
        list.add(ss)
        val s = list.joinToString("")
        val signature = s.md5()
        sb.append("signature=$signature")
        return sb.toString()
    }

    private fun signature2(map: MutableMap<String, String>, other: String = ""): String{
        return signature("NVPh5oo715z5DIWAeQlhMDsWXXQV4hwt", map, other)
    }

    private fun signature3(map: MutableMap<String, String>, other: String = ""): String{
        return signature("LnT6xpN3khm36zse0QzvmgTZ3waWdRSA", map, other)
    }

    suspend fun getQrcode(mid: String?): KuGouQrcode {
        val newMid = mid ?: mid()
        val map = mutableMapOf(
            "appid" to "1014", "clientver" to "8131", "clienttime" to clientTime().toString(),
            "uuid" to newMid, "mid" to newMid, "type" to "1"
        )
        val jsonNode = client.get("https://login-user.kugou.com/v1/qrcode?${signature(map)}").body<JsonNode>()
        val qrcode = jsonNode["data"]["qrcode"].asText()
        return KuGouQrcode("https://h5.kugou.com/apps/loginQRCode/html/index.html?qrcode=$qrcode&appid=1014",
            qrcode, newMid)
    }

    suspend fun checkQrcode(kuGouQrcode: KuGouQrcode): KuGouEntity {
        val map = mutableMapOf("appid" to "1014", "clientver" to "8131",
            "clienttime" to clientTime().toString(), "qrcode" to kuGouQrcode.qrcode, "dfid" to "-",
            "mid" to kuGouQrcode.mid, "plat" to "4", "uuid" to kuGouQrcode.mid)
        val jsonNode = client.get(
            "https://login-user.kugou.com/v1/get_userinfo_qrcode?${signature(map)}"
        ).body<JsonNode>()
        val dataStatus = jsonNode["data"]["status"].asInt()
        return when (dataStatus) {
            1, 2 -> qrcodeNotScanned()
            0 -> qrcodeExpire()
            4 -> {
                val token = jsonNode["data"]["token"].asText()
                val userid = jsonNode["data"]["userid"].asLong()
                val response =
                    client.get("https://login-user.kugou.com/v1/autologin?a_id=1014&userid=$userid&t=$token&ct=${clientTime()}&callback=qrcodeLoginCallback&domain=kugou.com&uuid=${kuGouQrcode.mid}&mid=$${kuGouQrcode.mid}&plat=4&dfid=-&kguser_jv=180925")
                val setCookie = response.setCookie()
                val cookie = setCookie.renderCookieHeader()
                val kuGoo = setCookie.find { it.name == "KuGoo" }?.value
                val kuGouEntity = KuGouEntity()
                kuGouEntity.token = token
                kuGouEntity.userid = userid
                kuGouEntity.mid = kuGouQrcode.mid
                kuGouEntity.kuGoo = kuGoo!!
                kuGouEntity
            }
            else -> error("未知的错误代码：$dataStatus")
        }
    }

    suspend fun sendMobileCode(phone: String, mid: String) {
        val time = System.currentTimeMillis()
        val map = mutableMapOf(
            // 1058
            "appid" to "3116",
            "clientver" to "1000",
            "clienttime" to time.toString(),
            "mid" to mid,
            "uuid" to mid,
            "dfid" to "-",
            "srcappid" to "2919"
        )
        val preJsonNode = client.submitForm("$api/exec/kuGou",
            parameters {
                append("phone", phone)
                append("time", time.toString())
            }
        ).body<JsonNode>()
        val params = preJsonNode["params"]?.asText() ?: error("获取加密参数失败，可能手机号格式不正确")
        val pk = preJsonNode["pk"].asText()
        val mobile = phone.substring(0, 2) + "********" + phone.substring(phone.length - 1)
        val other = "{\"plat\":4,\"clienttime_ms\":$time,\"businessid\":5,\"pk\":\"$pk\",\"params\":\"$params\",\"mobile\":\"$mobile\"}"
        val jsonNode = client.post("https://gateway.kugou.com/v8/send_mobile_code?${signature2(map, other)}") {
            setBody(other)
            contentType(ContentType.Text.Plain)
            headers {
                mapOf("x-router" to "loginservice.kugou.com", "referer" to "https://m3ws.kugou.com/").forEach {
                    append(it.key, it.value)
                }
            }
        }.body<JsonNode>()
        if (jsonNode["error_code"].asInt() != 0) error(jsonNode["data"].asText())
    }

    suspend fun verifyCode(phone: String, code: String, mid: String): KuGouEntity {
        val time = clientTime()
        val map = mutableMapOf(
            "appid" to "3116",
            "clientver" to "10",
            "clienttime" to time.toString(),
            "mid" to mid,
            "uuid" to mid,
            "dfid" to "-",
            "srcappid" to "2919"
        )
        val other = "{\"plat\":4,\"mobile\":\"$phone\",\"code\":\"$code\",\"expire_day\":60,\"support_multi\":1,\"userid\":\"\",\"force_login\":0}"
        val response = client.post("https://login-user.kugou.com/v2/loginbyverifycode/?${signature2(map, other)}") {
            setBody(other)
            contentType(ContentType.Text.Plain)
            headers {
                mapOf("x-router" to "loginservice.kugou.com", "referer" to "https://m3ws.kugou.com/").forEach {
                    append(it.key, it.value)
                }
            }
        }
        val jsonNode = response.body<JsonNode>()
        return if (jsonNode["error_code"].asInt() == 0) {
            val setCookie = response.setCookie()
            val kuGoo = jsonNode["data"]["value"].asText()
            val token = setCookie.find { it.name == "t" }?.value
            val userid = setCookie.find { it.name == "KugooID" }!!.value
            KuGouEntity().also {
                it.token = token!!
                it.userid = userid.toLong()
                it.kuGoo = kuGoo
                it.mid = mid
            }
        }
        else error(jsonNode["data"].asText())
    }

    suspend fun login(username: String, password: String, mid: String?): KuGouEntity {
        val newMid = mid ?: mid()
        val md5Pwd = password.md5()
        val params = "appid=1058&username=$username&pwd=$md5Pwd&code=&ticket=&clienttime=${clientTime()}&expire_day=60&autologin=false&redirect_uri=&state=&callback=loginModule.loginCallback&login_ver=1&mobile=&mobile_code=&plat=4&dfid=-&mid=$newMid&kguser_jv=180925"
        val response = client.get("https://login-user.kugou.com/v1/login/?$params") {
            referer("https://m3ws.kugou.com/")
        }
        val jsonNode = response.bodyAsText().jsonpToJsonNode()
        return when (jsonNode["errorCode"]?.asInt()){
            30791 -> {
                // 验证码
                error("需要验证验证码，请使用短信验证码登陆")
            }
            null -> {
                val cookie = response.setCookie()
                val kuGoo = cookie.find { it.name == "KuGoo" }?.value
                val token = jsonNode["token"].asText()
                val userid = jsonNode["userid"].asLong()
                KuGouEntity().also {
                    it.token = token
                    it.userid = userid
                    it.kuGoo = kuGoo!!
                    it.mid = newMid
                }
            }
            30768 -> error("需要短信验证码！请直接使用短信验证码登录！")
            else -> error(jsonNode["errorMsg"].asText())
        }
    }

    suspend fun musicianSign(kuGouEntity: KuGouEntity) {
        // 1014
        // 1058
        val kuGoo = kuGouEntity.kuGoo
        val aId = RegexUtils.extract(kuGoo, "a_id=", "&")!!
        val time = System.currentTimeMillis().toString()
        val map = mutableMapOf("appid" to aId, "token" to kuGouEntity.token,
            "kugouid" to kuGouEntity.userid.toString(), "srcappid" to "2919", "clientver" to "20000",
            "clienttime" to time, "dfid" to "-",
            "mid" to time, "uuid" to time)
        val jsonNode = client.submitForm("https://h5activity.kugou.com/v1/musician/do_signed?${signature2(map)}")
            .body<JsonNode>()
        if (jsonNode["errcode"].asInt() != 0) error("酷狗音乐人签到失败！" + jsonNode["errmsg"].asText())
    }

    suspend fun listenMusic(kuGouEntity: KuGouEntity) {
//        val aId = MyUtils.regex("a_id=", "&", kuGouEntity.kuGoo)!!
        val map = mutableMapOf("userid" to kuGouEntity.userid.toString(), "token" to kuGouEntity.token,
            "appid" to "3116", "clientver" to "10547", "clienttime" to (System.currentTimeMillis() / 1000).toString(),
            "mid" to kuGouEntity.mid, "uuid" to RandomUtils.letter(32), "dfid" to "-")
        val other = """{"mixsongid":273263741}"""
        val jsonNode = client.post("https://gateway.kugou.com/v2/report/listen_song?${signature3(map, other)}") {
            setBody(other)
            contentType(ContentType.Text.Plain)
            headers {
                mapOf("x-router" to "youth.kugou.com", "User-Agent" to "Android12-1070-10536-130-0-ReportPlaySongToServerProtocol-wifi").forEach {
                    append(it.key, it.value)
                }
            }
        }.body<JsonNode>()
        val code = jsonNode["error_code"].asInt()
        if (code != 0 && code != 130012) error(jsonNode["error_msg"].asText())
    }

    suspend fun watchAd(kuGouEntity: KuGouEntity) {
        val map = mutableMapOf("userid" to kuGouEntity.userid.toString(), "token" to kuGouEntity.token,
            "appid" to "3116", "clientver" to "10780", "clienttime" to (System.currentTimeMillis() / 1000).toString(),
            "mid" to kuGouEntity.mid, "uuid" to RandomUtils.letter(32), "dfid" to "-")
        val now = System.currentTimeMillis()
        val before = now - 15 * 1423
        val other = """
            {"ad_id":"12424568007","play_start":$before,"play_end":$now}
        """.trimIndent()
        val jsonNode = client.post("https://gateway.kugou.com/youth/v1/ad/play_report?${signature3(map, other)}") {
            setJsonBody(other)
            userAgent("Android12-1070-10780-130-0-AdPlayStatusProtocol-3gnet(20)")
        }.body<JsonNode>()
        // {"error_msg":"","data":{"remain_vip_hour":9,"total":8,"done":5,"remain":3,"award_vip_hour":3},"status":1,"error_code":0}
        if (jsonNode["error_code"].asInt() !in listOf(0, 30002)) error(jsonNode["error_msg"].asText())
    }

}
