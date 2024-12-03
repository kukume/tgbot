package me.kuku.telegram.logic

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import me.kuku.telegram.utils.client
import me.kuku.telegram.utils.convertValue

object V2exLogic {

    suspend fun latestTopic(): List<V2exTopic> {
        val jsonNode = client.get("https://www.v2ex.com/api/topics/latest.json").body<JsonNode>()
        return jsonNode.convertValue()
    }

}


class V2exTopic {
    var node: Node = Node()
    var member: Member = Member()
    @JsonProperty("last_reply_by")
    var lastReplyBy: String = ""
    @JsonProperty("last_touched")
    var lastTouched: Long = 0
    var title: String = ""
    var url: String = ""
    var created: Long = 0
    var deleted: Int = 0
    var content: String = ""
    @JsonProperty("content_rendered")
    var contentRendered: String = ""
    @JsonProperty("last_modified")
    var lastModified: Long = 0
    var replies: Int = 0
    var id: Int = 0

    class Node {
        @JsonProperty("avatar_large")
        var avatarLarge: String = ""
        var name: String = ""
        @JsonProperty("avatar_normal")
        var avatarNormal: String = ""
        var title: String = ""
        var url: String = ""
        var topics: Int = 0
        var footer: String? = ""
        var header: String? = ""
        @JsonProperty("title_alternative")
        var titleAlternative: String = ""
        @JsonProperty("avatar_mini")
        var avatarMini: String = ""
        var stars: Int = 0
        var aliases: List<String> = listOf()
        var root: Boolean = false
        var id: Int = 0
        @JsonProperty("parent_node_name")
        var parentNodeName: String? = ""
    }

    class Member {
        var id: Int = 0
        var username: String = ""
        var url: String = ""
        var website: String? = ""
        var twitter: String? = ""
        var psn: String? = ""
        var github: String? = ""
        var btc: String? = ""
        var location: String? = ""
        var tagline: String? = ""
        var bio: String? = ""
        @JsonProperty("avatar_mini")
        var avatarMini: String = ""
        @JsonProperty("avatar_normal")
        var avatarNormal: String = ""
        @JsonProperty("avatar_large")
        var avatarLarge: String = ""
        var created: Long = 0
        @JsonProperty("last_modified")
        var lastModified: String = ""
    }


}
