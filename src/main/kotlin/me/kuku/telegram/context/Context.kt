@file:Suppress("UNCHECKED_CAST")

package me.kuku.telegram.context

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery
import com.pengrad.telegrambot.model.InlineQuery
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.*
import com.pengrad.telegrambot.request.*
import com.pengrad.telegrambot.response.SendResponse
import me.kuku.telegram.config.telegramBot
import me.kuku.telegram.utils.CacheManager
import me.kuku.utils.JobManager
import java.io.Serializable
import java.time.Duration
import java.util.*
import kotlin.collections.ArrayList

abstract class Context {
    abstract val tgId: Long
    abstract val bot: TelegramBot
}

abstract class MessageContext: Context() {
    abstract val chatId: Long
    abstract val message: Message
    abstract val messageThreadId: Int?

    suspend fun sendMessage(text: String, replyKeyboard: Keyboard? = null, parseMode: ParseMode? = null): SendResponse {
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
        return bot.asyncExecute(sendMessage)
    }

    suspend fun Message.delete(timeout: Long = 0) {
        if (timeout > 0) {
            JobManager.delay(timeout) {
                bot.asyncExecute(DeleteMessage(chatId, this@delete.messageId()))
            }
        } else {
            bot.asyncExecute(DeleteMessage(chatId, this.messageId()))
        }
    }
}

class AbilityContext(override val bot: TelegramBot, val update: Update): MessageContext() {

    override val message: Message = update.message()

    override val tgId: Long = message.from().id()

    override val chatId: Long = message.chat().id()

    override val messageThreadId: Int? = message.messageThreadId()

    private val messageSplit: List<String> = (message.text() ?: message.caption()).split(" ")

    fun firstArg(): String = messageSplit.getOrNull(1) ?: error("first argument is missing")
    fun secondArg(): String = messageSplit.getOrNull(2) ?: error("second argument is missing")
    fun thirdArg(): String = messageSplit.getOrNull(3) ?: error("third argument is missing")

}

typealias ReturnMessageAfter = TelegramContext.() -> Unit


private val callbackHistory by lazy {
    CacheManager.getCache<String, ArrayList<History>>("callbackHistory", Duration.ofMinutes(5))
}
private val callbackHistoryKey = mutableSetOf<String>()
private val callbackAfter by lazy {
    CacheManager.getCache<String, Any>("callbackAfter", Duration.ofMinutes(5))
}

@Suppress("ConstPropertyName")
private data class History(var message: Message?, var data: String, var text: String? = null): Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}


class TelegramContext(override val bot: TelegramBot, val update: Update): MessageContext() {
    lateinit var query: CallbackQuery
    override val message: Message by lazy {
        if (this::query.isInitialized) {
            val tempMessage = query.maybeInaccessibleMessage()
            if (tempMessage is Message) {
                tempMessage
            } else {
               error("bot can't access the message")
            }
        } else update.message()
    }
    override val tgId: Long by lazy {
        if (this::query.isInitialized) query.from().id() else update.chatMember().from().id()
    }
    override val chatId: Long by lazy {
        message.chat().id()
    }
    override val messageThreadId: Int? = null

    init {
        update.callbackQuery()?.let { query = it }
    }

    private fun addReturnButton(replyMarkup: InlineKeyboardMarkup, after: ReturnMessageAfter, top: Boolean,
                                goBackStep: Int): InlineKeyboardMarkup {
        val data = query.data()
        val historyKey = "$tgId${message.messageId()}"
        val history = callbackHistory[historyKey] ?: run {
            if (callbackHistoryKey.contains(historyKey)) {
                callbackHistoryKey.remove(historyKey)
                errorMessageExpired()
            } else callbackHistoryKey.add(historyKey)
            arrayListOf()
        }
        val lastButton = message.replyMarkup().inlineKeyboard().last().last()
        val lastCallbackData = lastButton.callbackData
        if (lastCallbackData == data && lastButton.text == "返回" && history.find { it.data == lastCallbackData } == null) {
            errorAnswerCallbackQuery("该返回按钮不可用，缓存已过期")
        }
        if (history.isEmpty() || (history.lastOrNull() != null && history.last().data != data)) {
            if (history.lastOrNull() == null) {
                history.addLast(History(message, "return_${UUID.randomUUID()}", message.text()))
            } else {
                history.last().message = message
            }
            history.add(History(null, data))
            callbackHistory.put(historyKey, history)
        }
        val key = if (top) {
            history.first().data
        } else {
            val hit = history.find { it.data == data }!!
            if (hit != history.last()) {
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

    suspend fun editMessageText(text: String, replyMarkup: InlineKeyboardMarkup = InlineKeyboardMarkup(),
                        parseMode: ParseMode? = null,
                        returnButton: Boolean = true,
                        top: Boolean = false,
                        goBackStep: Int = 1,
                        after: ReturnMessageAfter = {}) {
        val messageId = message.messageId()
        val markup = if (returnButton) {
            addReturnButton(replyMarkup, after, top, goBackStep)
        } else replyMarkup
        val editMessageText = EditMessageText(chatId, messageId, text)
            .replyMarkup(markup)
        parseMode?.let { editMessageText.parseMode(parseMode) }
        bot.asyncExecute(editMessageText)
    }

    suspend fun editMessageMedia(media: InputMedia<*>, replyMarkup: InlineKeyboardMarkup = InlineKeyboardMarkup(),
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
        bot.asyncExecute(editMessageMedia)
    }

    suspend fun answerCallbackQuery(text: String, showAlert: Boolean = false) {
        if (this::query.isInitialized) {
            val answerCallbackQuery = AnswerCallbackQuery(query.id())
                .showAlert(showAlert)
                .text(text)
            bot.asyncExecute(answerCallbackQuery)
        }
    }

    suspend fun waiting() {
        editMessageText("请稍后...", returnButton = false)
    }
}

class AnswerCallbackQueryException(message: String, val showAlert: Boolean = false): RuntimeException(message)

class MessageExpiredException(message: String): RuntimeException(message)

fun errorAnswerCallbackQuery(message: String, showAlert: Boolean = false): Nothing =
    throw AnswerCallbackQueryException(message, showAlert)

fun errorMessageExpired(message: String = "该条消息已过期，请重新发送指令以进行操作"): Nothing = throw MessageExpiredException(message)

suspend fun Update.handleReturnButton() {
    val mes = message()?.messageId()?: callbackQuery()?.maybeInaccessibleMessage()?.messageId() ?: return
    val data = callbackQuery()?.data() ?: return
    val tgId = callbackQuery().from().id()
    val key = "$tgId$mes$data"
    if (data.startsWith("return_")) {
        val returnKey = "$tgId$mes"
        val list = callbackHistory[returnKey]
        val first = list?.first()
        if (first?.data == data) {
            val message = first.message!!
            val editMessageText = EditMessageText(tgId, mes, first.text)
                .replyMarkup(message.replyMarkup())
            telegramBot.asyncExecute(editMessageText)
        } else {
            val editMessageText = EditMessageText(tgId, mes, "该条消息已过期，请重新发送指令以进行操作")
            telegramBot.asyncExecute(editMessageText)
        }
        callbackHistory.remove(returnKey)
        callbackHistoryKey.remove(returnKey)
    }
    val after = callbackAfter.get(key) as? ReturnMessageAfter
    after?.invoke(TelegramContext(telegramBot, this@Update))
}

class InlineQueryContext(override val bot: TelegramBot, val update: Update): Context() {
    val inlineQuery: InlineQuery = update.inlineQuery() ?: error("inlineQuery is null")

    override val tgId: Long
        get() = inlineQuery.from().id()

    suspend fun answerInlineQuery(vararg results: InlineQueryResult<*>, cacheTime: Int? = null) {
        val answerInlineQuery = AnswerInlineQuery(inlineQuery.id(), *results)
        cacheTime?.let {
            answerInlineQuery.cacheTime(it)
        }
        bot.asyncExecute(answerInlineQuery)
    }
}
