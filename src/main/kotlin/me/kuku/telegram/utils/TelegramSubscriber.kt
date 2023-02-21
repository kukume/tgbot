package me.kuku.telegram.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext
import me.kuku.telegram.config.telegramExceptionHandler
import me.kuku.utils.JobManager
import org.telegram.abilitybots.api.bot.BaseAbilityBot
import org.telegram.abilitybots.api.objects.*
import org.telegram.telegrambots.meta.api.objects.Update

class AbilitySubscriber {

    private val abilityMap = mutableMapOf<String, Ability>()

    operator fun String.invoke(block: suspend AbilityContext.() -> Unit) {
        val ability = ability(this, this, block = block)
        abilityMap[this] = ability
    }

    fun sub(name: String, info: String = "这个命令没有描述", input: Int = 0, reply: Reply? = null, locality: Locality = Locality.ALL,
            privacy: Privacy = Privacy.PUBLIC, block: suspend AbilityContext.() -> Unit) {
        val ability = ability(name, info, input, reply, locality, privacy, block)
        abilityMap[name] = ability
    }

    private fun ability(name: String, info: String = "这个命令没有描述", input: Int = 0, reply: Reply? = null, locality: Locality = Locality.ALL,
                        privacy: Privacy = Privacy.PUBLIC, block: suspend AbilityContext.() -> Unit): Ability {
        return Ability.builder().locality(locality).privacy(privacy).name(name).info(info).input(input).action {
            JobManager.now {
                invokeAbility(AbilityContext(it), block)
            }
        }.also { reply?.let { r -> it.reply(r) } }.build()
    }

    private suspend fun invokeAbility(abilityContext: AbilityContext, block: suspend AbilityContext.() -> Unit) {
        telegramExceptionHandler.invokeHandler(abilityContext) {
            block.invoke(abilityContext)
        }
    }

}

private typealias CallbackBody = suspend TelegramContext.() -> Unit
private typealias UpdateBody = Update.() -> Boolean


class TelegramSubscribe {

    val threadLocal = ThreadLocal<MutableMap<String, Any>>()
    private val before = mutableListOf<CallbackBody>()
    private val after = mutableListOf<CallbackBody>()
    private val filters: MutableList<Filter> = mutableListOf()

    class Filter(val filter: UpdateBody, var block: CallbackBody = {})
    private fun filterBuild(filter: UpdateBody, exec: CallbackBody) = Filter(filter)
    private fun CallbackBody.filterBuild(filter: UpdateBody) { filters.add(Filter(filter, this)) }
    private fun Filter.push(exec: CallbackBody) = filters.add(this.also { it.block = exec })


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

    fun before(block: suspend TelegramContext.() -> Unit) = before.add(block)
    fun after(block: suspend TelegramContext.() -> Unit) = after.add(block)

    fun callback(text: String, block: CallbackBody) =
        block.filterBuild{ callbackQuery?.data == text }

    fun callbackStartsWith(text: String, block: CallbackBody) = block.filterBuild {
        callbackQuery?.data?.startsWith(text) == true
    }

    fun callbackEndsWith(text: String, block: CallbackBody) = block.filterBuild {
        callbackQuery?.data?.endsWith(text) == true
    }

    fun callbackRegex(re: String, block: CallbackBody) = block.filterBuild {
        Regex(re).matches(callbackQuery?.data ?: "")
    }

    fun callbackContains(text: String, block: CallbackBody) = block.filterBuild {
        callbackQuery?.data?.contains(text) == true
    }

    suspend fun invoke(bot: BaseAbilityBot, update: Update) {
        update.callbackQuery ?: return
        withContext(Dispatchers.Default + threadLocal.asContextElement(mutableMapOf())) {
            for (filter in filters) {
                if (filter.filter.invoke(update)) {
                    val context = TelegramContext(bot, update)
                    for (function in before) {
                        function.invoke(context)
                    }
                    filter.block.invoke(context)
                    for (function in after) {
                        function.invoke(context)
                    }
                }
            }
        }
    }

}
