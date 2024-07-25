package me.kuku.telegram.config

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import kotlinx.coroutines.runBlocking
import me.kuku.telegram.context.*
import me.kuku.telegram.yamlConfig
import me.kuku.utils.OkHttpUtils
import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.jvm.jvmName
import kotlin.reflect.jvm.kotlinFunction

object TelegramConfiguration {

    private val telegramSubscribeList = mutableListOf<TelegramSubscribe>()
    private val abilitySubscriberList = mutableListOf<AbilitySubscriber>()
    private val inlineQuerySubscriberList = mutableListOf<InlineQuerySubscriber>()
    private val updateFunction = mutableListOf<UpdateFunction>()
    private data class UpdateFunction(val function: KFunction<*>)

    fun init() {
        val abilitySubscriber = AbilitySubscriber()
        val inlineQuerySubscriber = InlineQuerySubscriber()

        val modules = TelegramConfig.modules
        val functions = mutableListOf<KFunction<*>>()
        for (module in modules) {
            val index = module.lastIndexOf('.')
            val clazz = module.substring(0, index)
            val name = module.substring(index + 1)
            Class.forName(clazz).declaredMethods.find { it.name == name }?.kotlinFunction?.let {
                functions.add(it)
            }
        }
        for (function in functions) {
            val type = function.extensionReceiverParameter?.type
            val kClass = type?.classifier as? KClass<*>
            when (kClass?.jvmName) {
                "me.kuku.telegram.context.AbilitySubscriber" -> {
                    function.call(abilitySubscriber)
                }
                "me.kuku.telegram.context.TelegramSubscribe" -> {
                    val telegramSubscribe = TelegramSubscribe()
                    function.call(telegramSubscribe)
                    telegramSubscribeList.add(telegramSubscribe)
                }
                "com.pengrad.telegrambot.model.Update" -> {
                    updateFunction.add(UpdateFunction(function))
                }
                "me.kuku.telegram.context.TelegramExceptionHandler" -> {
                    function.call(telegramExceptionHandler)
                }
                "me.kuku.telegram.context.MixSubscribe" -> {
                    val mixSubscribe = MixSubscribe()
                    function.call(mixSubscribe)
                    val mixSubscribeClazz = mixSubscribe::class.java
                    val abilities = mixSubscribeClazz.getDeclaredField("abilities")
                        .also { it.isAccessible = true }.get(mixSubscribe) as List<AbilitySubscriber>
                    val telegrams = mixSubscribeClazz.getDeclaredField("telegrams")
                        .also { it.isAccessible = true }.get(mixSubscribe) as List<TelegramSubscribe>
                    abilitySubscriberList.addAll(abilities)
                    telegramSubscribeList.addAll(telegrams)
                }
                "me.kuku.telegram.context.InlineQuerySubscriber" -> {
                    function.call(inlineQuerySubscriber)
                }
            }
        }

        abilitySubscriberList.add(abilitySubscriber)
        inlineQuerySubscriberList.add(inlineQuerySubscriber)
        telegramBot.setUpdatesListener {
            for (update in it) {
                Thread.startVirtualThread {
                    runBlocking {
                        for (function in updateFunction) {
                            telegramExceptionHandler.invokeHandler(TelegramContext(telegramBot, update)) {
                                function.function.callSuspend(update)
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
                        for (single in inlineQuerySubscriberList) {
                            single.invoke(telegramBot, update)
                        }
                        abilitySubscriberList.repeatCheck(telegramBot, update)
                    }
                }
            }
            UpdatesListener.CONFIRMED_UPDATES_ALL
        }
    }
}

val telegramBot: TelegramBot = run {
    val builder = OkHttpClient.Builder()
        .connectTimeout(75, TimeUnit.SECONDS)
        .writeTimeout(75, TimeUnit.SECONDS)
        .readTimeout(75, TimeUnit.SECONDS)
    if (TelegramConfig.proxyType != Proxy.Type.DIRECT) {
        builder.proxy(
            Proxy(Proxy.Type.HTTP, InetSocketAddress(TelegramConfig.proxyHost, TelegramConfig.proxyPort))
        )
    }
    val botBuilder =  TelegramBot.Builder(TelegramConfig.token)
        .okHttpClient(builder.build())
    if (TelegramConfig.url.isNotEmpty()) {
        botBuilder.apiUrl("${TelegramConfig.url}/bot")
    }
    botBuilder.build()
}


val telegramExceptionHandler = TelegramExceptionHandler()

object TelegramConfig {
    var token: String = ""
    var creatorId: Long = 0
    var proxyHost: String = ""
    var proxyPort: Int = 0
    var proxyType: Proxy.Type = Proxy.Type.DIRECT
    var url: String = ""
    var localPath: String = ""
    var api: String = ""

    var modules: List<String> = listOf()


    init {
        val telegramConfig = yamlConfig.config("ktor.telegram")
        token = telegramConfig.property("token").getString()
        creatorId = telegramConfig.property("creatorId").getString().toLong()
        proxyHost = telegramConfig.property("proxyHost").getString()
        proxyPort = telegramConfig.property("proxyPort").getString().toInt()
        proxyType = Proxy.Type.valueOf(telegramConfig.property("proxyType").getString().uppercase())
        url = telegramConfig.property("url").getString()
        localPath = telegramConfig.property("localPath").getString()
        api = telegramConfig.property("api").getString()
        modules = telegramConfig.property("modules").getList()

        val runtime = Runtime.getRuntime()
        val process = try {
            runtime.exec(arrayOf("/usr/bin/env"))
        } catch (e: Exception) {
            null
        }
        if (process != null) {
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
}

private var tempApi: String? = null
private const val apiErrorMsg: String = "访问api受限，请联系bot拥有者检查api服务器"

val api: String
    get() {
        if (tempApi == null) {
            val configApi = TelegramConfig.api
            tempApi = configApi.ifEmpty { "https://api.jpa.cc" }
        }
        try {
            val response = OkHttpUtils.get(tempApi!!)
            response.close()
            if (response.code == 200) return tempApi!!
            else error(apiErrorMsg)
        } catch (e: Exception) {
            error(apiErrorMsg)
        }
    }