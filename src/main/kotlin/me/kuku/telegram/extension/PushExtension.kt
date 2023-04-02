package me.kuku.telegram.extension

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import me.kuku.ktor.plugins.getOrFail
import me.kuku.ktor.plugins.receiveJsonNode
import me.kuku.pojo.CommonResult
import me.kuku.telegram.config.TelegramBot
import me.kuku.telegram.entity.PushEntity
import me.kuku.telegram.entity.PushService
import me.kuku.telegram.utils.AbilitySubscriber
import me.kuku.telegram.utils.TelegramSubscribe
import me.kuku.telegram.utils.inlineKeyboardButton
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import java.util.UUID

@Component
class PushExtension(
    private val pushService: PushService,
    private val telegramBot: TelegramBot
) {

    fun AbilitySubscriber.push() {
        userSub("push") {
            val query = inlineKeyboardButton("查看key", "queryPush")
            val recreate = inlineKeyboardButton("重新生成key", "recreateKey")
            sendMessage("请选择", InlineKeyboardMarkup(
                listOf(
                    listOf(query),
                    listOf(recreate)
                )
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
                接口地址为：${telegramBot.db().getVar<String>("pushUrl").get() ?: "管理员未设置接口url"}
                参数：key、text
            """.trimIndent(), parseMode = "markdown")
        }
        callback("recreateKey") {
            val pushEntity = pushService.findByTgId(tgId) ?: PushEntity().also { it.tgId = tgId }
            pushEntity.key = UUID.randomUUID().toString().replace("-", "")
            pushService.save(pushEntity)
            editMessageText("""
                TelegramBot推送
                您的key为：`${pushEntity.key}`
                接口地址为：${telegramBot.db().getVar<String>("pushUrl").get() ?: "管理员未设置接口url"}
                参数：key、text、parseMode
            """.trimIndent(), parseMode = "markdown")
        }
    }


}


@Component
class PushController(
    private val pushService: PushService,
    private val telegramBot: TelegramBot
) {

    fun Routing.push() {

        route("push") {

            suspend fun ApplicationCall.push(key: String, text: String, parseMode: String?) {
                val pushEntity = pushService.findByKey(key) ?: error("key不存在")
                val tgId = pushEntity.tgId
                val sendMessage = SendMessage()
                sendMessage.parseMode = parseMode
                sendMessage.text = "#自定义推送\n$text"
                sendMessage.chatId = tgId.toString()
                telegramBot.execute(sendMessage)
                respond(CommonResult.success<Unit>())
            }

            get {
                val key = call.request.queryParameters.getOrFail("key")
                val text = call.request.queryParameters.getOrFail("text")
                val parseMode = call.request.queryParameters["parseMode"]
                call.push(key, text, parseMode)
            }

            post {
                val jsonNode = call.receiveJsonNode()
                val key = jsonNode.getOrFail("key").asText()
                val text = jsonNode.getOrFail("text").asText()
                val parseMode = jsonNode["parseMode"]?.asText()
                call.push(key, text, parseMode)
            }

        }


    }

}
