package me.kuku.telegram.utils

import me.kuku.telegram.config.TelegramAbilityExceptionEvent
import me.kuku.utils.JobManager
import org.springframework.context.ApplicationContext
import org.telegram.abilitybots.api.bot.BaseAbilityBot
import org.telegram.abilitybots.api.objects.Ability
import org.telegram.abilitybots.api.objects.Locality
import org.telegram.abilitybots.api.objects.MessageContext
import org.telegram.abilitybots.api.objects.Privacy
import org.telegram.abilitybots.api.objects.Reply
import org.telegram.abilitybots.api.sender.SilentSender
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.io.Serializable
import java.util.*

internal lateinit var context: ApplicationContext

private suspend fun invokeAbility(messageContext: MessageContext, block: suspend MessageContext.() -> Unit) {
    runCatching {
        block.invoke(messageContext)
    }.onFailure {
        context.publishEvent(TelegramAbilityExceptionEvent(messageContext, it))
    }
}

fun ability(name: String, info: String = "这个命令没有描述", input: Int = 0, reply: Reply? = null, locality: Locality = Locality.ALL,
            privacy: Privacy = Privacy.PUBLIC, block: suspend MessageContext.() -> Unit): Ability {
    return Ability.builder().locality(locality).privacy(privacy).name(name).info(info).input(input).action {
        JobManager.now { invokeAbility(it, block) }
    }.also { reply?.let { r -> it.reply(r) } }.build()
}

fun inlineKeyboardButton(text: String, callbackData: String) = InlineKeyboardButton(text).also { it.callbackData = callbackData }

fun MessageContext.sendMd(message: String): Optional<Message> {
    return bot().silent().sendMd(message, chatId())
}

fun MessageContext.send(message: String): Optional<Message> {
    return bot().silent().send(message, chatId())
}

fun <T: Serializable, Method: BotApiMethod<T>> MessageContext.execute(method: Method): Optional<T> {
    return bot().silent().execute(method)
}
