package me.kuku.telegram.config

import me.kuku.telegram.utils.context
import me.kuku.telegram.utils.db
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.telegram.abilitybots.api.bot.AbilityBot
import org.telegram.abilitybots.api.bot.BaseAbilityBot
import org.telegram.abilitybots.api.objects.MessageContext
import org.telegram.abilitybots.api.util.AbilityExtension
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.DefaultBotOptions.ProxyType
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

@Component
class TelegramBean(
    private val telegramConfig: TelegramConfig,
    private val applicationContext: ApplicationContext
) {

    @Bean
    fun telegramBot(): TelegramBot {
        val botOptions = DefaultBotOptions()
        if (telegramConfig.proxyType != ProxyType.NO_PROXY) {
            botOptions.proxyHost = telegramConfig.proxyHost
            botOptions.proxyPort = telegramConfig.proxyPort
            botOptions.proxyType = telegramConfig.proxyType
        }
        context = applicationContext
        return TelegramBot(telegramConfig.token, telegramConfig.username, telegramConfig.creatorId, botOptions, applicationContext).also {
            db = it.db()
        }
    }

}

@Component
class ApplicationStart(
    private val applicationContext: ApplicationContext,
    private val telegramBot: TelegramBot,
    private val telegramConfig: TelegramConfig
): ApplicationListener<ApplicationReadyEvent> {

    override fun onApplicationEvent(event: ApplicationReadyEvent) {

        fun interfaces(clazz: Class<*>, list: MutableList<Class<*>>) {
            list.addAll(clazz.interfaces)
            if (clazz.superclass != null && clazz.superclass != Any::class.java) interfaces(clazz.superclass, list)
        }

        if (telegramConfig.token.isNotEmpty()) {
            val names = applicationContext.beanDefinitionNames
            val clazzList = mutableListOf<Class<*>>()
            for (name in names) {
                applicationContext.getType(name)?.let {
                    clazzList.add(it)
                }
            }
            for (clazz in clazzList) {
                mutableListOf<Class<*>>()
                    .also { interfaces(clazz, it) }.takeIf { it.contains(AbilityExtension::class.java) }?.let {
                    val ob = applicationContext.getBean(clazz) as? AbilityExtension
                    ob?.apply {
                        telegramBot.addExtension(this)
                    }
                }
            }
            val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
            botsApi.registerBot(telegramBot)
        }
    }
}

class TelegramBot(botToken: String, botUsername: String, private val creatorId: Long, botOptions: DefaultBotOptions,
                  private val applicationContext: ApplicationContext):
    AbilityBot(botToken, botUsername, botOptions) {

    override fun creatorId() = creatorId

    public override fun addExtension(extension: AbilityExtension) {
        super.addExtension(extension)
    }

    override fun onUpdateReceived(update: Update) {
        super.onUpdateReceived(update)
        applicationContext.publishEvent(TelegramUpdateEvent(update))
    }

    override fun blacklist(): MutableSet<Long> {
        return db.getSet(BLACKLIST)
    }

    override fun admins(): MutableSet<Long> {
        return db.getSet(ADMINS)
    }
}

class TelegramUpdateEvent(val update: Update): ApplicationEvent(update)

class TelegramAbilityExceptionEvent(val messageContext: MessageContext, val ex: Throwable): ApplicationEvent(messageContext)

class TelegramCallbackExceptionEvent(val bot: BaseAbilityBot, val query: CallbackQuery, val ex: Throwable): ApplicationEvent(query)

class TelegramReplyExceptionEvent(val bot: BaseAbilityBot, val update: Update, val ex: Throwable): ApplicationEvent(update)

@Component
@ConfigurationProperties(prefix = "kuku.telegram")
class TelegramConfig {
    var token: String = ""
    var username: String = ""
    var creatorId: Long = 0
    var proxyHost: String = ""
    var proxyPort: Int = 0
    var proxyType: ProxyType = ProxyType.NO_PROXY
}

