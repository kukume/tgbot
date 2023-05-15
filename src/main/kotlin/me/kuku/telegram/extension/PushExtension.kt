package me.kuku.telegram.extension

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import me.kuku.ktor.plugins.getOrFail
import me.kuku.ktor.plugins.receiveJsonNode
import me.kuku.pojo.CommonResult
import me.kuku.telegram.entity.PushEntity
import me.kuku.telegram.entity.PushService
import me.kuku.telegram.utils.AbilitySubscriber
import me.kuku.telegram.utils.TelegramSubscribe
import me.kuku.telegram.utils.inlineKeyboardButton
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
) {

    fun Routing.push() {

        route("push") {

            suspend fun ApplicationCall.push(key: String, text: String, parseMode: String?) {
                val pushEntity = pushService.findByKey(key) ?: error("key不存在")
                val tgId = pushEntity.tgId
                val sendMessage = SendMessage(tgId, "#自定义推送\n$text")
                parseMode?.let {
                    sendMessage.parseMode(ParseMode.valueOf(parseMode))
                }
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
