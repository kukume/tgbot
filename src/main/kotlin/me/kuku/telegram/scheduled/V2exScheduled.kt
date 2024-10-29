package me.kuku.telegram.scheduled

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendMessage
import io.ktor.util.logging.*
import me.kuku.telegram.context.asyncExecute
import me.kuku.telegram.entity.ConfigService
import me.kuku.telegram.entity.Status
import me.kuku.telegram.logic.V2exLogic
import me.kuku.telegram.logic.V2exTopic
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.net.ConnectException
import java.util.concurrent.TimeUnit

@Component
class V2exScheduled(
    private val configService: ConfigService,
    private val telegramBot: TelegramBot
) {

    private val logger = LoggerFactory.getLogger(V2exScheduled::class.java)

    private var v2exId = 0

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    suspend fun push() {
        val list = try {
            V2exLogic.latestTopic()
        } catch (e: ConnectException) {
            return
        }
        if (list.isEmpty()) return
        val newList = mutableListOf<V2exTopic>()
        if (v2exId != 0) {
            for (topic in list) {
                if (topic.id <= v2exId) break
                newList.add(topic)
            }
        }
        v2exId = list[0].id
        val pushList = configService.findByV2exPush(Status.ON)
        for (v2exTopic in newList) {
            for (configEntity in pushList) {
                val str = """
                    #v2ex新帖推送
                    标题：${v2exTopic.title}
                    链接：${v2exTopic.url}
                """.trimIndent()
                kotlin.runCatching {
                    val sendMessage = SendMessage(configEntity.tgId, str)
                    telegramBot.asyncExecute(sendMessage)
                }.onFailure {
                    logger.error(it)
                }
            }
        }
    }

}
