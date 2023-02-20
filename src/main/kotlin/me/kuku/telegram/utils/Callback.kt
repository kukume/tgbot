package me.kuku.telegram.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.launch
import me.kuku.telegram.config.telegramExceptionHandler
import me.kuku.utils.JobManager
import org.telegram.abilitybots.api.bot.BaseAbilityBot
import org.telegram.abilitybots.api.objects.Reply
import org.telegram.telegrambots.meta.api.objects.Update

private suspend fun invokeCallback(bot: BaseAbilityBot, update: Update, block: suspend TelegramContext.() -> Unit) {
    val context = TelegramContext(bot, update)
    telegramExceptionHandler.invokeHandler(context) {
        block.invoke(context)
    }
}

class CallbackQ {

    val threadLocal = ThreadLocal<MutableMap<String, Any>>()

    val map = mutableMapOf<String, suspend TelegramContext.() -> Unit>()

    private val startWithMap = mutableMapOf<String, suspend TelegramContext.() -> Unit>()

    private val beforeList = mutableListOf<suspend TelegramContext.() -> Unit>()

    private val afterList = mutableListOf<suspend TelegramContext.() -> Unit>()

    fun set(key: String, value: Any) {
        val cacheMap = threadLocal.get()
        cacheMap[key] = value
    }

    fun set(value: Any) {
        val name = value::class.java.simpleName
        val key = name.substring(0, 1).lowercase() + name.substring(1)
        set(key, value)
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

    fun query(name: String, block: suspend TelegramContext.() -> Unit): CallbackQ {
        map[name] = block
        return this
    }

    operator fun String.invoke(block: suspend TelegramContext.() -> Unit): CallbackQ {
        map[this] = block
        return this@CallbackQ
    }

    fun queryStartWith(name: String, block: suspend TelegramContext.() -> Unit): CallbackQ {
        startWithMap[name] = block
        return this
    }

    fun before(block: suspend TelegramContext.() -> Unit): CallbackQ {
        beforeList.add(block)
        return this
    }

    fun after(block: suspend TelegramContext.() -> Unit): CallbackQ {
        afterList.add(block)
        return this
    }

    fun toReply(): Reply {
        return Reply.of({ bot, upd ->
            val callbackQuery = upd.callbackQuery
            val data = callbackQuery.data
            JobManager.now {
                launch(Dispatchers.Default + threadLocal.asContextElement(mutableMapOf())) {
                    beforeList.forEach { invokeCallback(bot, upd, it) }
                    map[data]?.let {
                        invokeCallback(bot, upd, it)
                    }
                    startWithMap.forEach { (k, v) ->
                        if (data.startsWith(k)) invokeCallback(bot, upd, v)
                    }
                    afterList.forEach { invokeCallback(bot, upd, it) }
                    threadLocal.remove()
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

fun callback(body: CallbackQ.() -> Unit): Reply {
    val q = CallbackQ().apply { body() }
    return q.toReply()
}

fun callback(data: String, block: suspend TelegramContext.() -> Unit): Reply {
    return Reply.of({ bot, upd ->
        JobManager.now { invokeCallback(bot, upd, block) }
    }, pre@{ upd ->
        val query = upd.callbackQuery ?: return@pre false
        val resData = query.data
        resData == data
    })
}

fun callbackStartWith(data: String, block: suspend TelegramContext.() -> Unit): Reply {
    return Reply.of({ bot, upd ->
        JobManager.now { invokeCallback(bot, upd, block) }
    }, pre@{ upd ->
        val query = upd.callbackQuery ?: return@pre false
        val resData = query.data
        resData.startsWith(data)
    })
}
