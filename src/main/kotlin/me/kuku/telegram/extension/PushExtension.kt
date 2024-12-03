package me.kuku.telegram.extension

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendDocument
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.request.SendVideo
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import me.kuku.telegram.context.*
import me.kuku.telegram.entity.PushEntity
import me.kuku.telegram.entity.PushService
import me.kuku.telegram.ktor.context.KtorRouter
import me.kuku.telegram.utils.client
import me.kuku.telegram.utils.toJsonNode
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PushExtension(
    private val pushService: PushService
) {

    fun AbilitySubscriber.push() {
        userSub("push") {
            val query = inlineKeyboardButton("查看key", "queryPush")
            val recreate = inlineKeyboardButton("重新生成key", "recreateKey")
            sendMessage("请选择", InlineKeyboardMarkup(
                arrayOf(query),
                arrayOf(recreate)
            ))
        }
    }

    fun TelegramSubscribe.push() {
        callback("queryPush") {
            val pushEntity = pushService.findByTgId(tgId) ?: run {
                val pushEntity = PushEntity()
                pushEntity.tgId = tgId
                pushEntity.key = UUID.randomUUID().toString().replace("-", "")
                pushService.save(pushEntity)
                pushEntity
            }
            editMessageText("""
                TelegramBot推送
                您的key为：`${pushEntity.key}`
                接口地址为：管理员未设置接口url
                参数：key、text
            """.trimIndent(), parseMode = ParseMode.Markdown)
        }
        callback("recreateKey") {
            val pushEntity = pushService.findByTgId(tgId) ?: PushEntity().also { it.tgId = tgId }
            pushEntity.key = UUID.randomUUID().toString().replace("-", "")
            pushService.save(pushEntity)
            editMessageText("""
                TelegramBot推送
                您的key为：`${pushEntity.key}`
                接口地址为：管理员未设置接口url
                参数：key、text、parseMode
            """.trimIndent(), parseMode = ParseMode.Markdown)
        }
    }


}


@Component
class PushController(
    private val pushService: PushService,
    private val telegramBot: TelegramBot
): KtorRouter {

    override fun Routing.route() {

        route("push") {

            suspend fun ApplicationCall.push(key: String, text: String, parseMode: String?) {
                val pushEntity = pushService.findByKey(key) ?: error("key不存在")
                val tgId = pushEntity.tgId
                val sendMessage = SendMessage(tgId, "#自定义推送\n$text")
                parseMode?.let {
                    sendMessage.parseMode(ParseMode.valueOf(parseMode))
                }
                telegramBot.asyncExecute(sendMessage)
                respond("{}")
            }

            get {
                val key = call.request.queryParameters.getOrFail("key")
                val text = call.request.queryParameters.getOrFail("text")
                val parseMode = call.request.queryParameters["parseMode"]
                call.push(key, text, parseMode)
            }

            post {
                val jsonNode = call.receiveText().toJsonNode()
                val key = jsonNode["key"].asText()
                val text = jsonNode["text"].asText()
                val parseMode = jsonNode["parseMode"]?.asText()
                call.push(key, text, parseMode)
            }

            post("chat") {
                val pushBody = call.receive<PushBody>()
                val chatId = pushBody.chatId
                val messageThreadId = pushBody.messageThreadId
                val messages = pushBody.message
                val textMessages = messages.filter { it.type == PushBody.Type.TEXT }
                val sb = StringBuilder()
                textMessages.forEach { sb.append(it.content) }
                val imageList = messages.filter { it.type == PushBody.Type.IMAGE }.map { it.content }
                if (imageList.isNotEmpty()) {
                    telegramBot.sendPic(chatId, sb.toString(), imageList, messageThreadId)
                    call.respond("""{"message": "success"}""")
                    return@post
                }
                val videoMessage = messages.find { it.type == PushBody.Type.VIDEO }
                if (videoMessage != null) {
                    val sendVideo = SendVideo(chatId, client.get(videoMessage.content).body<ByteArray>())
                    messageThreadId?.let {
                        sendVideo.messageThreadId(it)
                    }
                    sendVideo.caption(sb.toString())
                    telegramBot.asyncExecute(sendVideo)
                    call.respond("""{"message": "success"}""")
                    return@post
                }
                val fileMessage = messages.find { it.type == PushBody.Type.FILE }
                if (fileMessage != null) {
                    val sendDocument = SendDocument(chatId, client.get(fileMessage.content).body<ByteArray>())
                    messageThreadId?.let {
                        sendDocument.messageThreadId(it)
                    }
                    sendDocument.caption(sb.toString())
                    telegramBot.asyncExecute(sendDocument)
                    call.respond("""{"message": "success"}""")
                    return@post
                }
                telegramBot.sendTextMessage(chatId, sb.toString(), messageThreadId)
                call.respond("""{"message": "success"}""")
            }

        }


    }

}

class PushBody {
    var chatId: Long = 0
    var messageThreadId: Int? = 0
    var message: MutableList<Message> = mutableListOf()

    class Message {
        var type: Type = Type.TEXT
        var content: String = ""
    }

    enum class Type {
        TEXT, IMAGE, VIDEO, FILE    }


}
