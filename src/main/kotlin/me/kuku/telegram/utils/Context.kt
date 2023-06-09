@file:Suppress("UNCHECKED_CAST")

package me.kuku.telegram.utils

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.InputMedia
import com.pengrad.telegrambot.model.request.Keyboard
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.*
import com.pengrad.telegrambot.response.SendResponse
import kotlinx.coroutines.*
import me.kuku.utils.JobManager
import org.ehcache.CacheManager
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.CacheEventListenerConfigurationBuilder
import org.ehcache.config.builders.ExpiryPolicyBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.ehcache.event.CacheEventListener
import org.ehcache.event.EventFiring
import org.ehcache.event.EventOrdering
import org.ehcache.event.EventType
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

abstract class Context {
    abstract val tgId: Long
    abstract val chatId: Long
    abstract val bot: TelegramBot
    abstract val message: Message

    fun sendMessage(text: String, replyKeyboard: Keyboard? = null, parseMode: ParseMode? = null): SendResponse {
        val sendMessage = SendMessage(chatId, text)
        replyKeyboard?.let {
            sendMessage.replyMarkup(replyKeyboard)
        }
        parseMode?.let {
            sendMessage.parseMode(parseMode)
        }
        message.messageThreadId()?.let {
            sendMessage.messageThreadId(it)
        }
        return bot.execute(sendMessage)
    }

    fun Message.delete(timeout: Long = 0) {
        if (timeout > 0) {
            JobManager.delay(timeout) {
                bot.execute(DeleteMessage(chatId, this@delete.messageId()))
            }
        } else {
            bot.execute(DeleteMessage(chatId, this.messageId()))
        }
    }
}

class AbilityContext(override val bot: TelegramBot, val update: Update): Context() {

    override val message: Message = update.message()

    override val tgId: Long = message.from().id()

    override val chatId: Long = message.chat().id()

    private val messageSplit: List<String> = message.text().split(" ")

    fun firstArg(): String = messageSplit.getOrNull(1) ?: error("first argument is missing")
    fun secondArg(): String = messageSplit.getOrNull(2) ?: error("second argument is missing")
    fun thirdArg(): String = messageSplit.getOrNull(3) ?: error("third argument is missing")

}

typealias ReturnMessageAfter = TelegramContext.() -> Unit


private val callbackHistory by lazy {
    val cache = SpringUtils.getBean<CacheManager>().createCache("callbackHistory",
        CacheConfigurationBuilder.newCacheConfigurationBuilder(String::class.javaObjectType, LinkedList::class.java,
            ResourcePoolsBuilder.heap(100)).withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofMinutes(2)))
        )
//    cache.runtimeConfiguration.registerCacheEventListener({
//        val list = it.oldValue as LinkedList<History>
//        val message = list.first.message!!
//        val telegramBot = SpringUtils.getBean<TelegramBot>()
//        val editMessageText = EditMessageText(message.chat().id(), message.messageId(), "该条消息已过期，请重新发送指令以进行操作")
//        telegramBot.execute(editMessageText)
//    }, EventOrdering.UNORDERED, EventFiring.ASYNCHRONOUS, EventType.EXPIRED)
    cache
}
private val callbackAfter by lazy {
    SpringUtils.getBean<CacheManager>().createCache("callbackAfter",
        CacheConfigurationBuilder.newCacheConfigurationBuilder(String::class.javaObjectType, Any::class.java,
            ResourcePoolsBuilder.heap(100)).withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofMinutes(2)))
    )
}

private data class History(var message: Message?, var data: String)

private data class LastMessage(val text: String, val chatId: Long, val replyMarkup: InlineKeyboardMarkup, val messageId: Int)

class TelegramContext(val bot: TelegramBot, val update: Update) {
    lateinit var query: CallbackQuery
    val message: Message by lazy {
        if (this::query.isInitialized) query.message() else update.message()
    }
    val tgId: Long by lazy {
        if (this::query.isInitialized) query.from().id() else update.chatMember().from().id()
    }
    val chatId: Long by lazy {
        message.chat().id()
    }

    private val lastMessageList: MutableList<LastMessage> = mutableListOf()

    init {
        update.callbackQuery()?.let { query = it }
    }

    fun Message.delete(timeout: Long = 0) {
        if (timeout > 0) {
            JobManager.delay(timeout) {
                bot.execute(DeleteMessage(chatId, this@delete.messageId()))
            }
        } else {
            bot.execute(DeleteMessage(chatId, this.messageId()))
        }
    }

    private fun addReturnButton(replyMarkup: InlineKeyboardMarkup, after: ReturnMessageAfter, top: Boolean,
                                goBackStep: Int): InlineKeyboardMarkup {
        val data = query.data()
        val historyKey = "$tgId${message.messageId()}"
        val history = callbackHistory.get(historyKey) as? LinkedList<History> ?: LinkedList()
        val lastButton = message.replyMarkup().inlineKeyboard().last().last()
        val lastCallbackData = lastButton.callbackData()
        if (lastCallbackData == data && lastButton.text() == "返回" && history.find { it.data == lastCallbackData } == null) {
            errorAnswerCallbackQuery("该返回按钮不可用，缓存已过期")
        }
        if (history.isEmpty() || (history.last != null && history.last.data != data)) {
            if (history.lastOrNull() == null) {
                history.addLast(History(message, "return_${UUID.randomUUID()}"))
            } else {
                history.last.message = message
            }
            history.add(History(null, data))
            callbackHistory.put(historyKey, history)
        }
        val key = if (top) {
            history.first.data
        } else {
            val hit = history.find { it.data == data }!!
            if (hit != history.last) {
                val index = history.indexOf(hit)
                for (i in 0 until history.size - index - 1) history.removeLast()
                history[index - 1].data
            } else {
                history[history.size - 2 - (goBackStep - 1)].data
            }
        }
        callbackAfter.put("$historyKey$key", after)
        val list = replyMarkup.inlineKeyboard().toMutableList()
        list.add(arrayOf(inlineKeyboardButton("返回", key)))
        return InlineKeyboardMarkup(*list.toTypedArray())
    }

    fun editMessageText(text: String, replyMarkup: InlineKeyboardMarkup = InlineKeyboardMarkup(),
                        parseMode: ParseMode? = null,
                        returnButton: Boolean = true,
                        top: Boolean = false,
                        goBackStep: Int = 1,
                        after: ReturnMessageAfter = {}) {
        val messageId = message.messageId()
        val markup = if (returnButton) {
            addReturnButton(replyMarkup, after, top, goBackStep)
        } else replyMarkup
        val lastMessage = LastMessage(text, chatId, markup, messageId)
        lastMessageList.add(lastMessage)
        val editMessageText = EditMessageText(chatId, messageId, text)
            .replyMarkup(markup)
        parseMode?.let { editMessageText.parseMode(parseMode) }
        bot.execute(editMessageText)
    }

    fun editMessageMedia(media: InputMedia<*>, replyMarkup: InlineKeyboardMarkup = InlineKeyboardMarkup(),
                         returnButton: Boolean = true,
                         top: Boolean = false,
                         goBackStep: Int = 1,
                         after: ReturnMessageAfter = {}) {
        val markup = if (returnButton) {
            addReturnButton(replyMarkup, after, top, goBackStep)
        } else replyMarkup
        val messageId = message.messageId()
        val editMessageMedia = EditMessageMedia(chatId, messageId, media)
            .replyMarkup(markup)
        bot.execute(editMessageMedia)
    }

    fun answerCallbackQuery(text: String, showAlert: Boolean = false) {
        if (this::query.isInitialized) {
            val answerCallbackQuery = AnswerCallbackQuery(query.id())
                .showAlert(showAlert)
                .text(text)
            bot.execute(answerCallbackQuery)
        }
    }

    suspend fun nextMessage(maxTime: Long = 30000, errMessage: String = "您发送的信息有误，请重新发送", filter: FilterMessage = { true }): Message {
        val message = waitNextMessageCommon(tgId.toString(), maxTime, errMessage, lastMessageList, filter)
        editMessageText("请稍后......")
        return message
    }

    fun waiting() {
        editMessageText("请稍后...", returnButton = false)
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
        val mes = message()?.messageId()?: callbackQuery()?.message()?.messageId() ?: return
        val data = callbackQuery()?.data() ?: return
        val tgId = callbackQuery().from().id()
        val key = "$tgId$mes$data"
        if (data.startsWith("return_")) {
            val returnKey = "$tgId$mes"
            val list = callbackHistory.get(returnKey) as? LinkedList<History>
            val first = list?.first
            if (first?.data == data) {
                val message = first.message!!
                val editMessageText = EditMessageText(tgId, mes, message.text())
                    .replyMarkup(message.replyMarkup())
                telegramBot.execute(editMessageText)
            } else {
                val answerCallbackQuery = AnswerCallbackQuery(callbackQuery().id())
                    .text("该条消息已过期，返回按钮不可用")
                telegramBot.execute(answerCallbackQuery)
            }
            callbackHistory.remove(returnKey)
        }
        contextSessionCacheMap.remove(tgId.toString())
        val after = callbackAfter.get(key) as? ReturnMessageAfter
        after?.invoke(TelegramContext(telegramBot, this@Update))
    }

}

private typealias FilterMessage = suspend Message.() -> Boolean

private data class NextMessageValue(val continuation: Continuation<Message>, val errMessage: String, val lastMessage: List<LastMessage>, val filter: FilterMessage)

private val contextSessionCacheMap = ConcurrentHashMap<String, NextMessageValue>()

private suspend fun waitNextMessageCommon(code: String, maxTime: Long, errMessage: String, lastMessage: List<LastMessage>,
                                          filter: FilterMessage): Message {
    return withContext(Dispatchers.IO) {
        try {
            withTimeout(maxTime){
                val msg = suspendCancellableCoroutine {
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
        if (message() == null) return
        val tgId = message().from().id().toString()
        val value = contextSessionCacheMap[tgId] ?: return
        if (value.filter.invoke(message())) {
            contextSessionCacheMap.remove(tgId)?.let {
                value.continuation.resume(message()).also {
                    val deleteMessage = DeleteMessage(message().chat().id().toString(), message().messageId())
                    telegramBot.execute(deleteMessage)
                }
            }
        } else {
            val deleteMessage = DeleteMessage(message().chat().id().toString(), message().messageId())
            telegramBot.execute(deleteMessage)
            val lastMessage = value.lastMessage.lastOrNull() ?: return
            val editMessageText = EditMessageText(lastMessage.chatId, lastMessage.messageId, value.errMessage)
                .replyMarkup(lastMessage.replyMarkup)
            telegramBot.execute(editMessageText)
        }
    }
}
