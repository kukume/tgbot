package me.kuku.telegram.logic

import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import me.kuku.telegram.entity.SmZdmEntity
import me.kuku.telegram.exception.qrcodeNotScanned
import me.kuku.telegram.exception.qrcodeScanned
import me.kuku.telegram.utils.*
import org.springframework.stereotype.Service

@Service
class SmZdmLogic(
    private val twoCaptchaLogic: TwoCaptchaLogic
) {

    suspend fun login1(phone: String, tgId: Long? = null) {
        val geeJsonNode = client.get("https://zhiyou.smzdm.com/user/getgeetest/captcha_init_v3?scene=login&rand=${RandomUtils.letter(2)}") {
            headers {
                referer("https://zhiyou.smzdm.com/user/login/window/")
            }
        }.bodyAsText().toJsonNode()
        val gt = geeJsonNode["gt"].asText()
        val challenge = geeJsonNode["challenge"].asText()
        val geeTest = geeTest(gt, challenge, "https://zhiyou.smzdm.com/", tgId)
        val sendCodeNode = client.submitForm("https://zhiyou.smzdm.com/user/login/ajax_get_mobile_code/",
            parameters {
                append("geetest_challenge", geeTest.challenge)
                append("geetest_validate", geeTest.validate)
                append("geetest_seccode", geeTest.secCode)
                append("mobile", phone)
            }).bodyAsText().toJsonNode()
        if (sendCodeNode["error_code"].asInt() != 0) error(sendCodeNode["error_msg"].asText())
    }

    suspend fun login2(phone: String, code: String): SmZdmEntity {
        val response = client.submitForm("https://zhiyou.smzdm.com/user/login/ajax_quick_check",
            parameters { append("mobile", phone)
                append("mobile_code", code)
                append("rememberme", "1")
                append("captcha", "")
                append("redirect_to", "to") })
        val jsonNode = response.bodyAsText().toJsonNode()
        if (jsonNode["error_code"].asInt() != 0) error(jsonNode["error_msg"].asText())
        var cookie = response.setCookie().renderCookieHeader()
        val loginResponse = client.get("https://zhiyou.smzdm.com/user/login/jsonp_is_protocol") {
            headers {
                referer("https://zhiyou.smzdm.com/user/login/window/")
            }
        }
        cookie += loginResponse.setCookie().renderCookieHeader()
        return SmZdmEntity().also { it.cookie = cookie }
    }

    suspend fun wechatQrcode1(): SmZdmWechatQrcode {
        val jsonNode =
            client.post("https://zhiyou.smzdm.com/user/login/jsonp_weixin_qrcode_token") {
                headers {
                    referer("https://zhiyou.smzdm.com/user/login/window/")
                }
            }.bodyAsText().toJsonNode()
        val data = jsonNode["data"]
        val url = data["QrCodeUrl"].asText()
        val sceneStr = data["sceneStr"].asText()
        return SmZdmWechatQrcode(url, sceneStr)
    }

    @Suppress("DuplicatedCode")
    suspend fun wechatQrcode2(smZdmWechatQrcode: SmZdmWechatQrcode): SmZdmEntity {
        val response = client.submitForm("https://zhiyou.smzdm.com/user/login/jsonp_weixin_qrcode_check",
            parameters { append("scene_str", smZdmWechatQrcode.sceneStr) }) {
            headers {
                referer("https://zhiyou.smzdm.com/user/login/window/")
            }
        }
        val jsonNode = response.bodyAsText().toJsonNode()
        if (jsonNode["error_code"].asInt() != 0) error(jsonNode["error_msg"].asText())
        return when (jsonNode["data"]["status"].asInt()) {
            1 -> qrcodeNotScanned()
            2 -> qrcodeScanned()
            3 -> {
                val cookie = response.setCookie().renderCookieHeader()
                client.get("https://www.smzdm.com/") {
                    headers {
                        cookieString(cookie)
                        userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                    }
                }
                SmZdmEntity().also { it.cookie = cookie }
            }
            else -> error("未知错误")
        }
    }

    suspend fun appQrcode1(): SmZdmAppQrcode {
        val jsonNode = client.post("https://zhiyou.smzdm.com/user/login/jsonp_qrcode_token") {
            headers {
                referer("https://zhiyou.smzdm.com/user/login/window/")
            }
        }.bodyAsText().toJsonNode()
        val data = jsonNode["data"]
        val token = data["qrcode_token"].asText()
        val url = data["url"].asText()
        return SmZdmAppQrcode(url, token)
    }

    @Suppress("DuplicatedCode")
    suspend fun appQrcode2(smZdmAppQrcode: SmZdmAppQrcode): SmZdmEntity {
        // {"error_code":0,"error_msg":"","data":{"status":"1","redirect_to":""}}
        val response = client.submitForm("https://zhiyou.smzdm.com/user/login/jsonp_qrcode_check",
            parameters { append("qrcode_token", smZdmAppQrcode.token) }) {
            headers {
                referer("https://zhiyou.smzdm.com/user/login/window/")
            }
        }
        val jsonNode = response.bodyAsText().toJsonNode()
        if (jsonNode["error_code"].asInt() != 0) error(jsonNode["error_msg"].asText())
        return when (jsonNode["data"]["status"].asInt()) {
            1 -> qrcodeNotScanned()
            2 -> qrcodeScanned()
            3 -> {
                val cookie = response.setCookie().renderCookieHeader()
                client.get("https://www.smzdm.com/") {
                    headers {
                        cookieString(cookie)
                        userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                    }
                }
                SmZdmEntity().also { it.cookie = cookie }
            }
            else -> error("未知错误")
        }
    }


    suspend fun webSign(smZdmEntity: SmZdmEntity, tgId: Long? = null) {
        val jsonNode = client.get("https://zhiyou.smzdm.com/user/getgeetest/geetest_captcha_init").bodyAsText().toJsonNode()
        val data = jsonNode["data"]["geetest_data"]
        val gt = data["gt"].asText()
        val challenge = data["challenge"].asText()
        val geeTest = geeTest(gt, challenge, tgId = tgId)
        val text = client.get("https://zhiyou.smzdm.com/user/checkin/jsonp_checkin?callback=jQuery112406820925204571995_1673311348950&geetest_challenge=${geeTest.challenge}&geetest_validate=${geeTest.validate}&geetest_seccode=${geeTest.secCode.toUrlEncode()}&_=${System.currentTimeMillis()}") {
            headers {
                cookieString(smZdmEntity.cookie)
                referer("https://www.smzdm.com/")
            }
        }.bodyAsText()
        val signNode = text.substring(text.indexOf('{'), text.lastIndexOf('}') + 1).toJsonNode()
        if (signNode["error_code"].asInt() != 0) error(signNode["error_msg"].asText())
    }

    private suspend fun geeTest(gt: String, challenge: String, referer: String = "https://www.smzdm.com/", tgId: Long? = null): GeeTestResult {
        val result = twoCaptchaLogic.geeTest(gt, challenge, referer, tgId = tgId)
        return GeeTestResult(result.challenge, result.validate)

    }

    private fun appSign(smZdmEntity: SmZdmEntity, t: Long): String {
        val cookie = smZdmEntity.cookie
        val sess = RegexUtils.extract(cookie, "sess=", ";")
        val sign = "f=android&sk=1&time=$t&token=$sess&v=10.0&weixin=0&key=apr1\$AwP!wRRT\$gJ/q.X24poeBInlUJC"
        return sign.md5().uppercase()
    }

    suspend fun appSign(smZdmEntity: SmZdmEntity) {
        val t = System.currentTimeMillis()
        val sess = RegexUtils.extract(smZdmEntity.cookie, "sess=", ";")!!
        val jsonNode = client.submitForm("https://user-api.smzdm.com/checkin",
            parameters {
                append("touchstone_event", "")
                append("v", "10.0")
                append("sign", appSign(smZdmEntity, t))
                append("weixin", "0")
                append("time", t.toString())
                append("sk", "1")
                append("token", sess)
                append("f", "android")
                append("captcha", "")
            }) {
            headers {
                cookieString(smZdmEntity.cookie)
            }
        }.bodyAsText().toJsonNode()
        if (jsonNode["error_code"].asInt() != 0) error(jsonNode["error_msg"].asText())
    }

    private data class GeeTestResult(val challenge: String, val validate: String, val secCode: String = "$validate|jordan")

}

data class SmZdmWechatQrcode(val url: String, val sceneStr: String)

data class SmZdmAppQrcode(val url: String, val token: String)
