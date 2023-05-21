package me.kuku.telegram.logic

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import me.kuku.telegram.entity.QqEntity
import me.kuku.telegram.entity.QqService
import me.kuku.telegram.utils.errorAnswerCallbackQuery
import me.kuku.utils.MyUtils
import me.kuku.utils.client
import me.kuku.utils.convertValue
import me.kuku.utils.cookieString
import org.springframework.stereotype.Service

@Service
class QqLogic(
    private val qqService: QqService
) {

    private suspend fun entity(tgId: Long?, url: String): QqEntity {
        val domain = MyUtils.regexOrFail("\\w*.qq.com", url)
        var qqEntity = if (tgId == null) null else qqService.findByTgIdAndDomain(tgId, domain)
        if (qqEntity == null) {
            kotlin.runCatching {
                val jsonNode = client.get("http://192.168.1.237:5462/auth?domain=$domain").body<JsonNode>()
                val qq = jsonNode["qq"].asLong()
                val sKey = jsonNode["skey"].asText()
                val psKey = jsonNode["psKey"][domain].asText()
                qqEntity = QqEntity()
                qqEntity!!.cookie = "uin=o${qq}; skey=${sKey}; p_uin=o${qq}; p_skey=${psKey};"
            }
        }
        if (qqEntity == null) errorAnswerCallbackQuery("未绑定对应域名的cookie")
        return qqEntity!!
    }

    suspend fun groupEssence(tgId: Long?, group: Long, page: Int = 0, size: Int = 20): List<QqGroupEssenceMessage> {
        val entity = entity(tgId, "qun.qq.com")
        val gtk = entity.getSKeyGtk()
        val jsonNode = client.get("https://qun.qq.com/cgi-bin/group_digest/digest_list?bkn=$gtk&bkn=$gtk&group_code=$group&page_start=$page&page_limit=$size") {
            headers {
                cookieString(entity.cookie)
            }
        }.body<JsonNode>()
        if (jsonNode["retcode"].asInt() != 0) error(jsonNode["retmsg"].asText())
        return jsonNode["data"]["msg_list"]?.convertValue() ?: listOf()
    }


}

class QqGroupEssenceMessage {
    @JsonProperty("group_code")
    var group: Long = 0
    @JsonProperty("msg_seq")
    var msgSeq: Int = 0
    @JsonProperty("msg_random")
    var msgRandom: Long = 0
    @JsonProperty("sender_uin")
    var senderUin: Long = 0
    @JsonProperty("sender_nick")
    var senderNick: String = ""
    @JsonProperty("sender_time")
    var senderTime: Long = 0
    @JsonProperty("add_digest_uin")
    var addUin: Long = 0
    @JsonProperty("add_digest_nick")
    var addNick: String = ""
    @JsonProperty("add_digest_time")
    var addTime: Long = 0
    @JsonProperty("msg_content")
    var msgContent: MutableList<MsgContent> = mutableListOf()

    fun str(): String {
        val sb = StringBuilder()
        for (msg in msgContent) {
            if (msg.msgType == 1) sb.append(msg.text)
            else if (msg.msgType == 3) sb.append("[图片]")
        }
        return sb.toString()
    }

    fun text(): String {
        val sb = StringBuilder()
        for (msg in msgContent) {
            if (msg.msgType == 1) sb.append("${msg.text} ")
        }
        return sb.toString()
    }

    class MsgContent {
        @JsonProperty("msg_type")
        var msgType: Int = 0
        var text: String = ""
        @JsonProperty("image_url")
        var imageUrl: String = ""
    }

}
