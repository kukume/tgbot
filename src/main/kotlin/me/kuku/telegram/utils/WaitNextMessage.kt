package me.kuku.telegram.utils

import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.Update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import me.kuku.telegram.config.TelegramUpdateEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
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

fun Update.waitNextMessage(maxTime: Long = 30000): Message {
    return waitNextMessageCommon(message().chat().id().toString(), maxTime)
}

@Service
class ContextSession: ApplicationListener<TelegramUpdateEvent> {
    override fun onApplicationEvent(event: TelegramUpdateEvent) {
        val update = event.update
        val message = update.message() ?: return
        contextSessionCacheMap.remove(message.chat().id().toString())?.resume(message)
    }
}
