package me.kuku.telegram.extension

import me.kuku.telegram.utils.*
import org.springframework.stereotype.Service
import org.telegram.abilitybots.api.util.AbilityExtension
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Service
class TestExtension: AbilityExtension {

    fun sss() = ability("test", "test") {
        val sendMessage = SendMessage()
        val button = InlineKeyboardButton("呵呵哒")
        button.callbackData = "heheda"
        val sss = listOf(button)
        val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(sss))
        sendMessage.replyMarkup = inlineKeyboardMarkup
        sendMessage.chatId = chatId().toString()
        sendMessage.text = "请选择"
        execute(sendMessage)
    }

    fun ssss() = callbackFlow("heheda", nextList = listOf(
        reply {
            silent().send("第二条消息：${it.message?.text ?: "没有"}, 请发送第三条消息", it.message.chatId)
            val nextMessage = it.waitNextMessage()
            silent().send("第三条消息：${nextMessage.text}, 请发送第四条消息", nextMessage.chatId)
        }
    )) {
        silent().send("请发送第一条消息", it.message.chatId)
    }

}