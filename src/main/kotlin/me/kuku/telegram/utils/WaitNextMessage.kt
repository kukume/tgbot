package me.kuku.telegram.utils

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import me.kuku.telegram.config.TelegramUpdateEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import org.telegram.abilitybots.api.objects.MessageContext
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private val contextSessionCacheMap = ConcurrentHashMap<String, Continuation<Message>>()

private fun waitNextMessageCommon(code: String, maxTime: Long): Message {
    return runBlocking {
        try {
            withTimeout(maxTime){
                val msg = suspendCoroutine {
                    contextSessionCacheMap.merge(code, it) { _, _ ->
                        throw IllegalStateException("Account $code was still waiting.")
                    }
                }
                msg
            }
        }catch (e: Exception){
            contextSessionCacheMap.remove(code)
            error(e.message ?: "")
        }
    }
}

context(MessageContext)
fun waitNextMessage(maxTime: Long = 30000): Message {
    return waitNextMessageCommon(user().id.toString(), maxTime)
}

fun Update.waitNextMessage(maxTime: Long = 30000): Message {
    return waitNextMessageCommon(message.chatId.toString(), maxTime)
}

fun CallbackQuery.waitNextMessage(maxTime: Long = 30000): Message {
    return waitNextMessageCommon(message.chatId.toString(), maxTime)
}

@Service
class ContextSession: ApplicationListener<TelegramUpdateEvent> {
    override fun onApplicationEvent(event: TelegramUpdateEvent) {
        val update = event.update
        val message = update.message ?: return
        contextSessionCacheMap.remove(message.chatId.toString())?.resume(message)
    }
}
