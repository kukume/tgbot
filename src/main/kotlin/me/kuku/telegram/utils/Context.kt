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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import me.kuku.utils.JobManager
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.collections.Map
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class Context {
    abstract val tgId: Long
    abstract val chatId: Long
    abstract val bot: TelegramBot

    fun sendMessage(text: String, replyKeyboard: Keyboard? = null, parseMode: ParseMode? = null) {
        val sendMessage = SendMessage(chatId, text)
        replyKeyboard?.let {
            sendMessage.replyMarkup(replyKeyboard)
        }
        parseMode?.let {
            sendMessage.parseMode(parseMode)
        }
        bot.execute(sendMessage)
    }
}

class AbilityContext(override val bot: TelegramBot, val update: Update): Context() {

    val message: Message = update.message()

    override val tgId: Long = message.from().id()

    override val chatId: Long = message.chat().id()

    private val messageSplit: List<String> = message.text().split(" ")

    fun firstArg(): String = messageSplit.getOrNull(1) ?: error("first argument is missing")
    fun secondArg(): String = messageSplit.getOrNull(2) ?: error("second argument is missing")
    fun thirdArg(): String = messageSplit.getOrNull(3) ?: error("third argument is missing")

}

private val returnMessageCache = mutableListOf<ReturnMessageCache>()

typealias ReturnMessageAfter = TelegramContext.() -> Unit
private data class ReturnMessageCache(val query: String, val messageId: Int, val chatId: Long, val method: BaseRequest<*, *>,
                                      val context: TelegramContext, val after: ReturnMessageAfter,
                                      var expire: Long = System.currentTimeMillis() + 1000 * 120, var top: Boolean = false, var goBackStep: Int = 1) {
    fun expire() = System.currentTimeMillis() > expire
}



private val callbackHistory = ConcurrentHashMap<String, LinkedList<History>>()
private val callbackHistoryTimer = ConcurrentHashMap<String, Long>()

private data class History(val message: Message, val data: String, var refreshReturn: Boolean)

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
                                refreshReturn: Boolean, goBackStep: Int): InlineKeyboardMarkup {
        val data = query.data()
        val historyKey = "$tgId${message.messageId()}"
        val history = callbackHistory.getOrDefault(historyKey, LinkedList())
        callbackHistoryTimer[historyKey] = System.currentTimeMillis() + 1000 * 60 * 10
        if (history.isEmpty() || (history.last != null && history.last.data != data)) {
            callbackHistory[historyKey] = history.also { it.addLast(History(message, data, refreshReturn)) }
//            if (history.size > 10) history.removeFirst()
        } else if (refreshReturn && !history.last.refreshReturn) {
            history.last.refreshReturn = true
        }
        val uuid = UUID.randomUUID().toString()
        val key = if (refreshReturn) {
            history.getOrNull(history.size - 2 - (goBackStep - 1))?.data
                ?: errorAnswerCallbackQuery("缓存已过期，请重新发送指令后重试")
        } else {
            val temp = "return_$uuid"
            val tempMessage = if (history.getOrNull(history.size - 2 - (goBackStep - 1))?.refreshReturn == true) {
                history.getOrNull(history.size - 4 - (goBackStep - 1))?.message
                    ?: errorAnswerCallbackQuery("缓存已过期，请重新发送指令后重试")
            } else message
            returnMessageCache.add(ReturnMessageCache(temp, message.messageId(), chatId,
                EditMessageText(chatId, message.messageId(), tempMessage.text())
                .replyMarkup(tempMessage.replyMarkup()), this, after, top = top, goBackStep = goBackStep))
            temp
        }
        val list = replyMarkup.inlineKeyboard().toMutableList()
        list.add(arrayOf(inlineKeyboardButton("返回", key)))
        return InlineKeyboardMarkup(*list.toTypedArray())
    }

    fun editMessageText(text: String, replyMarkup: InlineKeyboardMarkup = InlineKeyboardMarkup(),
                        parseMode: ParseMode? = null,
                        returnButton: Boolean = true,
                        top: Boolean = false,
                        refreshReturn: Boolean = false,
                        goBackStep: Int = 1,
                        after: ReturnMessageAfter = {}) {
        val messageId = message.messageId()
        val markup = if (returnButton) {
            addReturnButton(replyMarkup, after, top, refreshReturn, goBackStep)
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
                         refreshReturn: Boolean = false,
                         goBackStep: Int = 1,
                         after: ReturnMessageAfter = {}) {
        val markup = if (returnButton) {
            addReturnButton(replyMarkup, after, top, refreshReturn, goBackStep)
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
        val delList = mutableListOf<ReturnMessageCache>()
        for (cache in returnMessageCache) {
            if (data == cache.query) {
                val top = cache.top
                val groupCacheList = returnMessageCache.filter { it.messageId == mes }
                if (groupCacheList.isEmpty()) continue
                val topCache = if (top) {
                    delList.addAll(groupCacheList)
                    groupCacheList[0]
                } else {
                    val confirmIndex = groupCacheList.indexOf(cache) - (cache.goBackStep - 1)
                    delList.addAll(groupCacheList.subList(confirmIndex, groupCacheList.size))
                    groupCacheList[confirmIndex]
                }
                callbackHistory["$tgId$mes"]?.removeLastOrNull()
                val editMessageText = topCache.method
                telegramBot.execute(editMessageText)
                cache.after.invoke(cache.context)
                contextSessionCacheMap.remove(tgId.toString())
                break
            }
            if (Objects.equals(cache.messageId, mes)) {
                cache.expire = System.currentTimeMillis() + 1000 * 120
            }
        }
        if (data.startsWith("return_") && delList.isEmpty()) {
            val id = callbackQuery()?.id() ?: return
            val find = returnMessageCache.find { it.query == id }
            if (find == null) {
                val answerCallbackQuery = AnswerCallbackQuery(id)
                    .text("该条消息已过期，返回按钮不可用")
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
        val historyDeleteList = mutableListOf<String>()
        for ((k, v) in callbackHistoryTimer) {
            if (System.currentTimeMillis() > v) historyDeleteList.add(k)
        }
        historyDeleteList.forEach { callbackHistory.remove(it) }
    }

}

private typealias FilterMessage = suspend Message.() -> Boolean

private data class NextMessageValue(val continuation: Continuation<Message>, val errMessage: String, val lastMessage: List<LastMessage>, val filter: FilterMessage)

private val contextSessionCacheMap = ConcurrentHashMap<String, NextMessageValue>()

private suspend fun waitNextMessageCommon(code: String, maxTime: Long, errMessage: String, lastMessage: List<LastMessage>, filter: FilterMessage): Message {
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
