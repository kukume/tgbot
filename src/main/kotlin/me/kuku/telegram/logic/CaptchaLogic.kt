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
import org.springframework.stereotype.Service

@Service
class TwoCaptchaLogic(
    private val botConfigService: BotConfigService,
    private val telegramConfig: TelegramConfig,
    private val configService: ConfigService
) {

    private suspend inline fun <reified T> captcha(tgId: Long? = null, map: Map<String, String>): T {
        val newKey = run {
            tgId?.let {
                val configEntity = configService.findByTgId(tgId)
                configEntity?.twoCaptchaKey()
            }
        } ?: botConfigService.findByToken(telegramConfig.token)?.twoCaptchaKey() ?: error("未设置2captcha的key")
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
            if (jsonNode["status"].asInt() == 1) return jsonNode["request"].convertValue()
        }
    }

    suspend fun geeTest(gt: String, challenge: String, pageUrl: String, tgId: Long? = null): GeeTest {
        return captcha<GeeTest>(tgId, mapOf("method" to "geetest", "gt" to gt, "challenge" to challenge, "pageurl" to pageUrl))
    }

    suspend fun geeTestV4(captchaId: String, pageUrl: String, tgId: Long? = null): GeeTestV4 {
        return captcha(tgId, mapOf("method" to "geetest_v4", "id" to captchaId, "pageurl" to pageUrl))
    }

}

data class GeeTest(@JsonProperty("geetest_challenge") val challenge: String,
                   @JsonProperty("geetest_validate") val validate: String,
                   @JsonProperty("geetest_seccode") val secCode: String)


data class GeeTestV4(@JsonProperty("lot_number") val lotNumber: String,
                   @JsonProperty("gen_time") val genTime: String,
                   @JsonProperty("pass_token") val passToken: String,
                   @JsonProperty("captcha_output") val captchaOutput: String
)