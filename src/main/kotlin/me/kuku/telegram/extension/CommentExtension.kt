package me.kuku.telegram.extension

import com.pengrad.telegrambot.request.SendMessage
import me.kuku.telegram.config.TelegramConfig
import me.kuku.telegram.context.AbilitySubscriber
import me.kuku.telegram.context.asyncExecute
import me.kuku.telegram.context.nextMessage

fun AbilitySubscriber.comment() {
    sub("comment") {
        val creatorId = TelegramConfig.creatorId
        if (creatorId <= 0) {
            sendMessage("机器人拥有者没有设置id")
            return@sub
        }
        val message1 = sendMessage("请发送留言的内容").message()
        val message2 = nextMessage()
        val sendMessage = SendMessage(creatorId, "#留言\n${message2.text()}")
        bot.asyncExecute(sendMessage)
        message1.delete()
        message2.delete()
        sendMessage("留言成功")
    }
}