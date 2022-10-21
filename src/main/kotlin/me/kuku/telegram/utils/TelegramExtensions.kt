package me.kuku.telegram.utils

import kotlinx.coroutines.*
import me.kuku.telegram.config.TelegramAbilityExceptionEvent
import me.kuku.telegram.config.TelegramCallbackExceptionEvent
import me.kuku.telegram.config.TelegramReplyExceptionEvent
import me.kuku.telegram.config.TelegramUpdateEvent
import me.kuku.utils.JobManager
import org.springframework.context.ApplicationContext
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.io.Serializable
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Predicate
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal lateinit var db: DBContext

internal lateinit var context: ApplicationContext

private suspend fun invokeAbility(messageContext: MessageContext, block: suspend MessageContext.() -> Unit) {
    runCatching {
        block.invoke(messageContext)
    }.onFailure {
        context.publishEvent(TelegramAbilityExceptionEvent(messageContext, it))
    }
}

private suspend fun invokeReply(bot: BaseAbilityBot, update: Update, block: suspend BaseAbilityBot.(Update) -> Unit) {
    runCatching {
        block.invoke(bot, update)
    }.onFailure {
        context.publishEvent(TelegramReplyExceptionEvent(bot, update, it))
    }
}

private suspend fun invokeCallback(bot: BaseAbilityBot, query: CallbackQuery, block: suspend BaseAbilityBot.(CallbackQuery) -> Unit) {
    runCatching {
        block.invoke(bot, query)
    }.onFailure {
        context.publishEvent(TelegramCallbackExceptionEvent(bot, query, it))
    }
}

fun ability(name: String, info: String = "这个命令没有描述", input: Int = 0, reply: Reply? = null, locality: Locality = Locality.ALL,
            privacy: Privacy = Privacy.PUBLIC, block: suspend MessageContext.() -> Unit): Ability {
    return Ability.builder().locality(locality).privacy(privacy).name(name).info(info).input(input).action {
        JobManager.now { invokeAbility(it, block) }
    }.also { reply?.let { r -> it.reply(r) } }.build()
}

fun reply(vararg conditions: Predicate<Update>, block: suspend BaseAbilityBot.(Update) -> Unit): Reply {
    val list = conditions.toMutableList()
    list.add { it.message != null }
    return Reply.of({ bot, upd ->
        JobManager.now { invokeReply(bot, upd, block) }
    }, list)
}

fun replyFlow(onlyIf: Predicate<Update>? = null, nextList: List<Reply>? = null, nextFlowList: List<ReplyFlow>? = null,
              block: suspend BaseAbilityBot.(Update) -> Unit): ReplyFlow {
    val builder = ReplyFlow.builder(db).action { bot, upd ->
        JobManager.now { invokeReply(bot, upd, block) }
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

fun inlineKeyboardButton(text: String, callbackData: String) = InlineKeyboardButton(text).also { it.callbackData = callbackData }

class CallBackQ {

    val threadLocal = ThreadLocal<MutableMap<String, Any>>()

    val map = mutableMapOf<String, suspend BaseAbilityBot.(CallbackQuery) -> Unit>()

    val startWithMap = mutableMapOf<String, suspend BaseAbilityBot.(CallbackQuery) -> Unit>()

    val beforeList = mutableListOf<suspend BaseAbilityBot.(CallbackQuery) -> Unit>()

    val afterList = mutableListOf<suspend BaseAbilityBot.(CallbackQuery) -> Unit>()

    fun set(key: String, value: Any) {
        val cacheMap = threadLocal.get()
        cacheMap[key] = value
    }

    inline fun <reified T: Any> get(key: String): T? {
        val cacheMap = threadLocal.get()
        return cacheMap[key] as? T
    }

    inline fun <reified T: Any> getOrFail(key: String): T {
        return get(key) ?: error("$key not found")
    }

    inline fun <reified T: Any> firstArg(): T {
        val cacheMap = threadLocal.get()
        return cacheMap.values.toList()[0] as T
    }

    inline fun <reified T: Any> secondArg(): T {
        val cacheMap = threadLocal.get()
        return cacheMap.values.toList()[1] as T
    }

    inline fun <reified T: Any> thirdArg(): T {
        val cacheMap = threadLocal.get()
        return cacheMap.values.toList()[2] as T
    }

    fun query(name: String, block: suspend BaseAbilityBot.(CallbackQuery) -> Unit): CallBackQ {
        map[name] = block
        return this
    }

    fun queryStartWith(name: String, block: suspend BaseAbilityBot.(CallbackQuery) -> Unit): CallBackQ {
        startWithMap[name] = block
        return this
    }

    fun before(block: suspend BaseAbilityBot.(CallbackQuery) -> Unit): CallBackQ {
        beforeList.add(block)
        return this
    }

    fun after(block: suspend BaseAbilityBot.(CallbackQuery) -> Unit): CallBackQ {
        afterList.add(block)
        return this
    }

}

fun callback(body: CallBackQ.() -> Unit): Reply {
    val q = CallBackQ()
    body.invoke(q)
    return Reply.of({bot, upd ->
        val callbackQuery = upd.callbackQuery
        val data = callbackQuery.data
        JobManager.now {
            launch(Dispatchers.Default + q.threadLocal.asContextElement(mutableMapOf())) {
                q.beforeList.forEach { invokeCallback(bot, callbackQuery, it) }
                q.map[data]?.let {
                    invokeCallback(bot, callbackQuery, it)
                }
                q.startWithMap.forEach { (k, v) ->
                    if (data.startsWith(k)) invokeCallback(bot, callbackQuery, v)
                }
                q.afterList.forEach { invokeCallback(bot, callbackQuery, it) }
            }
        }
    }, pre@{ upd ->
        val query = upd.callbackQuery ?: return@pre false
        val resData = query.data
        for (entry in q.map) {
            if (entry.key == resData) {
                return@pre true
            }
        }
        for (entry in q.startWithMap) {
            if (resData.startsWith(entry.key)) {
                return@pre true
            }
        }
        return@pre false
    })
}

fun callback(data: String, block: suspend BaseAbilityBot.(CallbackQuery) -> Unit): Reply {
    return Reply.of({ bot, upd ->
        JobManager.now { invokeCallback(bot, upd.callbackQuery, block) }
    }, pre@{ upd ->
        val query = upd.callbackQuery ?: return@pre false
        val resData = query.data
        resData == data
    })
}

fun callbackStartWith(data: String, block: suspend BaseAbilityBot.(CallbackQuery) -> Unit): Reply {
    return Reply.of({ bot, upd ->
        JobManager.now { invokeCallback(bot, upd.callbackQuery, block) }
    }, pre@{ upd ->
        val query = upd.callbackQuery ?: return@pre false
        val resData = query.data
        resData.startsWith(data)
    })
}

fun callbackFlow(data: String, nextList: List<Reply>? = null, nextFlowList: List<ReplyFlow>? = null,
                 block: suspend BaseAbilityBot.(CallbackQuery) -> Unit): ReplyFlow {
    val builder = ReplyFlow.builder(db).action { bot, upd ->
        JobManager.now { invokeCallback(bot, upd.callbackQuery, block) }
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
fun sendMd(message: String): Optional<Message> {
    return silent().sendMd(message, chatId())
}

context(MessageContext)
fun send(message: String): Optional<Message> {
    return silent().send(message, chatId())
}

context(MessageContext)
fun <T: Serializable, Method: BotApiMethod<T>> execute(method: Method): Optional<T> {
    return silent().execute(method)
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