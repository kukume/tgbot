package me.kuku.telegram.extension

import me.kuku.telegram.utils.AnswerCallbackQueryException
import me.kuku.telegram.utils.MessageExpiredException
import me.kuku.telegram.utils.TelegramExceptionHandler
import org.springframework.stereotype.Component
import java.net.SocketException
import kotlin.IllegalStateException

@Component
class ErrorHandler {

    fun TelegramExceptionHandler.ss() {
        abilityHandler<IllegalStateException> {
            abilityContext.sendMessage(throwable.toString())
        }
    }

    fun TelegramExceptionHandler.re() {

        handler<IllegalStateException> {
            telegramContext.editMessageText(throwable.toString())
        }

        handler<AnswerCallbackQueryException> {
            telegramContext.answerCallbackQuery(throwable.message?: "未知错误消息", throwable.showAlert)
        }

        handler<IllegalArgumentException> {
            telegramContext.editMessageText(throwable.toString())
        }

        handler<MessageExpiredException> {
            telegramContext.editMessageText(throwable.message ?: "未知错消息", returnButton = false)
        }

        handler<SocketException> {
            telegramContext.editMessageText("请求接口异常，请重试，异常信息：${throwable.message}")
        }

    }


}
