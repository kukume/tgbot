package me.kuku.telegram.scheduled

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.request.SendPhoto
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import me.kuku.telegram.context.asyncExecute
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.DouYuFish
import me.kuku.telegram.logic.DouYuLogic
import me.kuku.telegram.context.sendPic
import me.kuku.telegram.context.sendTextMessage
import me.kuku.utils.JobManager
import me.kuku.utils.client
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

    private val douYuTitleMap = mutableMapOf<Long, MutableMap<Long, String>>()

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
                        val text = "#斗鱼开播提醒\n#${room.nickName} $msg\n标题：${room.name}\n分类：${room.gameName}\n在线：${room.online}\n链接：${room.url}"
                        val imageUrl = room.imageUrl
                        if (imageUrl.isNotEmpty()) {
                            client.get(imageUrl).body<ByteArray>().let {
                                val sendPhoto = SendPhoto(tgId, it)
                                    .caption(text).fileName("douYuRoom.jpg")
                                telegramBot.asyncExecute(sendPhoto)
                            }
                        } else telegramBot.asyncExecute(SendMessage(tgId, text))
                    }
                } else map[id] = b
            }
        }
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    suspend fun titleChange() {
        val list = douYuService.findByTitleChange(Status.ON)
        for (douYuEntity in list) {
            val baseResult = douYuLogic.room(douYuEntity)
            delay(3000)
            if (baseResult.failure()) continue
            val tgId = douYuEntity.tgId
            if (!douYuTitleMap.containsKey(tgId)) douYuTitleMap[tgId] = mutableMapOf()
            val map = douYuTitleMap[tgId]!!
            for (room in baseResult.data()) {
                val name = room.name
                val roomId = room.roomId
                val value = map[roomId]
                if (value != null && value != name) {
                    val text = "#斗鱼标题更新提醒\n${room.nickName}\n旧标题：${value}\n新标题：${name}\n链接：${room.url}"
                    val imageUrl = room.imageUrl
                    if (imageUrl.isNotEmpty()) {
                        client.get(imageUrl).body<ByteArray>().let {
                            val sendPhoto = SendPhoto(tgId.toString(), it).fileName("douYuRoom.jpg")
                                .caption(text)
                            telegramBot.asyncExecute(sendPhoto)
                        }
                    } else telegramBot.sendTextMessage(tgId, text)
                }
                map[roomId] = name
            }
        }
    }

    @Scheduled(cron = "0 3 6 * * ?")
    suspend fun douYuSign() {
        val list = douYuService.findByFishGroup(Status.ON)
        for (douYuEntity in list) {
            logService.log(douYuEntity, LogType.DouYu) {
                delay(3000)
                val cookie = douYuEntity.cookie
                if (cookie.isNotEmpty()) douYuLogic.fishGroup(douYuEntity)
            }
        }
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    suspend fun douYuPush() {
        val entityList = douYuService.findByPush(Status.ON)
        for (douYuEntity in entityList) {
            val tgId = douYuEntity.tgId
            val list = try {
                douYuLogic.focusFishGroup(douYuEntity)
            } catch (e: Exception) {continue}
            val newList = mutableListOf<DouYuFish>()
            if (douYuPushMap.containsKey(tgId)) {
                val oldId = douYuPushMap[tgId]!!
                for (biliBiliPojo in list) {
                    if (biliBiliPojo.id <= oldId) break
                    newList.add(biliBiliPojo)
                }
                for (douYuFish in newList) {
                    val text = "#斗鱼鱼吧动态推送\n#${douYuFish.nickname}\n标题：${douYuFish.title}\n内容：${douYuFish.content}\n链接：${douYuFish.url}"
                    if (douYuFish.image.isNotEmpty()) {
                        JobManager.delay(1000 * 60) {
                            telegramBot.sendPic(tgId, text, douYuFish.image)
                        }
                    } else {
                        telegramBot.asyncExecute(SendMessage(tgId, text))
                    }
                }
            }
            douYuPushMap[tgId] = list[0].id
        }
    }

}
