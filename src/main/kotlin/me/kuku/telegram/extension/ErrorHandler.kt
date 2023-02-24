package me.kuku.telegram.extension

import me.kuku.telegram.utils.AnswerCallbackQueryException
import me.kuku.telegram.utils.TelegramExceptionHandler
import org.springframework.stereotype.Component
import kotlin.IllegalStateException

@Component
class ErrorHandler {

    fun TelegramExceptionHandler.re() {

        handler<IllegalStateException> {
            telegramContext.editMessageText(throwable.toString())
        }

        handler<AnswerCallbackQueryException> {
            telegramContext.answerCallbackQuery(throwable.message?: "未知错误消息")
        }

        abilityHandler<IllegalStateException> {
            abilityContext.sendMessage(throwable.toString())
        }

    }


}
