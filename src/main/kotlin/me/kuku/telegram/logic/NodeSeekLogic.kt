package me.kuku.telegram.logic

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import me.kuku.telegram.entity.NodeSeekEntity
import me.kuku.utils.*

object NodeSeekLogic {

    private const val api = "https://api.jpa.cc"

    suspend fun sign(entity: NodeSeekEntity, random: Boolean = false): Int {
        val jsonNode = client.get("$api/nodeseek/sign?cookie=${entity.cookie.toUrlEncode()}&random=$random")
            .body<JsonNode>()
        // gain current
        if (!jsonNode["success"].asBoolean()) error(jsonNode["message"].asText())
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
        }.body<JsonNode>()
        if (jsonNode.has("cookie")) return jsonNode["cookie"].asText()
        else error(jsonNode["message"].asText())
    }

}
