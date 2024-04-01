package me.kuku.telegram.context

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.request.DeleteMessage
import com.pengrad.telegrambot.request.EditMessageText
import kotlinx.coroutines.*
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private val updateContextSessionCacheMap = ConcurrentHashMap<String, Continuation<Message>>()

private fun updateWaitNextMessageCommon(code: String, maxTime: Long): Message {
    return runBlocking {
        try {
            withTimeout(maxTime){
                val msg = suspendCoroutine {
                    updateContextSessionCacheMap.merge(code, it) { _, _ ->
                        throw IllegalStateException("Account $code was still waiting.")
                    }
                }
                msg
            }
        }catch (e: Exception){
            updateContextSessionCacheMap.remove(code)
            error(e.message ?: "")
        }
    }
}

fun Update.waitNextMessage(maxTime: Long = 30000): Message {
    return updateWaitNextMessageCommon(message().chat().id().toString(), maxTime)
}

@Service
class ContextSession(
    private val telegramBot: TelegramBot
) {

    fun Update.waitMessage() {
        val message = message() ?: return
        updateContextSessionCacheMap.remove(message.chat().id().toString())?.resume(message)
    }

    suspend fun Update.ss() {
        if (message() == null) return
        val tgId = message().from().id().toString()
        val value = contextSessionCacheMap[tgId] ?: return
        if (value.filter.invoke(message())) {
            contextSessionCacheMap.remove(tgId)?.let {
                value.continuation.resume(message()).also {
                    val deleteMessage = DeleteMessage(message().chat().id().toString(), message().messageId())
                    telegramBot.asyncExecute(deleteMessage)
                }
            }
        } else {
            val deleteMessage = DeleteMessage(message().chat().id().toString(), message().messageId())
            telegramBot.asyncExecute(deleteMessage)
            val lastMessage = value.lastMessage
            val editMessageText = EditMessageText(lastMessage.chatId, lastMessage.messageId, value.errMessage)
                .replyMarkup(lastMessage.replyMarkup)
            telegramBot.asyncExecute(editMessageText)
        }
    }

    fun Update.clear() {
        callbackQuery()?.data() ?: return
        contextSessionCacheMap.remove(callbackQuery().from().id().toString())?.continuation?.cancel()
    }


}

private data class LastMessage(val text: String, val chatId: Long, val replyMarkup: InlineKeyboardMarkup, val messageId: Int)

private typealias FilterMessage = suspend Message.() -> Boolean

private data class NextMessageValue(val continuation: CancellableContinuation<Message>, val errMessage: String, val lastMessage: LastMessage, val filter: FilterMessage)

private val contextSessionCacheMap = ConcurrentHashMap<String, NextMessageValue>()

suspend fun TelegramContext.nextMessage(maxTime: Long = 30000, errMessage: String = "您发送的信息有误，请重新发送",
                                        waitText: String = "请稍后......", filter: FilterMessage = { true }): Message {
    val lastMessage = LastMessage(message.text(), chatId,
        InlineKeyboardMarkup(arrayOf(inlineKeyboardButton("返回", query.data()))), message.messageId())
    val message = waitNextMessageCommon(tgId.toString(), maxTime, errMessage, lastMessage, filter)
        ?: throw CancelNextMessageException()
    editMessageText(waitText)
    return message
}

class CancelNextMessageException: RuntimeException()

private suspend fun waitNextMessageCommon(code: String, maxTime: Long, errMessage: String, lastMessage: LastMessage,
                                          filter: FilterMessage
): Message? {
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
            if (e is TimeoutCancellationException)
                throw e
            null
        }
    }
}
