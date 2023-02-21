package me.kuku.telegram.extension

import me.kuku.telegram.utils.TelegramExceptionHandler
import org.springframework.stereotype.Component
import java.lang.IllegalStateException

@Component
class ErrorHandler {

    fun TelegramExceptionHandler.re() {

        handler<IllegalStateException> {
            telegramContext.editMessageText(throwable.toString(), returnButton = false)
        }

    }

}
