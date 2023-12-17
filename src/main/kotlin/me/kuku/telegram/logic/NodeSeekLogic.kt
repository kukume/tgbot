package me.kuku.telegram.logic

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import me.kuku.telegram.config.api
import me.kuku.telegram.entity.NodeSeekEntity
import me.kuku.utils.*

object NodeSeekLogic {


    suspend fun sign(entity: NodeSeekEntity, random: Boolean = false) {
        client.get("$api/nodeseek/sign?cookie=${entity.cookie.toUrlEncode()}&random=$random")
            .bodyAsText()
    }

    suspend fun querySign(entity: NodeSeekEntity): Int {
        val jsonNode = client.get("$api/nodeseek/sign/query?cookie=${entity.cookie.toUrlEncode()}")
            .body<JsonNode>()
        // gain current
        if (!(jsonNode["success"]?.asBoolean() ?: error("未获取到NodeSeek签到执行结果"))) error(jsonNode["message"].asText())
        return jsonNode["gain"].asInt()
    }

    suspend fun login(username: String, password: String, token: String? = null): String {
        val jsonNode = client.post("$api/nodeseek/login") {
            setFormDataContent {
                append("username", username)
                append("password", password)
                token?.let {
                    append("token", token)
                }
            }
        }.bodyAsText().toJsonNode()
        if (jsonNode.has("cookie")) return jsonNode["cookie"].asText()
        else error(jsonNode["message"].asText())
    }

}
