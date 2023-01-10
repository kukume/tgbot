package me.kuku.telegram.logic

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import me.kuku.pojo.CommonResult
import me.kuku.telegram.entity.SmZdmEntity
import me.kuku.utils.*

object SmZdmLogic {

    suspend fun login1(phone: String) {
        val geeJsonNode = client.get("https://zhiyou.smzdm.com/user/getgeetest/captcha_init_v3?scene=login&rand=${MyUtils.randomNum(2)}") {
            headers {
                referer("https://zhiyou.smzdm.com/user/login/window/")
            }
        }.bodyAsText().toJsonNode()
        val gt = geeJsonNode["gt"].asText()
        val challenge = geeJsonNode["challenge"].asText()
        val geeTest = geeTest(gt, challenge, "https://zhiyou.smzdm.com/")
        val sendCodeNode = client.post("https://zhiyou.smzdm.com/user/login/ajax_get_mobile_code/") {
            setFormDataContent {
                append("geetest_challenge", geeTest.challenge)
                append("geetest_validate", geeTest.validate)
                append("geetest_seccode", geeTest.secCode)
                append("mobile", phone)
            }
        }.bodyAsText().toJsonNode()
        if (sendCodeNode["error_code"].asInt() != 0) error(sendCodeNode["error_msg"].asText())
    }

    suspend fun login2(phone: String, code: String): SmZdmEntity {
        val response = client.post("https://zhiyou.smzdm.com/user/login/ajax_quick_check") {
            setFormDataContent {
                append("mobile", phone)
                append("mobile_code", code)
                append("rememberme", "1")
                append("captcha", "")
                append("redirect_to", "to")
            }
        }
        val jsonNode = response.bodyAsText().toJsonNode()
        if (jsonNode["error_code"].asInt() != 0) error(jsonNode["error_msg"].asText())
        var cookie = response.cookie()
        val loginResponse = client.get("https://zhiyou.smzdm.com/user/login/jsonp_is_protocol") {
            headers {
                referer("https://zhiyou.smzdm.com/user/login/window/")
            }
        }
        cookie += loginResponse.cookie()
        return SmZdmEntity().also { it.cookie = cookie }
    }

    suspend fun wechatQrcode1(): SmZdmWechatQrcode {
        val jsonNode =
            client.post("https://zhiyou.smzdm.com/user/login/jsonp_weixin_qrcode_token").bodyAsText().toJsonNode()
        val data = jsonNode["data"]
        val url = data["QrCodeUrl"].asText()
        val sceneStr = data["sceneStr"].asText()
        return SmZdmWechatQrcode(url, sceneStr)
    }

    suspend fun wechatQrcode2(smZdmWechatQrcode: SmZdmWechatQrcode): SmZdmEntity {
        val response = client.post("https://zhiyou.smzdm.com/user/login/jsonp_weixin_qrcode_check") {
            setFormDataContent {
                append("scene_str", smZdmWechatQrcode.sceneStr)
            }
        }
        val cookie = response.cookie()
        val sess = OkUtils.cookie(cookie, "sess")
        return if (sess == null) error("未获取到cookie")
        else {
            client.get("https://www.smzdm.com/") {
                headers {
                    cookieString(cookie)
                    userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                }
            }
            SmZdmEntity().also { it.cookie = cookie }
        }
    }

    suspend fun webSign(smZdmEntity: SmZdmEntity) {
        val jsonNode = client.get("https://zhiyou.smzdm.com/user/getgeetest/geetest_captcha_init").bodyAsText().toJsonNode()
        val data = jsonNode["data"]["geetest_data"]
        val gt = data["gt"].asText()
        val challenge = data["challenge"].asText()
        val geeTest = geeTest(gt, challenge)
        val text = client.get("https://zhiyou.smzdm.com/user/checkin/jsonp_checkin?callback=jQuery112406820925204571995_1673311348950&geetest_challenge=${geeTest.challenge}&geetest_validate=${geeTest.validate}&geetest_seccode=${geeTest.secCode.toUrlEncode()}&_=${System.currentTimeMillis()}") {
            headers {
                cookieString(smZdmEntity.cookie)
                referer("https://www.smzdm.com/")
            }
        }.bodyAsText()
        val signNode = text.substring(text.indexOf('{'), text.lastIndexOf('}') + 1).toJsonNode()
        if (signNode["error_code"].asInt() != 0) error(signNode["error_msg"].asText())
    }

    private suspend fun geeTest(gt: String, challenge: String, referer: String = "https://www.smzdm.com/"): GeeTestResult {
        val jsonNode = client.post("https://api.kukuqaq.com/geetest") {
            setFormDataContent {
                append("gt", gt)
                append("challenge", challenge)
                append("referer", referer)
            }
        }.body<JsonNode>()
        if (!jsonNode.has("code")) {
            val ch = jsonNode["challenge"].asText()
            val validate = jsonNode["validate"].asText()
            return GeeTestResult(ch, validate)
        } else error(jsonNode["message"].asText())

    }

    private fun appSign(smZdmEntity: SmZdmEntity, t: Long): String {
        val cookie = smZdmEntity.cookie
        val sess = OkUtils.cookie(cookie, "sess")
        val sign = "f=android&sk=1&time=$t&token=$sess&v=10.0&weixin=0&key=apr1\$AwP!wRRT\$gJ/q.X24poeBInlUJC"
        return sign.md5().uppercase()
    }

    suspend fun appSign(smZdmEntity: SmZdmEntity) {
        val t = System.currentTimeMillis()
        val sess = OkUtils.cookie(smZdmEntity.cookie, "sess")!!
        val text = client.post("https://user-api.smzdm.com/checkin") {
            setFormDataContent {
                append("touchstone_event", "")
                append("v", "10.0")
                append("sign", appSign(smZdmEntity, t))
                append("weixin", "0")
                append("time", t.toString())
                append("sk", "1")
                append("token", sess)
                append("f", "android")
                append("captcha", "")
            }
            headers {
                cookieString(smZdmEntity.cookie)
            }
        }.bodyAsText()
        println(text)
    }

    private data class GeeTestResult(val challenge: String, val validate: String, val secCode: String = "$validate|jordan")

}

data class SmZdmWechatQrcode(val url: String, val sceneStr: String)
