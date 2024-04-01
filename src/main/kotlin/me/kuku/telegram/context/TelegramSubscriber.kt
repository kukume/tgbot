package me.kuku.telegram.context

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Chat
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.GetMe
import com.pengrad.telegrambot.request.SendMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext
import me.kuku.telegram.config.TelegramConfig
import me.kuku.telegram.config.telegramExceptionHandler
import me.kuku.telegram.entity.BotConfigService
import me.kuku.telegram.utils.SpringUtils

class AbilitySubscriber {

    private val abilityMap = mutableMapOf<String, Ability>()

    operator fun String.invoke(block: AbilityContextBody) {
        val ability = ability(this, block = block)
        abilityMap[this] = ability
    }

    fun sub(name: String, input: Int = 0, locality: Locality = Locality.USER,
            privacy: Privacy = Privacy.PUBLIC, block: AbilityContextBody
    ) {
        val ability = ability(name, input, locality, privacy, block)
        abilityMap[name] = ability
    }

    fun userSub(name: String, input: Int = 0, locality: Locality = Locality.USER,
                privacy: Privacy = Privacy.PUBLIC, block: AbilityContextBody
    ) {
        val ability = ability(name, input, locality, privacy, block)
        abilityMap[name] = ability
    }

    fun groupSub(name: String, input: Int = 0, locality: Locality = Locality.GROUP,
                 privacy: Privacy = Privacy.PUBLIC, block: AbilityContextBody
    ) {
        val ability = ability(name, input, locality, privacy, block)
        abilityMap[name] = ability
    }

    fun allSub(name: String, input: Int = 0, locality: Locality = Locality.ALL,
               privacy: Privacy = Privacy.PUBLIC, block: AbilityContextBody
    ) {
        val ability = ability(name, input, locality, privacy, block)
        abilityMap[name] = ability
    }

    private fun ability(name: String, input: Int = 0, locality: Locality = Locality.USER,
                        privacy: Privacy = Privacy.PUBLIC, block: AbilityContextBody
    ): Ability {
        return Ability(name, input, locality, privacy, block)
    }

    private suspend fun invokeAbility(abilityContext: AbilityContext, block: AbilityContextBody) {
        telegramExceptionHandler.invokeHandler(abilityContext) {
            block.invoke(abilityContext)
        }
    }
    private fun Chat.Type.isPrivate() = this == Chat.Type.Private
    private fun Chat.Type.isGroup() = this == Chat.Type.group || this == Chat.Type.supergroup

    suspend fun invoke(bot: TelegramBot, update: Update) {
        val message = update.message() ?: return
        val text = message.text() ?: message.caption() ?: return
        val messageSplit = text.split(" ")
        if (messageSplit.isEmpty()) return
        var key = messageSplit[0].removePrefix("/")
        val type = message.chat().type()
        if (type == Chat.Type.channel) return
        if (type.isGroup()) {
            key = key.removeSuffix("@$botUsername")
        }
        abilityMap[key]?.let {
            val locality = it.locality
            val status = locality == Locality.ALL ||
                    (locality == Locality.USER && type.isPrivate()) ||
                    (locality == Locality.GROUP && type.isGroup())
            if (!status) return
            val privacy = it.privacy
            val fromId = message.from().id()
            if (privacy == Privacy.CREATOR && config.creatorId != fromId) return
            if (privacy == Privacy.ADMIN) {
                var confirm = false
                val botConfigEntity = botConfigService.findByToken(config.token)
                if (botConfigEntity != null) {
                    confirm = botConfigEntity.admins.contains(fromId)
                }
                if (!confirm) confirm = config.creatorId == fromId
                if (!confirm) return
            }
            val input = it.input
            if (input >= messageSplit.size) {
                val sendMessage =
                    SendMessage(message.chat().id(), "Sorry, this feature requires $input additional input")
                message.messageThreadId()?.let { threadId ->
                    sendMessage.messageThreadId(threadId)
                }
                bot.asyncExecute(sendMessage)
            } else {
                val abilityContext = AbilityContext(bot, update)
                invokeAbility(abilityContext, it.block)
            }
        }
    }

}

private val config by lazy {
    SpringUtils.getBean<TelegramConfig>()
}

private val botConfigService by lazy {
    SpringUtils.getBean<BotConfigService>()
}

enum class Locality {
    GROUP, USER, ALL
}

enum class Privacy {
    PUBLIC, CREATOR, ADMIN
}

private val botUsername: String by lazy {
    val telegramBot = SpringUtils.getBean<TelegramBot>()
    val response = telegramBot.execute(GetMe())
    response.user().username()
}

data class Ability(val name: String,
                   val input: Int, val locality: Locality, val privacy: Privacy, val block: AbilityContextBody
)

private typealias AbilityContextBody = suspend AbilityContext.() -> Unit
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

    fun Any?.set(message: String = "该值为null") {
        if (this == null) errorAnswerCallbackQuery(message)
        set(this)
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
        block.filterBuild{ callbackQuery()?.data() == text }

    fun callbackStartsWith(text: String, block: CallbackBody) = block.filterBuild {
        callbackQuery()?.data()?.startsWith(text) == true
    }

    fun callbackEndsWith(text: String, block: CallbackBody) = block.filterBuild {
        callbackQuery()?.data()?.endsWith(text) == true
    }

    fun callbackRegex(re: String, block: CallbackBody) = block.filterBuild {
        Regex(re).matches(callbackQuery()?.data() ?: "")
    }

    fun callbackContains(text: String, block: CallbackBody) = block.filterBuild {
        callbackQuery()?.data()?.contains(text) == true
    }

    suspend fun invoke(bot: TelegramBot, update: Update) {
        update.callbackQuery() ?: return
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


class MixSubscribe {

    private val abilities = mutableListOf<AbilitySubscriber>()
    private val telegrams = mutableListOf<TelegramSubscribe>()

    fun ability(block: AbilitySubscriber.() -> Unit) {
        val abilitySubscriber = AbilitySubscriber()
        block(abilitySubscriber)
        abilities.add(abilitySubscriber)
    }

    fun telegram(block: TelegramSubscribe.() -> Unit) {
        val telegramSubscribe = TelegramSubscribe()
        block(telegramSubscribe)
        telegrams.add(telegramSubscribe)
    }

}
