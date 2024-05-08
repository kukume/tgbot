package me.kuku.telegram.logic

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import me.kuku.telegram.entity.GlaDosEntity
import me.kuku.utils.*

object GlaDosLogic {

    private fun JsonNode.check() {
        if (this["code"].asInt() != 0) error(this["message"].asText())
    }

    suspend fun sendCode(email: String) {
        val jsonNode = client.post("https://glados.space/api/authorization") {
            setJsonBody("""{"address":"$email","site":"glados.network"}""")
        }.body<JsonNode>()
        jsonNode.check()
    }

    suspend fun verifyCode(email: String, code: String): String {
        val response = client.post("https://glados.space/api/login") {
            setJsonBody("""{"method":"email","site":"glados.network","email":"$email","mailcode":"$code"}""")
        }
        val jsonNode = response.body<JsonNode>()
        jsonNode.check()
        return response.cookie()
    }

    suspend fun sign(entity: GlaDosEntity): String {
        val jsonNode = client.post("https://glados.space/api/user/checkin") {
            setJsonBody("""{"token":"glados.one"}""")
            cookieString(entity.cookie)
        }.bodyAsText().toJsonNode()
        jsonNode.check()
        return jsonNode["message"].asText()
    }


}
