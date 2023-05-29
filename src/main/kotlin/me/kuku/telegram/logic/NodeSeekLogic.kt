package me.kuku.telegram.logic

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import me.kuku.telegram.entity.NodeSeekEntity
import me.kuku.utils.*

object NodeSeekLogic {

    private const val api = "https://api.kukuqaq.com"

    suspend fun sign(entity: NodeSeekEntity, random: Boolean = false) {
        val jsonNode = client.get("$api/nodeseek/sign?cookie=${entity.cookie.toUrlEncode()}&random=$random")
            .body<JsonNode>()
        // gain current
        if (!jsonNode["success"].asBoolean()) error(jsonNode["message"].asText())
    }

    suspend fun post(): List<NodeSeekPost> {
        val jsonNode = client.get("$api/nodeseek/post").body<JsonNode>()
        return jsonNode.convertValue()
    }

    suspend fun login(username: String, password: String): String {
        val jsonNode = client.get("$api/nodeseek/login") {
            setFormDataContent {
                append("username", username)
                append("password", password)
            }
        }.body<JsonNode>()
        if (jsonNode.has("cookie")) return jsonNode["cookie"].asText()
        else error(jsonNode["message"].asText())
    }

}

class NodeSeekPost {
    var userid: Int = 0
    var title: String = ""
    var url: String = ""
    var username: String = ""
    var view: Int = 0
    var comment: Int = 0
    var category: String = ""


    fun userUrl() = "https://www.nodeseek.com/space/$userid"
    fun userAvatar() = "https://www.nodeseek.com/avatar/$userid.png"
    fun id() = MyUtils.regex("post-", "-", url)!!.toInt()
}
