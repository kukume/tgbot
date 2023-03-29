package me.kuku.telegram.logic

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import me.kuku.telegram.entity.LeiShenEntity
import me.kuku.utils.*

object LeiShenLogic {

    suspend fun login(phone: String, password: String): LeiShenEntity {
        val jsonNode = client.post("https://webapi.leigod.com/wap/login/bind") {
            setJsonBody("""
                {"username":"$phone","password":"$password","user_type":"0","src_channel":"guanwang","code":"","country_code":86,"lang":"zh_CN"}
            """.trimIndent())
        }.body<JsonNode>()
        if (jsonNode["code"].asInt() != 0) error(jsonNode["msg"].asText())
        val data = jsonNode["data"]
        val info = data["login_info"]
        val accountToken = info["account_token"].asText()
        val nnToken = info["nn_token"].asText()
        val expiryTime = DateTimeFormatterUtils.parseToDate(info["expiry_time"].asText(), "yyyy-MM-dd HH:mm:ss").time
        return LeiShenEntity().also {
            it.accountToken = accountToken
            it.nnToken = nnToken
            it.expiryTime = expiryTime
            it.username = phone
            it.password = password
        }
    }

    suspend fun userInfo(leiShenEntity: LeiShenEntity): LeiShenUserInfo {
        val jsonNode = client.post("https://webapi.leigod.com/api/user/info") {
            setJsonBody("""
                {"account_token":"${leiShenEntity.accountToken}","lang":"zh_CN"}
            """.trimIndent())
        }.body<JsonNode>()
        if (jsonNode["code"].asInt() != 0) error(jsonNode["msg"].asText())
        return jsonNode["data"].convertValue()
    }

    suspend fun recover(leiShenEntity: LeiShenEntity) {
        val jsonNode = client.post("https://webapi.leigod.com/api/user/recover") {
            setJsonBody("""
                {"account_token":"${leiShenEntity.accountToken}","lang":"zh_CN"}
            """.trimIndent())
        }.body<JsonNode>()
        if (jsonNode["code"].asInt() != 0) error(jsonNode["msg"].asText())
    }

    suspend fun pause(leiShenEntity: LeiShenEntity) {
        val jsonNode = client.post("https://webapi.leigod.com/api/user/pause") {
            setJsonBody("""
                {"account_token":"${leiShenEntity.accountToken}","lang":"zh_CN"}
            """.trimIndent())
        }.body<JsonNode>()
        if (jsonNode["code"].asInt() != 0) error(jsonNode["msg"].asText())
    }

}


class LeiShenUserInfo {
    @JsonProperty("pause_status")
    var pauseStatus: String = ""
    @JsonProperty("pause_status_id")
    var pauseStatusId: Int = 0   // 1 暂停   0 未暂停
    @JsonProperty("expiry_time")
    var expiryTime: String = ""
}
