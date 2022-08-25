package me.kuku.telegram.utils

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import me.kuku.telegram.config.TelegramUpdateEvent
import me.kuku.utils.JobManager
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import org.telegram.abilitybots.api.bot.BaseAbilityBot
import org.telegram.abilitybots.api.db.DBContext
import org.telegram.abilitybots.api.objects.Ability
import org.telegram.abilitybots.api.objects.Locality
import org.telegram.abilitybots.api.objects.MessageContext
import org.telegram.abilitybots.api.objects.Privacy
import org.telegram.abilitybots.api.objects.Reply
import org.telegram.abilitybots.api.objects.ReplyFlow
import org.telegram.abilitybots.api.sender.SilentSender
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Predicate
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

lateinit var db: DBContext

fun ability(name: String, info: String = "这个命令没有描述", input: Int = 0, reply: Reply? = null, locality: Locality = Locality.ALL,
            privacy: Privacy = Privacy.PUBLIC, block: suspend MessageContext.() -> Unit): Ability {
    return Ability.builder().locality(locality).privacy(privacy).name(name).info(info).input(input).action {
        JobManager.now {
            block.invoke(it)
        }
    }.also { reply?.let { r -> it.reply(r) } }.build()
}

fun reply(vararg conditions: Predicate<Update>, block: suspend BaseAbilityBot.(Update) -> Unit): Reply {
    val list = conditions.toMutableList()
    list.add { it.message != null }
    return Reply.of({ bot, upd ->
        JobManager.now {
            block.invoke(bot, upd)
        }
    }, list)
}

fun replyFlow(onlyIf: Predicate<Update>? = null, nextList: List<Reply>? = null, nextFlowList: List<ReplyFlow>? = null,
              block: suspend BaseAbilityBot.(Update) -> Unit): ReplyFlow {
    val builder = ReplyFlow.builder(db).action { bot, upd ->
        JobManager.now {
            block.invoke(bot, upd)
        }
    }
    onlyIf ?: run {
        Predicate<Update> {
            it.message != null
        }
    }.let { builder.onlyIf(it) }
    nextList?.let { nextList.forEach { next -> builder.next(next) } }
    nextFlowList?.let { nextFlowList.forEach { next -> builder.next(next) } }
    return builder.build()
}

fun callback(data: String, block: suspend BaseAbilityBot.(CallbackQuery) -> Unit): Reply {
    return Reply.of({ bot, upd ->
        JobManager.now {
            block.invoke(bot, upd.callbackQuery)
        }
    }, pre@{ upd ->
        val query = upd.callbackQuery ?: return@pre false
        val resData = query.data
        resData == data
    })
}

fun callbackFlow(data: String, nextList: List<Reply>? = null, nextFlowList: List<ReplyFlow>? = null,
                 block: suspend BaseAbilityBot.(CallbackQuery) -> Unit): ReplyFlow {
    val builder = ReplyFlow.builder(db).action { bot, upd ->
        JobManager.now {
            block.invoke(bot, upd.callbackQuery)
        }
    }.onlyIf onlyIf@{ upd ->
        val query = upd.callbackQuery ?: return@onlyIf false
        val resData = query.data
        resData == data
    }
    nextList?.let { nextList.forEach { next -> builder.next(next) } }
    nextFlowList?.let { nextFlowList.forEach { next -> builder.next(next) } }
    return builder.build()
}

fun MessageContext.silent(): SilentSender = this.bot().silent()

context(MessageContext)
fun sendMd(message: String) {
    silent().sendMd(message, chatId())
}

context(MessageContext)
fun send(message: String) {
    silent().send(message, chatId())
}

context(MessageContext)
fun <T: Serializable, Method: BotApiMethod<T>> execute(method: Method) {
    silent().execute(method)
}

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
    return waitNextMessageCommon(message.from.id.toString(), maxTime)
}

fun CallbackQuery.waitNextMessage(maxTime: Long = 30000): Message {
    return waitNextMessageCommon(message.from.id.toString(), maxTime)
}

@Service
class ContextSession: ApplicationListener<TelegramUpdateEvent> {
    override fun onApplicationEvent(event: TelegramUpdateEvent) {
        val update = event.update
        val message = update.message ?: return
        contextSessionCacheMap.remove(message.from.id.toString())?.resume(message)
    }
}