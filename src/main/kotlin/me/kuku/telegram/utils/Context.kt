package me.kuku.telegram.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import me.kuku.telegram.config.TelegramBot
import me.kuku.telegram.config.TelegramUpdateEvent
import me.kuku.utils.JobManager
import org.springframework.context.ApplicationListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.telegram.abilitybots.api.bot.BaseAbilityBot
import org.telegram.abilitybots.api.objects.MessageContext
import org.telegram.abilitybots.api.sender.SilentSender
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class Context {
    abstract val tgId: Long
    abstract val chatId: Long
    abstract val bot: BaseAbilityBot

    fun sendMessage(text: String, replyKeyboard: ReplyKeyboard? = null, parseMode: String? = null) {
        val sendMessage = SendMessage.builder().text(text).chatId(chatId)
            .parseMode(parseMode).replyMarkup(replyKeyboard)
            .build()
        bot.execute(sendMessage)
    }
}

class AbilityContext(val messageContext: MessageContext): Context() {

    override val bot: BaseAbilityBot = messageContext.bot()

    val silent: SilentSender = bot.silent()

    override val tgId = messageContext.user().id

    override val chatId: Long = messageContext.chatId()

    fun firstArg(): String = messageContext.firstArg()
    fun secondArg(): String = messageContext.secondArg()
    fun thirdArg(): String = messageContext.thirdArg()


}

private val returnMessageCache = mutableListOf<ReturnMessageCache>()

typealias ReturnMessageAfter = TelegramContext.() -> Unit
private data class ReturnMessageCache(val query: String, val messageId: Int, val chatId: Long, val method: BotApiMethodSerializable,
                                      val context: TelegramContext, val after: ReturnMessageAfter,
                                      var expire: Long = System.currentTimeMillis() + 1000 * 120, var top: Boolean = false) {
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

    private fun addReturnButton(replyMarkup: InlineKeyboardMarkup, after: ReturnMessageAfter, top: Boolean): InlineKeyboardMarkup {
        val uuid = UUID.randomUUID().toString()
        val key = "return_$uuid"
        val list = replyMarkup.keyboard.toMutableList()
        list.add(listOf(inlineKeyboardButton("返回", key)))
        returnMessageCache.add(ReturnMessageCache(key, message.messageId, chatId, EditMessageText.builder().text(message.text)
            .messageId(message.messageId).chatId(chatId)
            .replyMarkup(message.replyMarkup).build(), this, after, top = top))
        return InlineKeyboardMarkup(list)
    }

    fun editMessageText(text: String, replyMarkup: InlineKeyboardMarkup = InlineKeyboardMarkup(mutableListOf()),
                        parseMode: String? = null,
                        returnButton: Boolean = true,
                        top: Boolean = false,
                        after: ReturnMessageAfter = {}) {
        val messageId = message.messageId
        val markup = if (returnButton) {
            addReturnButton(replyMarkup, after, top)
        } else replyMarkup
        val editMessageText = EditMessageText.builder().text(text)
            .messageId(messageId).chatId(chatId).parseMode(parseMode)
            .replyMarkup(markup).build()
        bot.execute(editMessageText)
    }

    fun editMessageMedia(media: InputMedia,replyMarkup: InlineKeyboardMarkup = InlineKeyboardMarkup(mutableListOf()),
                         returnButton: Boolean = true,
                         top: Boolean = false,
                         after: ReturnMessageAfter = {}) {
        val markup = if (returnButton) {
            addReturnButton(replyMarkup, after, top)
        } else replyMarkup
        val messageId = message.messageId
        val editMessageMedia = EditMessageMedia.builder().media(media)
            .messageId(messageId).chatId(chatId)
            .replyMarkup(markup).build()
        bot.execute(editMessageMedia)
    }

    fun answerCallbackQuery(text: String) {
        if (this::query.isInitialized) {
            val answerCallbackQuery = AnswerCallbackQuery.builder().callbackQueryId(query.id)
                .text(text).build()
            bot.execute(answerCallbackQuery)
        }
    }

    suspend fun nextMessage(maxTime: Long = 30000, filter: FilterMessage = { true }): Message {
        return waitNextMessageCommon(tgId.toString(), maxTime, filter)
    }
}

class AnswerCallbackQueryException(message: String): RuntimeException(message)

fun errorAnswerCallbackQuery(message: String): Nothing = throw AnswerCallbackQueryException(message)

@Component
class MonitorReturn(
    private val telegramBot: TelegramBot
) {

    fun Update.re() {
        val mes = message?.messageId ?: callbackQuery?.message?.messageId ?: return
        val data = callbackQuery?.data ?: return
        val delList = mutableListOf<ReturnMessageCache>()
        for (cache in returnMessageCache) {
            if (data == cache.query) {
                val top = cache.top
                val tgId = callbackQuery.from.id
                if (!top) {
                    val editMessageText = cache.method
                    telegramBot.execute(editMessageText)
                    cache.after.invoke(cache.context)
                    contextSessionCacheMap.remove(tgId.toString())
                    delList.add(cache)
                } else {
                    val groupCacheList = returnMessageCache.filter { it.messageId == mes }
                    if (groupCacheList.isEmpty()) continue
                    val topCache = groupCacheList[0]
                    val editMessageText = topCache.method
                    telegramBot.execute(editMessageText)
                    topCache.after.invoke(cache.context)
                    contextSessionCacheMap.remove(tgId.toString())
                    delList.addAll(groupCacheList)
                    break
                }
            }
            if (Objects.equals(cache.messageId, mes)) {
                cache.expire = System.currentTimeMillis() + 1000 * 120
            }
        }
        delList.forEach { returnMessageCache.remove(it) }
    }

//    fun TelegramSubscribe.re() {
//        callbackStartsWith("return_") {
//            val id = query.id
//            val find = returnMessageCache.find { it.query == id }
//            if (find == null) {
//                answerCallbackQuery("该条消息已过期，返回按钮不可用")
//            }
//        }
//    }

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
            val editMessageText = EditMessageText.builder().text("该消息已过期，请重新发送指令")
                .chatId(cache.chatId).messageId(cache.messageId).build()
            telegramBot.execute(editMessageText)
        }
    }

}

private typealias FilterMessage = Message.() -> Boolean

private data class NextMessageValue(val continuation: Continuation<Message>, val filter: FilterMessage)

private val contextSessionCacheMap = ConcurrentHashMap<String, NextMessageValue>()

private suspend fun waitNextMessageCommon(code: String, maxTime: Long, filter: FilterMessage): Message {
    return withContext(Dispatchers.IO) {
        try {
            withTimeout(maxTime){
                val msg = suspendCoroutine {
                    val value = NextMessageValue(it, filter)
                    contextSessionCacheMap.merge(code, value) { _, _ ->
                        error("Account $code was still waiting.")
                    }
                }
                msg
            }
        }catch (e: Exception){
            contextSessionCacheMap.remove(code)
            throw e
        }
    }
}

@Service
class ContextSessionBack(
    private val telegramBot: TelegramBot
): ApplicationListener<TelegramUpdateEvent> {
    override fun onApplicationEvent(event: TelegramUpdateEvent) {
        val update = event.update
        val message = update.message ?: return
        val tgId = update.message.from.id.toString()
        val value = contextSessionCacheMap[tgId] ?: return
        if (value.filter.invoke(message)) {
            contextSessionCacheMap.remove(tgId)?.let {
                value.continuation.resume(message).also {
                    val deleteMessage = DeleteMessage(message.chatId.toString(), message.messageId)
                    telegramBot.execute(deleteMessage)
                }
            }
        }
    }
}
