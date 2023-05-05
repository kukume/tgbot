package me.kuku.telegram.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import me.kuku.telegram.config.TelegramBot
import me.kuku.utils.JobManager
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
import org.telegram.telegrambots.meta.api.objects.Chat
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


private val callbackHistory = mutableMapOf<String, LinkedList<History>>()

private data class History(val message: Message, val data: String, val refreshReturn: Boolean)

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

    val lastMessageList: MutableList<Message> = mutableListOf()

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

    private fun addReturnButton(replyMarkup: InlineKeyboardMarkup, after: ReturnMessageAfter, top: Boolean,
                                refreshReturn: Boolean): InlineKeyboardMarkup {
        val data = query.data
        val historyKey = "$tgId${message.messageId}"
        val history = callbackHistory.getOrDefault(historyKey, LinkedList())
        if (history.isEmpty() || (history.last != null && history.last.data != data)) {
            callbackHistory[historyKey] = history.also { it.addLast(History(message, data, refreshReturn)) }
            if (history.size > 3) history.removeFirst()
        }
        val uuid = UUID.randomUUID().toString()
        val key = if (refreshReturn) history[history.size - 2].data else {
            val temp = "return_$uuid"
            val tempMessage = if (history.getOrNull(history.size - 2)?.refreshReturn == true) history[history.size -3].message else message
            returnMessageCache.add(ReturnMessageCache(temp, message.messageId, chatId, EditMessageText.builder().text(tempMessage.text)
                .messageId(message.messageId).chatId(chatId)
                .replyMarkup(tempMessage.replyMarkup).build(), this, after, top = top))
            temp
        }
        val list = replyMarkup.keyboard.toMutableList()
        list.add(listOf(inlineKeyboardButton("返回", key)))
        return InlineKeyboardMarkup(list)
    }

    fun editMessageText(text: String, replyMarkup: InlineKeyboardMarkup = InlineKeyboardMarkup(mutableListOf()),
                        parseMode: String? = null,
                        returnButton: Boolean = true,
                        top: Boolean = false,
                        refreshReturn: Boolean = false,
                        after: ReturnMessageAfter = {}) {
        val messageId = message.messageId
        val markup = if (returnButton) {
            addReturnButton(replyMarkup, after, top, refreshReturn)
        } else replyMarkup
        val lastMessage = Message()
        lastMessage.text = text
        lastMessage.chat = Chat(chatId, "")
        lastMessage.replyMarkup = markup
        lastMessage.messageId = messageId
        lastMessageList.add(lastMessage)
        val editMessageText = EditMessageText.builder().text(text)
            .messageId(messageId).chatId(chatId).parseMode(parseMode)
            .replyMarkup(markup).build()
        bot.execute(editMessageText)
    }

    fun editMessageMedia(media: InputMedia,replyMarkup: InlineKeyboardMarkup = InlineKeyboardMarkup(mutableListOf()),
                         returnButton: Boolean = true,
                         top: Boolean = false,
                         refreshReturn: Boolean = false,
                         after: ReturnMessageAfter = {}) {
        val markup = if (returnButton) {
            addReturnButton(replyMarkup, after, top, refreshReturn)
        } else replyMarkup
        val messageId = message.messageId
        val editMessageMedia = EditMessageMedia.builder().media(media)
            .messageId(messageId).chatId(chatId)
            .replyMarkup(markup).build()
        bot.execute(editMessageMedia)
    }

    fun answerCallbackQuery(text: String, showAlert: Boolean = false) {
        if (this::query.isInitialized) {
            val answerCallbackQuery = AnswerCallbackQuery.builder().callbackQueryId(query.id)
                .showAlert(showAlert)
                .text(text).build()
            bot.execute(answerCallbackQuery)
        }
    }

    suspend fun nextMessage(maxTime: Long = 30000, errMessage: String = "您发送的信息有误，请重新发送", filter: FilterMessage = { true }): Message {
        val message = waitNextMessageCommon(tgId.toString(), maxTime, errMessage, lastMessageList, filter)
        editMessageText("请稍后......")
        return message
    }
}

class AnswerCallbackQueryException(message: String, val showAlert: Boolean = false): RuntimeException(message)

class MessageExpiredException(message: String): RuntimeException(message)

fun errorAnswerCallbackQuery(message: String, showAlert: Boolean = false): Nothing =
    throw AnswerCallbackQueryException(message, showAlert)

fun errorMessageExpired(message: String): Nothing = throw MessageExpiredException(message)

@Component
class MonitorReturn(
    private val telegramBot: TelegramBot
) {

    fun Update.re() {
        val mes = message?.messageId ?: callbackQuery?.message?.messageId ?: return
        val data = callbackQuery?.data ?: return
        val tgId = callbackQuery.from.id
        val delList = mutableListOf<ReturnMessageCache>()
        for (cache in returnMessageCache) {
            if (data == cache.query) {
                val top = cache.top
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
        if (data.startsWith("return_") && delList.isEmpty()) {
            val id = callbackQuery?.id ?: return
            val find = returnMessageCache.find { it.query == id }
            if (find == null) {
                val answerCallbackQuery = AnswerCallbackQuery.builder().callbackQueryId(id)
                    .text("该条消息已过期，返回按钮不可用").build()
                telegramBot.execute(answerCallbackQuery)
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
//            val editMessageText = EditMessageText.builder().text("该消息已过期，请重新发送指令")
//                .chatId(cache.chatId).messageId(cache.messageId).build()
//            telegramBot.execute(editMessageText)
        }
    }

}

private typealias FilterMessage = suspend Message.() -> Boolean

private data class NextMessageValue(val continuation: Continuation<Message>, val errMessage: String, val lastMessage: List<Message>, val filter: FilterMessage)

private val contextSessionCacheMap = ConcurrentHashMap<String, NextMessageValue>()

private suspend fun waitNextMessageCommon(code: String, maxTime: Long, errMessage: String, lastMessage: List<Message>, filter: FilterMessage): Message {
    return withContext(Dispatchers.IO) {
        try {
            withTimeout(maxTime){
                val msg = suspendCoroutine {
                    val value = NextMessageValue(it, errMessage, lastMessage, filter)
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
) {

    suspend fun Update.ss() {
        if (message == null) return
        val tgId = message.from.id.toString()
        val value = contextSessionCacheMap[tgId] ?: return
        if (value.filter.invoke(message)) {
            contextSessionCacheMap.remove(tgId)?.let {
                value.continuation.resume(message).also {
                    val deleteMessage = DeleteMessage(message.chatId.toString(), message.messageId)
                    telegramBot.execute(deleteMessage)
                }
            }
        } else {
            val deleteMessage = DeleteMessage(message.chatId.toString(), message.messageId)
            telegramBot.execute(deleteMessage)
            val lastMessage = value.lastMessage.lastOrNull() ?: return
            val editMessageText = EditMessageText.builder().text(value.errMessage)
                .messageId(lastMessage.messageId).chatId(lastMessage.chatId)
                .replyMarkup(lastMessage.replyMarkup).build()
            telegramBot.execute(editMessageText)
        }
    }
}
