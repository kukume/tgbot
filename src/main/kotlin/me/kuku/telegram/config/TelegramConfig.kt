package me.kuku.telegram.config

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import me.kuku.telegram.context.*
import me.kuku.telegram.utils.SpringUtils
import okhttp3.OkHttpClient
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredMemberExtensionFunctions
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.jvm.jvmName

@Suppress("UNCHECKED_CAST")
@Component
class TelegramBean(
    private val telegramConfig: TelegramConfig
): ApplicationListener<ContextRefreshedEvent> {

    private val telegramSubscribeList = mutableListOf<TelegramSubscribe>()
    private val abilitySubscriberList = mutableListOf<AbilitySubscriber>()
    private val updateFunction = mutableListOf<UpdateFunction>()
    private data class UpdateFunction(val function: KFunction<*>, val any: Any)

    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        val applicationContext = event.applicationContext
        val names = applicationContext.beanDefinitionNames
        val clazzList = mutableListOf<Class<*>>()
        for (name in names) {
            applicationContext.getType(name)?.let {
                clazzList.add(it)
            }
        }
        val abilitySubscriber = AbilitySubscriber()
        for (clazz in clazzList) {
            val functions = kotlin.runCatching {
                clazz.kotlin.declaredMemberExtensionFunctions
            }.getOrNull() ?: continue
            for (function in functions) {
                val type = function.extensionReceiverParameter?.type
                val kClass = type?.classifier as? KClass<*>
                when (kClass?.jvmName) {
                    "me.kuku.telegram.context.AbilitySubscriber" -> {
                        val obj = applicationContext.getBean(clazz)
                        function.call(obj, abilitySubscriber)
                    }
                    "me.kuku.telegram.context.TelegramSubscribe" -> {
                        val telegramSubscribe = TelegramSubscribe()
                        val obj = applicationContext.getBean(clazz)
                        function.call(obj, telegramSubscribe)
                        telegramSubscribeList.add(telegramSubscribe)
                    }
                    "com.pengrad.telegrambot.model.Update" -> {
                        updateFunction.add(UpdateFunction(function, applicationContext.getBean(clazz)))
                    }
                    "me.kuku.telegram.context.TelegramExceptionHandler" -> {
                        val obj = applicationContext.getBean(clazz)
                        function.call(obj, telegramExceptionHandler)
                    }
                    "me.kuku.telegram.context.MixSubscribe" -> {
                        val mixSubscribe = MixSubscribe()
                        val obj = applicationContext.getBean(clazz)
                        function.call(obj, mixSubscribe)
                        val mixSubscribeClazz = mixSubscribe::class.java
                        val abilities = mixSubscribeClazz.getDeclaredField("abilities")
                            .also { it.isAccessible = true }.get(mixSubscribe) as List<AbilitySubscriber>
                        val telegrams = mixSubscribeClazz.getDeclaredField("telegrams")
                            .also { it.isAccessible = true }.get(mixSubscribe) as List<TelegramSubscribe>
                        abilitySubscriberList.addAll(abilities)
                        telegramSubscribeList.addAll(telegrams)
                    }
                }
            }
        }
        abilitySubscriberList.add(abilitySubscriber)
        val telegramBot = applicationContext.getBean(TelegramBot::class.java)
        telegramBot.setUpdatesListener {
            for (update in it) {
                Thread.startVirtualThread {
                    runBlocking {
                        for (function in updateFunction) {
                            telegramExceptionHandler.invokeHandler(TelegramContext(telegramBot, update)) {
                                function.function.callSuspend(function.any, update)
                            }
                        }
                        for (single in abilitySubscriberList) {
//                        telegramExceptionHandler.invokeHandler(AbilityContext(telegramBot, update)) {
                            single.invoke(telegramBot, update)
//                        }
                        }
                        for (telegramSubscribe in telegramSubscribeList) {
                            telegramExceptionHandler.invokeHandler(TelegramContext(telegramBot, update)) {
                                telegramSubscribe.invoke(telegramBot, update)
                            }
                        }
                    }
                }
            }
            UpdatesListener.CONFIRMED_UPDATES_ALL
        }
    }

    @Bean
    fun telegramBot(): TelegramBot {
        val builder = OkHttpClient.Builder()
            .connectTimeout(75, TimeUnit.SECONDS)
            .writeTimeout(75, TimeUnit.SECONDS)
            .readTimeout(75, TimeUnit.SECONDS)
        if (telegramConfig.proxyType != Proxy.Type.DIRECT) {
            builder.proxy(
                Proxy(Proxy.Type.HTTP, InetSocketAddress(telegramConfig.proxyHost, telegramConfig.proxyPort))
            )
        }
        val botBuilder =  TelegramBot.Builder(telegramConfig.token)
            .okHttpClient(builder.build())
        if (telegramConfig.url.isNotEmpty()) {
            botBuilder.apiUrl("${telegramConfig.url}/bot")
        }
        return botBuilder.build()
    }
}
val telegramExceptionHandler = TelegramExceptionHandler()

@Component
@ConfigurationProperties(prefix = "kuku.telegram")
class TelegramConfig {
    var token: String = ""
    var creatorId: Long = 0
    var proxyHost: String = ""
    var proxyPort: Int = 0
    var proxyType: Proxy.Type = Proxy.Type.DIRECT
    var url: String = ""
    var localPath: String = ""
    var api: String = ""

    @PostConstruct
    fun dockerInit() {
        val runtime = Runtime.getRuntime()
        val process = try {
            runtime.exec(arrayOf("/usr/bin/env"))
        } catch (e: Exception) {
            return
        }
        val text = process.inputStream.use {
            it.readAllBytes().toString(charset("utf-8"))
        }
        val line = text.split("\n")
        for (env in line) {
            val arr = env.split("=")
            if (arr.size == 2) {
                val key = arr[0].trim()
                val value = arr[1].trim()
                when (key.uppercase()) {
                    "KUKU_TELEGRAM_TOKEN" -> token = value
                    "KUKU_TELEGRAM_CREATOR_ID" -> creatorId = value.toLong()
                    "KUKU_TELEGRAM_PROXY_HOST" -> proxyHost = value
                    "KUKU_TELEGRAM_PROXY_PORT" -> proxyPort = value.toInt()
                    "KUKU_TELEGRAM_PROXY_TYPE" -> proxyType = Proxy.Type.valueOf(value.uppercase())
                    "KUKU_TELEGRAM_URL" -> url = value
                    "KUKU_LOCAL_PATH" -> localPath = value
                    "KUKU_API" -> api = value
                }
            }
        }
    }
}

val api: String by lazy {
    val api = SpringUtils.getBean<TelegramConfig>().api
    api.ifEmpty { "https://api.jpa.cc" }
}