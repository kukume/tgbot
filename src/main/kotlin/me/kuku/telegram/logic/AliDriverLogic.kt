package me.kuku.telegram.logic

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import me.kuku.pojo.CommonResult
import me.kuku.telegram.entity.AliDriverEntity
import me.kuku.telegram.entity.AliDriverService
import me.kuku.utils.client
import me.kuku.utils.setJsonBody
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AliDriverLogic(
    private val aliDriverService: AliDriverService
) {

    private val cache = mutableMapOf<Long, AliDriverAccessToken>()

    suspend fun login1() = client.get("https://api.kukuqaq.com/alidrive/qrcode").body<AliDriverQrcode>()

    suspend fun login2(qrcode: AliDriverQrcode): CommonResult<AliDriverEntity> {
        val jsonNode = client.post("https://api.kukuqaq.com/alidrive/qrcode") {
            setJsonBody(qrcode)
        }.body<JsonNode>()
        return if (jsonNode.has("code")) {
            CommonResult.fail(message = jsonNode["message"].asText(), code = jsonNode["code"].asInt())
        } else CommonResult.success(AliDriverEntity().also {
            it.refreshToken = jsonNode["refreshToken"].asText()
        })
    }

    private suspend fun accessToken(aliDriverEntity: AliDriverEntity): String {
        val accessToken = cache[aliDriverEntity.tgId]
        return if (accessToken == null || accessToken.isExpire()) {
            val jsonNode = client.post("https://auth.aliyundrive.com/v2/account/token") {
                setJsonBody("""{"refresh_token": "${aliDriverEntity.refreshToken}", "grant_type": "refresh_token"}"}""")
            }.body<JsonNode>()
            if (jsonNode.has("code")) error(jsonNode["message"].asText())
            val token = "${jsonNode["token_type"].asText()} ${jsonNode["access_token"].asText()}"
            cache[aliDriverEntity.tgId] = AliDriverAccessToken(token, System.currentTimeMillis() + jsonNode["expires_in"].asLong() * 1000)
            val newRefreshToken = jsonNode["refresh_token"].asText()
            aliDriverEntity.refreshToken = newRefreshToken
            aliDriverService.save(aliDriverEntity)
            token
        } else accessToken.accessToken
    }

    suspend fun sign(aliDriverEntity: AliDriverEntity): AliDriverSign {
        val accessToken = accessToken(aliDriverEntity)
        val jsonNode = client.post("https://member.aliyundrive.com/v1/activity/sign_in_list") {
            setJsonBody("{}")
            headers {
                append("Authorization", accessToken)
            }
        }.body<JsonNode>()
        return if (jsonNode["success"]?.asBoolean() == true) {
            val result = jsonNode["result"]
            val sign = AliDriverSign()
            sign.subject = result["subject"].asText()
            sign.customMessage = "签到成功，本月已签到${jsonNode["result"]["signInCount"].asInt()}次"
            sign.title = result["title"].asText()
            sign.isReward = result["isReward"].asBoolean()
            sign.blessing = result["blessing"].asText()
            sign.signInCount = result["signInCount"].asInt()
            for (node in result["signInLogs"]) {
                val signInLog = AliDriverSign.SignInLog()
                signInLog.day = node["day"].asInt()
                signInLog.status = node["status"].asText()
                signInLog.type = node["type"].asText()
                signInLog.rewardAmount = node["rewardAmount"].asInt()
                signInLog.themes = node["themes"].asText()
                signInLog.calendarChinese = node["calendarChinese"].asText()
                signInLog.calendarDay = node["calendarDay"].asInt()
                signInLog.calendarMonth = node["calendarMonth"].asText()
                signInLog.isReward = node["isReward"].asBoolean()
                sign.signInLogs.add(signInLog)
            }
            sign
        } else error(jsonNode["code"].asText())
    }

    context(HttpRequestBuilder)
    private suspend fun AliDriverEntity.appendAuth() {
        val accessToken = accessToken(this@AliDriverEntity)
        headers {
            append("Authorization", accessToken)
        }
    }

    private fun JsonNode.check() {
        if (this["success"]?.asBoolean() != true) error(this["code"].asText())
    }

    suspend fun receive(aliDriverEntity: AliDriverEntity, day: Int = LocalDate.now().dayOfMonth): String {
        val accessToken = accessToken(aliDriverEntity)
        val jsonNode = client.post("https://member.aliyundrive.com/v1/activity/sign_in_reward?_rx-s=mobile") {
            setJsonBody("""{"signInDay": $day}""")
            headers {
                append("Authorization", accessToken)
            }
        }.body<JsonNode>()
        return if (jsonNode["success"]?.asBoolean() == true) {
            "领取成功，${jsonNode["result"]["notice"].asText()}"
        } else error(jsonNode["code"].asText())
    }

    suspend fun queryTeam(aliDriverEntity: AliDriverEntity): AliDriverTeam {
        val jsonNode = client.post("https://member.aliyundrive.com/v1/activity/sign_in_team?_rx-s=mobile") {
            setJsonBody("")
            aliDriverEntity.appendAuth()
        }.body<JsonNode>()
        jsonNode.check()
        val result = jsonNode["result"]
        return AliDriverTeam(result["id"].asInt(), result["period"].asText(), result["title"].asText(),
            result["subTitle"].asText(), result["joinTeam"].asText(), result["joinCount"].asInt(), result["endTime"].asLong())
    }

    suspend fun joinTeam(aliDriverEntity: AliDriverEntity, id: Int, team: String = "blue" /* purple */) {
        val jsonNode = client.post("https://member.aliyundrive.com/v1/activity/sign_in_team_pk?_rx-s=mobile") {
            setJsonBody("""{"id": $id, "team": "$team"}""")
            aliDriverEntity.appendAuth()
        }.body<JsonNode>()
        jsonNode.check()
    }

}


data class AliDriverQrcode(
    var qrcodeUrl: String = "",
    var ck: String = "",
    var csrfToken: String = "",
    var idToken: String = "",
    var hs: String = "",
    var t: Long = 0
)

data class AliDriverAccessToken(val accessToken: String, val expire: Long) {
    fun isExpire() = System.currentTimeMillis() > expire
}

data class AliDriverTeam(val id: Int, val period: String, val title: String, val subTitle: String,
    val joinTeam: String, val joinCount: Int, val endTime: Long)

class AliDriverSign {
    var subject: String = ""
    var customMessage: String = ""
    var title: String = ""
    var isReward: Boolean = false
    var blessing: String = ""
    var signInCount: Int = 0
    var signInLogs: MutableList<SignInLog> = mutableListOf()

    class SignInLog {
        var day: Int = 0
        var status: String = ""
        var type: String = ""
        var rewardAmount: Int = 0
        var themes: String = ""
        var calendarChinese: String = ""
        var calendarDay: Int = 0
        var calendarMonth: String = ""
        var isReward: Boolean = false
    }
}
