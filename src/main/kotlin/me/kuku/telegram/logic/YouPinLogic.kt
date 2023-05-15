package me.kuku.telegram.logic

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import me.kuku.telegram.entity.YouPinEntity
import me.kuku.utils.*
import java.util.UUID

object YouPingLogic {

    private const val ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36"

    private suspend fun uk(): String {
        val time = DateTimeFormatterUtils.formatNow("HH:mm:ss")
        val json = """
            {"availHeight":1019,"availWidth":1707,"appCodeName":"Mozilla","appName":"Netscape","hardwareConcurrency":20,"language":"zh-CN","languages":["zh-CN"],"onLine":true,"platform":"Win32","product":"Gecko","productSub":"20030107","userAgent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36","vendor":"Google Inc.","vendorSub":"","plugins":"487f7b22f68312d2c1bbc93b1aea445b","doNotTrack":"","innerWidth":244,"innerHeight":916,"dateGMT":"$time GMT+0800 (中国标准时间)","src":"pc","bcn":"u","client_time":${System.currentTimeMillis()},"iud":"ca02d4d6-1473-4a53-acb1-31af388cf8f3","fonts":["nc_iconfont","ncpc_iconfont"],"cv":"ec355cd745d02d67028a50dca7c3216c","devtoolData":[${System.currentTimeMillis()}]}
        """.trimIndent()
        val w = AESUtils.aesEncrypt("DGlNrfZIKr3MCT0e", json)!!.base64Encode()
        val text = client.post("https://s.youpin898.com/api/w") {
            setBody(w)
            contentType(ContentType.Text.Plain)
            headers {
                append("D", "p=p&b=u&v=1")
            }
        }.bodyAsText()
        val ss = String(AESUtils.aesDecrypt(
            "F5D1eG6jG7ubETY7".toByteArray(),
            text.base64Decode()
        )!!).toJsonNode()
        return ss["u"].asText()
    }

    private fun HeadersBuilder.uk(uk: String) {
        append("Uk", uk)
    }

    suspend fun smsLogin1(phone: String): YouPinLoginCache {
        val uk = uk()
        val uuid = UUID.randomUUID().toString()
        val jsonNode = client.post("https://api.youpin898.com/api/user/Auth/SendSignInSmsCode") {
            setJsonBody("""
                {"Mobile":"$phone","SessionId":"$uuid"}
            """.trimIndent()
            )
            headers {
                uk(uk)
                userAgent(ua)
            }
        }.body<JsonNode>()
        if (jsonNode["Code"].asInt() != 0) error(jsonNode["Msg"].asText())
        return YouPinLoginCache(uk, uuid, phone)
    }

    suspend fun smsLogin2(cache: YouPinLoginCache, code: String): YouPinEntity {
        val jsonNode = client.post("https://api.youpin898.com/api/user/Auth/SmsSignIn") {
            setJsonBody("""
                {"Mobile":"${cache.phone}","Code":"$code","SessionId":"${cache.uuid}","TenDay":1}
            """.trimIndent())
            headers {
                uk(cache.uk)
                userAgent(ua)
            }
        }.body<JsonNode>()
        if (jsonNode["Code"].asInt() != 0) error(jsonNode["Msg"].asText())
        val token = jsonNode["Data"]["Token"].asText()
        return YouPinEntity().also {
            it.uk = cache.uk
            it.token = "Bearer $token"
        }
    }

}

data class YouPinLoginCache(val uk: String, val uuid: String, val phone: String)
