package me.kuku.telegram.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.launch
import me.kuku.telegram.config.TelegramCallbackExceptionEvent
import me.kuku.utils.JobManager
import org.telegram.abilitybots.api.bot.BaseAbilityBot
import org.telegram.abilitybots.api.objects.Reply
import org.telegram.telegrambots.meta.api.objects.CallbackQuery

private suspend fun invokeCallback(bot: BaseAbilityBot, query: CallbackQuery, block: suspend TelegramCallbackContext.() -> Unit) {
    runCatching {
        block.invoke(TelegramCallbackContext(bot, query))
    }.onFailure {
        context.publishEvent(TelegramCallbackExceptionEvent(bot, query, it))
    }
}

class CallBackQ {

    val threadLocal = ThreadLocal<MutableMap<String, Any>>()

    val map = mutableMapOf<String, suspend TelegramCallbackContext.() -> Unit>()

    private val startWithMap = mutableMapOf<String, suspend TelegramCallbackContext.() -> Unit>()

    private val beforeList = mutableListOf<suspend TelegramCallbackContext.() -> Unit>()

    private val afterList = mutableListOf<suspend TelegramCallbackContext.() -> Unit>()

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

    fun query(name: String, block: suspend TelegramCallbackContext.() -> Unit): CallBackQ {
        map[name] = block
        return this
    }

    operator fun String.invoke(block: suspend TelegramCallbackContext.() -> Unit): CallBackQ {
        map[this] = block
        return this@CallBackQ
    }

    fun queryStartWith(name: String, block: suspend TelegramCallbackContext.() -> Unit): CallBackQ {
        startWithMap[name] = block
        return this
    }

    fun before(block: suspend TelegramCallbackContext.() -> Unit): CallBackQ {
        beforeList.add(block)
        return this
    }

    fun after(block: suspend TelegramCallbackContext.() -> Unit): CallBackQ {
        afterList.add(block)
        return this
    }

    fun toReply(): Reply {
        return Reply.of({ bot, upd ->
            val callbackQuery = upd.callbackQuery
            val data = callbackQuery.data
            JobManager.now {
                launch(Dispatchers.Default + threadLocal.asContextElement(mutableMapOf())) {
                    beforeList.forEach { invokeCallback(bot, callbackQuery, it) }
                    map[data]?.let {
                        invokeCallback(bot, callbackQuery, it)
                    }
                    startWithMap.forEach { (k, v) ->
                        if (data.startsWith(k)) invokeCallback(bot, callbackQuery, v)
                    }
                    afterList.forEach { invokeCallback(bot, callbackQuery, it) }
                }
            }
        }, pre@{ upd ->
            val query = upd.callbackQuery ?: return@pre false
            val resData = query.data
            for (entry in map) {
                if (entry.key == resData) {
                    return@pre true
                }
            }
            for (entry in startWithMap) {
                if (resData.startsWith(entry.key)) {
                    return@pre true
                }
            }
            return@pre false
        })
    }

}

fun callback(body: CallBackQ.() -> Unit): Reply {
    val q = CallBackQ().apply { body() }
    return q.toReply()
}

fun callback(data: String, block: suspend TelegramCallbackContext.() -> Unit): Reply {
    return Reply.of({ bot, upd ->
        JobManager.now { invokeCallback(bot, upd.callbackQuery, block) }
    }, pre@{ upd ->
        val query = upd.callbackQuery ?: return@pre false
        val resData = query.data
        resData == data
    })
}

fun callbackStartWith(data: String, block: suspend TelegramCallbackContext.() -> Unit): Reply {
    return Reply.of({ bot, upd ->
        JobManager.now { invokeCallback(bot, upd.callbackQuery, block) }
    }, pre@{ upd ->
        val query = upd.callbackQuery ?: return@pre false
        val resData = query.data
        resData.startsWith(data)
    })
}
