package me.kuku.telegram.logic

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import me.kuku.telegram.entity.LeiShenEntity
import me.kuku.telegram.utils.*
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object LeiShenLogic {

    suspend fun login(phone: String, password: String): LeiShenEntity {
        val params = """
            {"os_type":4,"password":"$password","mobile_num":"$phone","src_channel":"guanwang","country_code":86,"username":"$phone","lang":"zh_CN","region_code":1,"ts":"${System.currentTimeMillis() / 1000}"}
        """.trimIndent().toJsonNode() as ObjectNode
        val sortedFields = params.fields().asSequence()
            .sortedBy { it.key }  // 对字段进行排序
            .map { entry ->
                val key = URLEncoder.encode(entry.key, "UTF-8")
                val value = URLEncoder.encode(entry.value.asText(), "UTF-8")
                "$key=$value"
            }
        val convert =  sortedFields.joinToString("&") + "&key=5C5A639C20665313622F51E93E3F2783"
        val md5 = convert.md5()
        params.put("sign", md5)
        val jsonNode = client.post("https://webapi.leigod.com/api/auth/login/v1") {
            setJsonBody(params)
        }.bodyAsText().toJsonNode()
        if (jsonNode["code"].asInt() != 0) error(jsonNode["msg"].asText())
        val data = jsonNode["data"]
        val info = data["login_info"]
        val accountToken = info["account_token"].asText()
        val nnToken = info["nn_token"].asText()
        val expiryTime = LocalDateTime.parse(info["expiry_time"].asText(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            .atZone(ZoneId.systemDefault()).toEpochSecond()
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
