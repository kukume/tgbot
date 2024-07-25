package me.kuku.telegram.scheduled

import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.request.SendPhoto
import com.pengrad.telegrambot.request.SendVideo
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import me.kuku.telegram.config.Cron
import me.kuku.telegram.config.TelegramConfig
import me.kuku.telegram.config.telegramBot
import me.kuku.telegram.context.asyncExecute
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.BiliBiliLogic
import me.kuku.telegram.logic.BiliBiliPojo
import me.kuku.telegram.context.sendPic
import me.kuku.utils.client
import java.io.File

private val liveMap = mutableMapOf<Long, MutableMap<Long, Boolean>>()
private val userMap = mutableMapOf<Long, Long>()


@Cron("03:23:00")
suspend fun biliBiliSign() {
    val list = BiliBiliService.findBySign(Status.ON)
    for (biliBiliEntity in list) {
        LogService.log(biliBiliEntity, LogType.BiliBili) {
            val firstRank = BiliBiliLogic.ranking(biliBiliEntity)[0]
            delay(5000)
            BiliBiliLogic.watchVideo(biliBiliEntity, firstRank)
            delay(5000)
            BiliBiliLogic.share(biliBiliEntity, firstRank.aid)
            delay(5000)
            BiliBiliLogic.liveSign(biliBiliEntity)
        }
        delay(3000)
    }
}

@Cron("2m")
suspend fun biliBiliLiveMonitor() {
    val list = BiliBiliService.findByLive(Status.ON)
    for (biliBiliEntity in list) {
        delay(3000)
        val tgId = biliBiliEntity.tgId
        if (!liveMap.containsKey(tgId)) liveMap[tgId] = mutableMapOf()
        val map = liveMap[tgId]!!
        val liveList = BiliBiliLogic.live(biliBiliEntity)
        for (live in liveList) {
            val userid = live.id.toLong()
            val b = live.status
            val name = live.uname
            if (map.containsKey(userid)) {
                if (map[userid] != b) {
                    map[userid] = b
                    val msg = if (b) "直播啦！！" else "下播了！！"
                    val text = "#哔哩哔哩开播提醒\n#$name $msg\n标题：${live.title}\n链接：${live.url}"
                    val imageUrl = live.imageUrl
                    if (imageUrl.isEmpty())
                        telegramBot.asyncExecute(SendMessage(tgId, text))
                    else {
                        client.get(imageUrl).body<ByteArray>().let {
                            val sendPhoto = SendPhoto(tgId, it).caption(text).fileName("live.jpg")
                            telegramBot.asyncExecute(sendPhoto)
                        }
                    }
                }
            } else map[userid] = live.status
        }
    }
}

@Cron("2m")
suspend fun biliBiliUserMonitor() {
    val biliBiliList = BiliBiliService.findByPush(Status.ON)
    for (biliBiliEntity in biliBiliList) {
        val tgId = biliBiliEntity.tgId
        delay(3000)
        val result = BiliBiliLogic.friendDynamic(biliBiliEntity)
        val list = result.data ?: continue
        val newList = mutableListOf<BiliBiliPojo>()
        if (userMap.containsKey(tgId)) {
            val oldId = userMap[tgId]!!
            for (biliBiliPojo in list) {
                if (biliBiliPojo.id.toLong() <= oldId) break
                newList.add(biliBiliPojo)
            }
            for (biliBiliPojo in newList) {
                val text = "#哔哩哔哩动态推送\n${BiliBiliLogic.convertStr(biliBiliPojo)}"
                val bvId = if (biliBiliPojo.bvId.isNotEmpty()) biliBiliPojo.bvId
                else if (biliBiliPojo.forwardBvId.isNotEmpty()) biliBiliPojo.forwardBvId
                else ""
                try {
                    if (bvId.isNotEmpty() && TelegramConfig.url.isNotEmpty()) {
                        var file: File? = null
                        try {
                            delay(3000)
                            file = BiliBiliLogic.videoByBvId(biliBiliEntity, biliBiliPojo.bvId)
                            val sendVideo =
                                SendVideo(tgId, file).caption(text)
                            telegramBot.asyncExecute(sendVideo)
                        } finally {
                            file?.delete()
                        }
                    } else if (biliBiliPojo.picList.isNotEmpty() || biliBiliPojo.forwardPicList.isNotEmpty()) {
                        val picList = biliBiliPojo.picList
                        picList.addAll(biliBiliPojo.forwardPicList)
                        telegramBot.sendPic(tgId, text, picList)
                    } else telegramBot.asyncExecute(SendMessage(tgId, text))
                } catch (e: Exception) {
                    telegramBot.asyncExecute(SendMessage(tgId, text))
                }
            }
        }
        userMap[tgId] = list[0].id.toLong()
    }
}