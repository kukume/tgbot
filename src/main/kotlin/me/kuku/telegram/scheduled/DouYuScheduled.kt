package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.config.TelegramBot
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.DouYuFish
import me.kuku.telegram.logic.DouYuLogic
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class DouYuScheduled(
    private val douYuService: DouYuService,
    private val douYuLogic: DouYuLogic,
    private val logService: LogService,
    private val telegramBot: TelegramBot
) {

    private val douYuLiveMap = mutableMapOf<Long, MutableMap<Long, Boolean>>()

    private val douYuPushMap = mutableMapOf<Long, Long>()

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    suspend fun douYu() {
        val list = douYuService.findByLive(Status.ON)
        for (douYuEntity in list) {
            val baseResult = douYuLogic.room(douYuEntity)
            delay(3000)
            if (baseResult.failure()) continue
            val rooms = baseResult.data()
            val tgId = douYuEntity.tgId
            if (!douYuLiveMap.containsKey(tgId)) douYuLiveMap[tgId] = mutableMapOf()
            val map = douYuLiveMap[tgId]!!
            for (room in rooms) {
                val id = room.roomId
                val b = room.showStatus
                if (map.containsKey(id)) {
                    if (map[id] != b) {
                        map[id] = b
                        val msg = if (b) "直播啦！！" else "下播啦"
                        telegramBot.silent().send("""
                            #斗鱼开播提醒
                            #${room.nickName} $msg
                            标题：${room.name}
                            分类：${room.gameName}
                            在线：${room.online}
                            链接：${room.url}
                        """.trimIndent(), tgId)
                    }
                } else map[id] = b
            }
        }
    }

    @Scheduled(cron = "0 3 6 * * ?")
    suspend fun douYuSign() {
        val list = douYuService.findByFishGroup(Status.ON)
        for (douYuEntity in list) {
            val logEntity = LogEntity().also {
                it.tgId = douYuEntity.tgId
                it.type = LogType.DouYu
            }
            kotlin.runCatching {
                douYuLogic.fishGroup(douYuEntity)
                logEntity.text = "成功"
            }.onFailure {
                logEntity.text = "失败"
                logEntity.sendFailMessage(telegramBot)
            }
            logService.save(logEntity)
        }
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    suspend fun douYuPush() {
        val entityList = douYuService.findByPush(Status.ON)
        for (douYuEntity in entityList) {
            val tgId = douYuEntity.tgId
            val list = douYuLogic.focusFishGroup(douYuEntity)
            val newList = mutableListOf<DouYuFish>()
            if (douYuPushMap.containsKey(tgId)) {
                val oldId = douYuPushMap[tgId]!!
                for (biliBiliPojo in list) {
                    if (biliBiliPojo.id <= oldId) break
                    newList.add(biliBiliPojo)
                }
                for (douYuFish in newList) {
                    val text = "#斗鱼鱼吧动态推送\n#${douYuFish.nickname}\n内容：${douYuFish.ownerContent}\n标题：${douYuFish.title}\n内容：${douYuFish.content}"
                    if (douYuFish.image.isNotEmpty()) {
                        telegramBot.sendPic(tgId, text, douYuFish.image)
                    } else {
                        telegramBot.silent().send(text, tgId)
                    }
                }
            }
            douYuPushMap[tgId] = list[0].id
        }
    }

}
