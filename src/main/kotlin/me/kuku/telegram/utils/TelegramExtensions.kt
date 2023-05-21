package me.kuku.telegram.utils

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InputMediaPhoto
import com.pengrad.telegrambot.request.SendMediaGroup
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.request.SendPhoto
import io.ktor.client.call.*
import io.ktor.client.request.*
import me.kuku.utils.client

fun inlineKeyboardButton(text: String, callbackData: String): InlineKeyboardButton = InlineKeyboardButton(text).callbackData(callbackData)

suspend fun TelegramBot.sendPic(tgId: Long, text: String, picUrl: List<String>, messageThreadId: Int? = null) {
    if (picUrl.size == 1) {
        val url = picUrl[0]
        val bytes = client.get(url).body<ByteArray>()
        val sendPhoto = SendPhoto(tgId.toString(), bytes).caption(text)
        messageThreadId?.let {
            sendPhoto.messageThreadId(it)
        }
        execute(sendPhoto)
    } else if (picUrl.isEmpty()) {
        sendTextMessage(tgId, text, messageThreadId)
    } else {
        val inputMediaList = mutableListOf<InputMediaPhoto>()
        for (imageUrl in picUrl) {
            val bytes = client.get(imageUrl).body<ByteArray>()
            val name = imageUrl.substring(imageUrl.lastIndexOf('/') + 1)
            val mediaPhoto = InputMediaPhoto(bytes).caption(text).fileName(name)
            inputMediaList.add(mediaPhoto)
        }
        val sendMediaGroup = SendMediaGroup(tgId.toString(), *inputMediaList.toTypedArray())
        messageThreadId?.let {
            sendMediaGroup.messageThreadId(it)
        }
        execute(sendMediaGroup)
    }
}

fun TelegramBot.sendTextMessage(tgId: Long, text: String, messageThreadId: Int? = null) {
    val sendMessage = SendMessage(tgId, text)
    messageThreadId?.let { sendMessage.messageThreadId(it) }
    execute(sendMessage)
}
