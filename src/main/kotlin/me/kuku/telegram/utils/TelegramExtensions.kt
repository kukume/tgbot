package me.kuku.telegram.utils

import io.ktor.client.call.*
import io.ktor.client.request.*
import me.kuku.telegram.config.TelegramBot
import me.kuku.telegram.config.telegramExceptionHandler
import me.kuku.utils.JobManager
import me.kuku.utils.client
import org.telegram.abilitybots.api.objects.Ability
import org.telegram.abilitybots.api.objects.Locality
import org.telegram.abilitybots.api.objects.MessageContext
import org.telegram.abilitybots.api.objects.Privacy
import org.telegram.abilitybots.api.objects.Reply
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.media.InputMedia
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.io.InputStream
import java.io.Serializable
import java.util.*

private suspend fun invokeAbility(messageContext: MessageContext, block: suspend MessageContext.() -> Unit) {
    telegramExceptionHandler.invokeHandler(AbilityContext(messageContext)) {
        block.invoke(messageContext)
    }
}

fun ability(name: String, info: String = "这个命令没有描述", input: Int = 0, reply: Reply? = null, locality: Locality = Locality.ALL,
            privacy: Privacy = Privacy.PUBLIC, block: suspend MessageContext.() -> Unit): Ability {
    return Ability.builder().locality(locality).privacy(privacy).name(name).info(info).input(input).action {
        JobManager.now { invokeAbility(it, block) }
    }.also { reply?.let { r -> it.reply(r) } }.build()
}

fun inlineKeyboardButton(text: String, callbackData: String) = InlineKeyboardButton(text).also { it.callbackData = callbackData }

fun MessageContext.sendMd(message: String): Optional<Message> {
    return bot().silent().sendMd(message, chatId())
}

fun MessageContext.send(message: String): Optional<Message> {
    return bot().silent().send(message, chatId())
}

fun <T: Serializable, Method: BotApiMethod<T>> MessageContext.execute(method: Method): Optional<T> {
    return bot().silent().execute(method)
}

suspend fun TelegramBot.sendPic(tgId: Long, text: String, picUrl: List<String>) {
    if (picUrl.size == 1) {
        val url = picUrl[0]
        client.get(url).body<InputStream>().use {
            val sendPhoto = SendPhoto(tgId.toString(), InputFile(it, "${url.substring(url.lastIndexOf('/') + 1)}.jpg"))
            sendPhoto.caption = text
            execute(sendPhoto)
        }
    } else {
        val inputMediaList = mutableListOf<InputMedia>()
        val ii = mutableListOf<InputStream>()
        try {
            for (imageUrl in picUrl) {
                val iis = client.get(imageUrl).body<InputStream>()
                val name = imageUrl.substring(imageUrl.lastIndexOf('/') + 1)
                val mediaPhoto =
                    InputMediaPhoto.builder().newMediaStream(iis).media("attach://$name")
                        .mediaName(name).isNewMedia(true).build()
                mediaPhoto.caption = text
                mediaPhoto.captionEntities
                ii.add(iis)
                inputMediaList.add(mediaPhoto)
            }
            val sendMediaGroup = SendMediaGroup(tgId.toString(), inputMediaList)
            execute(sendMediaGroup)
        } finally {
            ii.forEach { it.close() }
        }
    }
}
