package me.kuku.telegram.logic

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import me.kuku.telegram.config.TelegramConfig
import me.kuku.telegram.entity.BotConfigService
import me.kuku.telegram.entity.ConfigService
import me.kuku.utils.client
import me.kuku.utils.convertValue
import me.kuku.utils.setFormDataContent
import me.kuku.utils.toJsonNode
import org.springframework.stereotype.Service

@Service
class TwoCaptchaLogic(
    private val botConfigService: BotConfigService,
    private val telegramConfig: TelegramConfig
) {

    private suspend fun captcha(key: String? = null, map: Map<String, String>): String {
        val newKey = key ?: run {
            botConfigService.findByToken(telegramConfig.token)?.twoCaptchaKey ?: error("未设置2captcha的key")
        }
        val text = client.get("http://2captcha.com/in.php") {
            url {
                parameters.append("key", newKey)
                map.forEach { (k, v) -> parameters.append(k, v) }
            }
        }.bodyAsText()
        val arr = text.split("|")
        if (arr[0] != "OK") error("无法识别验证码")
        val code = arr[1]
        var i = 0
        while (true) {
            if (i++ > 35) error("无法识别验证码")
            delay(2000)
            val jsonNode = client.get("http://2captcha.com/res.php?key=$newKey&action=get&json=1&id=$code").body<JsonNode>()
            if (jsonNode["status"].asInt() == 1) return jsonNode["request"].asText()
        }
    }

    suspend fun recaptchaV3(key: String? = null, googleKey: String, pageUrl: String): String {
        return captcha(key, mapOf("method" to "userrecaptcha", "version" to "v3",
            "action" to "verify", "min_score" to "0.3", "googlekey" to googleKey, "pageurl" to pageUrl))
    }

    suspend fun recaptchaV2(key: String? = null, googleKey: String, pageUrl: String): String {
        return captcha(key, mapOf("method" to "userrecaptcha", "googlekey" to googleKey,
            "pageurl" to pageUrl))
    }

}

@Service
class GeeTestLogic(
    private val botConfigService: BotConfigService,
    private val telegramConfig: TelegramConfig,
    private val configService: ConfigService
) {

    /**
     * 成功返回
     * 三代
     * {
     *   "status": 0,
     *   "msg": "识别成功",
     *   "data": {
     *     "challenge": "97f46c4c54f19a1ef4b1558018dde310",
     *     "validate": "71a2b5eaf8352695d4db042b36e91e46"
     *   }
     *   "time":3467
     * }
     * 四代【注意事项：如遇无法通过可能是网站编码问题，则captcha_output的 '='符号用%3D替换，其他无需编码，请注意该事项】
     * {
     *   "status": 0,
     *   "msg": "识别成功",
     *   "data": {
     * 		"challenge": "12729b40-f360-4d02-9ddb-00c17711baf6",
     * 		"seccode": {
     * 			"captcha_id": "68309e2ff42954131328bc455ce80c14",
     * 			"lot_number": "a9931f30755542f48b17ace3deb1ff60",
     * 			"pass_token": "7887b8f3dc11da1d137eaec7f9a222452e37e80217fd9295a5cbdfb0fa8b0fb7",
     * 			"gen_time": "1656172614",
     * 			"captcha_output": "1P9rHCof8yt7R_zB5hE5Z81aXgM4KzOJF7hIH3-jfCxeVyi26gVsyxVlD9LICxvSyC6faJnTdmMJFnqr8y5VhsLg_540Inc8ARjzonfC18F1-IIAQfGPd8X7Dxur0lFxjmP93i9mogmm66l6HZsCWdxQjS421KWOt6_JjB7S8m522KRG3tG_-yThSROb8xrRHVa-5YpmYyX3ZCKUn-TRI9SxTT9WjGhNM9gkJhm-epI1MWnvUNqLVAzqWEUOQaB="
     * 		}
     *   }
     *   "time":3467
     * }
     *
     * 失败返回
     * {
     *     "status": -1,//用户积分不足也是这个值msg不同信息
     *     "msg": "识别失败",
     *     "code": 1004
     * }
     */
    suspend fun rr(gt: String, referer: String, challenge: String? = null, ip: String? = null,
                   host: String? = null, tgId: Long? = null, mmtKey: String? = null,
                   ua: String? = null, riskType: String? = null): RrOcrResult {
        val newKey = run {
            tgId?.let {
                val configEntity = configService.findByTgId(tgId)
                configEntity?.rrOcrKey()
            }
        } ?: botConfigService.findByToken(telegramConfig.token)?.rrOcrKey ?: error("未设置rrocr的key")
        val jsonNode = client.post("http://api.rrocr.com/api/recognize.html") {
            setFormDataContent {
                append("gt", gt)
                append("referer", referer)
                challenge?.let {
                    append("challenge", it)
                }
                ip?.let {
                    append("ip", it)
                }
                host?.let {
                    append("host", host)
                }
                mmtKey?.let {
                    append("mmt_key", mmtKey)
                }
                ua?.let {
                    append("useragent", ua)
                }
                riskType?.let {
                    append("risk_type", it)
                }
                append("appkey", newKey)
                append("sharecode", "4eb29017f5464cb2a971364373c65edb")
            }
        }.bodyAsText().toJsonNode()
        return jsonNode["data"]?.convertValue() ?: error(jsonNode["msg"].asText())
    }

}

data class RrOcrResult(var challenge: String = "", var validate: String = "", @JsonProperty("seccode") var secCode: RrOcrSecCode = RrOcrSecCode())

data class RrOcrSecCode(@JsonProperty("captcha_id") var captchaId: String = "", @JsonProperty("lot_number") var lotNumber: String = "",
                        @JsonProperty("pass_token") var passToken: String = "", @JsonProperty("gen_time") var genTime: String = "",
                        @JsonProperty("captcha_output") var captchaOutput: String = "")

