package me.kuku.telegram.context

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.File
import com.pengrad.telegrambot.model.PhotoSize
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InputMediaPhoto
import com.pengrad.telegrambot.request.SendMediaGroup
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.request.SendPhoto
import io.ktor.client.call.*
import io.ktor.client.request.*
import me.kuku.telegram.config.TelegramConfig
import me.kuku.telegram.utils.SpringUtils
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
        for ((i, imageUrl) in picUrl.withIndex()) {
            val bytes = client.get(imageUrl).body<ByteArray>()
            val name = imageUrl.substring(imageUrl.lastIndexOf('/') + 1)
            val mediaPhoto = InputMediaPhoto(bytes).fileName(name)
            if (i == 0) mediaPhoto.caption(text)
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

suspend fun File.byteArray(): ByteArray {
    val filePath = this.filePath()
    val telegramConfig = SpringUtils.getBean<TelegramConfig>()
    return if (telegramConfig.url.isNotEmpty()) {
        var localPath = telegramConfig.localPath
        if (localPath.isEmpty()) error("获取文件失败，localPath未设置")
        val newPath = if (localPath == "/") filePath.substring(1) else filePath.substring(26)
        if (localPath.last() != '/') localPath = "$localPath/"
        val file = java.io.File(localPath + newPath)
        if (!file.exists()) error("获取文件失败，localPath设置有误")
        file.readBytes()
    } else {
        val url = "https://api.telegram.org/file/bot${telegramConfig.token}/$filePath"
        client.get(url).body()
    }
}

fun Array<PhotoSize>.max(): PhotoSize? {
    return this.maxByOrNull { it.fileSize() }
}
