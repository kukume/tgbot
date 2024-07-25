package me.kuku.telegram.scheduled

import com.pengrad.telegrambot.request.SendPhoto
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import me.kuku.telegram.config.Cron
import me.kuku.telegram.config.telegramBot
import me.kuku.telegram.context.asyncExecute
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.HuYaLogic
import me.kuku.telegram.context.sendTextMessage
import me.kuku.utils.client

private val huYaLiveMap = mutableMapOf<Long, MutableMap<Long, Boolean>>()

@Cron("1m")
suspend fun huYa() {
    val list = HuYaService.findByLive(Status.ON)
    for (huYaEntity in list) {
        delay(3000)
        val baseResult = HuYaLogic.live(huYaEntity)
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
    }
}