package me.kuku.telegram.extension

import me.kuku.telegram.utils.TelegramSubscribe
import org.springframework.stereotype.Component

@Component
class OtherExtension {

    fun TelegramSubscribe.other() {
        callback("notWrite") {
            answerCallbackQuery("没写")
        }
    }

}
