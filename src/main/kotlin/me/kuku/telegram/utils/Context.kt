package me.kuku.telegram.utils

import me.kuku.telegram.config.TelegramBot
import me.kuku.utils.JobManager
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.telegram.abilitybots.api.bot.BaseAbilityBot
import org.telegram.abilitybots.api.objects.MessageContext
import org.telegram.abilitybots.api.sender.SilentSender
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethodSerializable
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.media.InputMedia
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import java.util.*
import java.util.concurrent.TimeUnit

class AbilityContext(val messageContext: MessageContext) {

    val bot: BaseAbilityBot = messageContext.bot()

    val silent: SilentSender = bot.silent()

}

private val returnMessageCache = mutableListOf<ReturnMessageCache>()

typealias ReturnMessageAfter = TelegramContext.() -> Unit
private data class ReturnMessageCache(val query: String, val messageId: Int, val chatId: Long, val method: BotApiMethodSerializable,
                                      val context: TelegramContext, val after: ReturnMessageAfter,
                                      var expire: Long = System.currentTimeMillis() + 1000 * 120) {
    fun expire() = System.currentTimeMillis() > expire
}

class TelegramContext(val bot: BaseAbilityBot, val update: Update) {
    lateinit var query: CallbackQuery
    val message: Message by lazy {
        if (this::query.isInitialized) query.message else update.message
    }
    val tgId: Long by lazy {
        if (this::query.isInitialized) query.from.id else update.chatMember.from.id
    }
    val chatId: Long by lazy {
        message.chatId
    }

    init {
        update.callbackQuery?.let { query = it }
    }

    fun Message.delete(timeout: Long = 0) {
        if (timeout > 0) {
            JobManager.delay(timeout) {
                bot.execute(DeleteMessage.builder().chatId(chatId).messageId(this@delete.messageId).build())
            }
        } else {
            bot.execute(DeleteMessage.builder().chatId(chatId).messageId(this.messageId).build())
        }
    }

    fun sendTextMessage(text: String, parseMode: String = "text") {
        val sendMessage = SendMessage.builder().text(text).chatId(chatId)
            .parseMode(parseMode)
            .build()
        bot.execute(sendMessage)
    }

    private fun addReturnButton(replyMarkup: InlineKeyboardMarkup, after: ReturnMessageAfter) {
        val uuid = UUID.randomUUID().toString()
        val key = "return_$uuid"
        replyMarkup.keyboard.add(listOf(inlineKeyboardButton("返回", key)))
        returnMessageCache.add(ReturnMessageCache(key, message.messageId, chatId, EditMessageText.builder().text(message.text)
            .messageId(message.messageId).chatId(chatId)
            .replyMarkup(message.replyMarkup).build(), this, after))
    }

    fun editMessageText(text: String, replyMarkup: InlineKeyboardMarkup = InlineKeyboardMarkup(mutableListOf()),
                        returnButton: Boolean = true,
                        after: ReturnMessageAfter = {}) {
        val messageId = message.messageId
        if (returnButton) {
            addReturnButton(replyMarkup, after)
        }
        val editMessageText = EditMessageText.builder().text(text)
            .messageId(messageId).chatId(chatId)
            .replyMarkup(replyMarkup).build()
        bot.execute(editMessageText)
    }

    fun editMessageMedia(media: InputMedia,replyMarkup: InlineKeyboardMarkup = InlineKeyboardMarkup(mutableListOf()),
                         returnButton: Boolean = true,
                         after: ReturnMessageAfter = {}) {
        if (returnButton) {
            addReturnButton(replyMarkup, after)
        }
        val messageId = message.messageId
        val editMessageMedia = EditMessageMedia.builder().media(media)
            .messageId(messageId).chatId(chatId)
            .replyMarkup(replyMarkup).build()
        bot.execute(editMessageMedia)
    }
}


@Component
class MonitorReturn(
    private val telegramBot: TelegramBot
) {

    fun Update.re() {
        val data = callbackQuery?.data ?: return
        for (cache in returnMessageCache) {
            if (data == cache.query) {
                val editMessageText = cache.method
                telegramBot.execute(editMessageText)
                cache.after.invoke(cache.context)
            }
            if (cache.messageId == message.messageId) {
                cache.expire = System.currentTimeMillis() + 1000 * 120
            }
        }
    }

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    fun clear() {
        val deleteList = mutableListOf<ReturnMessageCache>()
        for (cache in returnMessageCache) {
            if (cache.expire()) {
                deleteList.add(cache)
            }
        }
        for (cache in deleteList) {
            returnMessageCache.remove(cache)
            val editMessageText = EditMessageText.builder().text("该条信息已过期")
                .chatId(cache.chatId).messageId(cache.messageId).build()
            telegramBot.execute(editMessageText)
        }
    }

}
