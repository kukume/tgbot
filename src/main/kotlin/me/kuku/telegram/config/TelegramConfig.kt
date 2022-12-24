package me.kuku.telegram.config

import io.ktor.client.call.*
import io.ktor.client.request.*
import me.kuku.telegram.utils.context
import me.kuku.utils.OkHttpKtUtils
import me.kuku.utils.client
import org.mapdb.DBMaker
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.telegram.abilitybots.api.bot.AbilityBot
import org.telegram.abilitybots.api.bot.BaseAbilityBot
import org.telegram.abilitybots.api.db.MapDBContext
import org.telegram.abilitybots.api.objects.MessageContext
import org.telegram.abilitybots.api.util.AbilityExtension
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.DefaultBotOptions.ProxyType
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.media.InputMedia
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import java.io.File
import java.io.InputStream

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
        return TelegramBot(telegramConfig.token, telegramConfig.username, telegramConfig.creatorId, botOptions, applicationContext)
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

private fun createDbContext(botUsername: String): MapDBContext {
    if (botUsername.isEmpty()) error("kuku.telegram.username can't empty")
    val dir = File("config")
    if (!dir.exists()) dir.mkdir()
    return MapDBContext(DBMaker
        .fileDB(File("config${File.separator}$botUsername"))
        .fileMmapEnableIfSupported()
        .closeOnJvmShutdown()
        .transactionEnable()
        .make())
}

class TelegramBot(botToken: String, botUsername: String, private val creatorId: Long, botOptions: DefaultBotOptions,
                  private val applicationContext: ApplicationContext):
    AbilityBot(botToken, botUsername, createDbContext(botUsername), botOptions) {

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

    override fun getBaseUrl(): String {
        val telegramConfig = applicationContext.getBean(TelegramConfig::class.java)
        return if (telegramConfig.url.isEmpty()) super.getBaseUrl()
        else "${telegramConfig.url}/bot${botToken}/"
    }

    override fun admins(): MutableSet<Long> {
        return db.getSet(ADMINS)
    }

    suspend fun sendPic(tgId: Long, text: String, picUrl: List<String>) {
        if (picUrl.size == 1) {
            val url = picUrl[0]
            client.get(url).body<InputStream>().use {
                val sendPhoto = SendPhoto(tgId.toString(), InputFile(it, "${url.substring(url.lastIndexOf('/') + 1)}.jpg"))
                sendPhoto.caption = text
                execute(sendPhoto)
            }
        } else {
            val inputMediaList = mutableListOf<InputMedia>()
            val ii = mutableListOf<InputStream>()
            try {
                for (imageUrl in picUrl) {
                    val iis = client.get(imageUrl).body<InputStream>()
                    val name = imageUrl.substring(imageUrl.lastIndexOf('/') + 1)
                    val mediaPhoto =
                        InputMediaPhoto.builder().newMediaStream(iis).media("attach://$name")
                            .mediaName(name).isNewMedia(true).build()
                    mediaPhoto.caption = text
                    mediaPhoto.captionEntities
                    ii.add(iis)
                    inputMediaList.add(mediaPhoto)
                }
                val sendMediaGroup = SendMediaGroup(tgId.toString(), inputMediaList)
                execute(sendMediaGroup)
            } finally {
                ii.forEach { it.close() }
            }
        }
    }
}

class TelegramUpdateEvent(val update: Update): ApplicationEvent(update)

class TelegramAbilityExceptionEvent(val messageContext: MessageContext, val ex: Throwable): ApplicationEvent(messageContext)

class TelegramCallbackExceptionEvent(val bot: BaseAbilityBot, val query: CallbackQuery, val ex: Throwable): ApplicationEvent(query)

@Component
@ConfigurationProperties(prefix = "kuku.telegram")
class TelegramConfig {
    var token: String = ""
    var username: String = ""
    var creatorId: Long = 0
    var proxyHost: String = ""
    var proxyPort: Int = 0
    var proxyType: ProxyType = ProxyType.NO_PROXY
    var url: String = ""
}

