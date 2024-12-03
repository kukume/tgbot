package me.kuku.telegram.logic

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import me.kuku.telegram.config.TelegramConfig
import me.kuku.telegram.entity.BotConfigService
import me.kuku.telegram.entity.ConfigService
import me.kuku.telegram.utils.Jackson
import me.kuku.telegram.utils.client
import me.kuku.telegram.utils.setJsonBody
import org.springframework.stereotype.Service

@Service
class TwoCaptchaLogic(
    private val botConfigService: BotConfigService,
    private val telegramConfig: TelegramConfig,
    private val configService: ConfigService
) {

    private suspend inline fun <reified T> captcha(tgId: Long? = null, task: Map<String, Any>): T {
        val newKey = run {
            tgId?.let {
                val configEntity = configService.findByTgId(tgId)
                configEntity?.twoCaptchaKey()
            }
        } ?: botConfigService.findByToken(telegramConfig.token)?.twoCaptchaKey() ?: error("未设置2captcha的key")
        val paramNode = Jackson.createObjectNode()
        paramNode.put("clientKey", newKey)
        paramNode.putPOJO("task", task)
        val jsonNode = client.post("https://api.2captcha.com/createTask") {
            setJsonBody(paramNode)
        }.body<JsonNode>()
        if (jsonNode["errorId"].asInt() != 0) error("识别验证码失败：" + jsonNode["errorDescription"].asText())
        val code = jsonNode["taskId"].asLong()
        var i = 0
        while (true) {
            if (i++ > 35) error("无法识别验证码")
            delay(2000)
            val resultJsonNode = client.post("https://api.2captcha.com/getTaskResult") {
                setJsonBody("""
                    {
                       "clientKey": "$newKey", 
                       "taskId": $code
                    }
                """.trimIndent())
            }.body<JsonNode>()
            val resultCode = resultJsonNode["errorId"].asInt()
            if (resultCode == 0) {
                val status = resultJsonNode["status"].asText()
                if (status == "processing") continue
                else return Jackson.convertValue(resultJsonNode["solution"])
            } else {
                error("识别验证码失败：" + resultJsonNode["errorDescription"].asText())
            }
        }
    }

    suspend fun geeTest(gt: String, challenge: String, pageUrl: String, tgId: Long? = null): GeeTest {
        return captcha<GeeTest>(tgId,
            mapOf("type" to "GeeTestTaskProxyless", "gt" to gt, "challenge" to challenge, "websiteURL" to pageUrl))
    }

    suspend fun geeTestV4(captchaId: String, pageUrl: String, extraParams: Map<String, String> = mapOf(), tgId: Long? = null): GeeTestV4 {
        return captcha(tgId,
            mapOf("type" to "GeeTestTaskProxyless", "captcha_id" to captchaId, "websiteURL" to pageUrl, "version" to 4,
                "initParameters" to mutableMapOf("captcha_id" to captchaId).also { it.putAll(extraParams) }
            ))
    }

}

data class GeeTest(@JsonProperty("challenge") val challenge: String,
                   @JsonProperty("validate") val validate: String,
                   @JsonProperty("seccode") val secCode: String)


data class GeeTestV4(@JsonProperty("lot_number") val lotNumber: String,
                   @JsonProperty("gen_time") val genTime: String,
                   @JsonProperty("pass_token") val passToken: String,
                   @JsonProperty("captcha_output") val captchaOutput: String
)