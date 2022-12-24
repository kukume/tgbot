package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.config.TelegramBot
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.HuYaLogic
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class HuYaScheduled(
    private val huYaService: HuYaService,
    private val huYaLogic: HuYaLogic,
    private val telegramBot: TelegramBot
) {

    private val huYaLiveMap = mutableMapOf<Long, MutableMap<Long, Boolean>>()

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    suspend fun huYa() {
        val list = huYaService.findByLive(Status.ON)
        for (huYaEntity in list) {
            delay(3000)
            val baseResult = huYaLogic.live(huYaEntity)
            if (baseResult.failure()) continue
            val lives = baseResult.data()
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
                        telegramBot.silent().send("""
                            #虎牙开播提醒
                            #${room.nick} $msg
                            标题：${room.liveDesc}
                            分类：${room.gameName}
                            链接：${room.url}
                        """.trimIndent(), tgId)
                    }
                } else map[id] = b
            }
        }
    }

}
