package me.kuku.telegram.config

import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class AbilityHandler: ApplicationListener<TelegramAbilityExceptionEvent> {

    override fun onApplicationEvent(event: TelegramAbilityExceptionEvent) {
        val messageContext = event.messageContext
        messageContext.bot().silent().send(event.ex.toString(), messageContext.chatId())
    }
}

@Component
class CallbackHandler: ApplicationListener<TelegramCallbackExceptionEvent> {
    override fun onApplicationEvent(event: TelegramCallbackExceptionEvent) {
        val bot = event.bot
        bot.silent().send(event.ex.toString(), event.query.message.chatId)
    }
}
