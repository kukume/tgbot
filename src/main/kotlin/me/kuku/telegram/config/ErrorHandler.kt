package me.kuku.telegram.config

import me.kuku.telegram.utils.silent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class AbilityHandler: ApplicationListener<TelegramAbilityExceptionEvent> {

    override fun onApplicationEvent(event: TelegramAbilityExceptionEvent) {
        val messageContext = event.messageContext
        messageContext.silent().send(event.ex.message, messageContext.chatId())
    }
}

@Component
class ReplyHandler: ApplicationListener<TelegramReplyExceptionEvent> {
    override fun onApplicationEvent(event: TelegramReplyExceptionEvent) {
        event.bot.silent().send(event.ex.message, event.update.message.chatId)
    }
}

@Component
class CallbackHandler: ApplicationListener<TelegramCallbackExceptionEvent> {
    override fun onApplicationEvent(event: TelegramCallbackExceptionEvent) {
        val bot = event.bot
        bot.silent().send(event.ex.message, event.query.message.chatId)
    }
}