package me.kuku.telegram.scheduled

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendPhoto
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.util.logging.*
import kotlinx.coroutines.delay
import me.kuku.telegram.context.asyncExecute
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.HuYaLogic
import me.kuku.telegram.context.sendTextMessage
import me.kuku.telegram.utils.client
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class HuYaScheduled(
    private val huYaService: HuYaService,
    private val huYaLogic: HuYaLogic,
    private val telegramBot: TelegramBot
) {

    private val logger = LoggerFactory.getLogger(HuYaScheduled::class.java)

    private val huYaLiveMap = mutableMapOf<Long, MutableMap<Long, Boolean>>()

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    suspend fun huYa() {
        val list = huYaService.findByLive(Status.ON)
        for (huYaEntity in list) {
            kotlin.runCatching {
                delay(3000)
                val lives = huYaLogic.live(huYaEntity)
                val tgId = huYaEntity.tgId
                if (!huYaLiveMap.containsKey(tgId)) huYaLiveMap[tgId] = mutableMapOf()
                val map = huYaLiveMap[tgId]!!
                for (room in lives) {
                    val id = room.roomId
                    val b = room.isLive
                    if (map.containsKey(id)) {
                        if (map[id] != b) {
                            map[id] = b
                            val msg = if (b) "直播啦！！" else "下播啦"
                            val text = "#虎牙开播提醒\n#${room.nick} $msg\n标题：${room.liveDesc}\n分类：${room.gameName}\n链接：${room.url}"
                            val videoCaptureUrl = room.videoCaptureUrl
                            if (videoCaptureUrl.isEmpty()) telegramBot.sendTextMessage(tgId, text)
                            else {
                                client.get(videoCaptureUrl).body<ByteArray>().let {
                                    val sendPhoto =
                                        SendPhoto(tgId, text).caption(text).fileName("huYa.jpg")
                                    telegramBot.asyncExecute(sendPhoto)
                                }
                            }
                        }
                    } else map[id] = b
                }
            }.onFailure {
                logger.error(it)
            }
        }
    }

}
