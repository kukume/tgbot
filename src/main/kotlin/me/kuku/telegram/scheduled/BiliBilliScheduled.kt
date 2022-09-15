package me.kuku.telegram.scheduled

import me.kuku.telegram.config.TelegramBot
import me.kuku.telegram.config.TelegramConfig
import me.kuku.telegram.entity.BiliBiliService
import me.kuku.telegram.entity.Status
import me.kuku.telegram.logic.BiliBiliLogic
import me.kuku.telegram.logic.BiliBiliPojo
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendVideo
import org.telegram.telegrambots.meta.api.objects.InputFile
import java.util.concurrent.TimeUnit

@Component
class BiliBilliScheduled(
    private val biliBiliService: BiliBiliService,
    private val telegramBot: TelegramBot,
    private val telegramConfig: TelegramConfig
) {

    private val liveMap = mutableMapOf<Long, MutableMap<Long, Boolean>>()
    private val userMap = mutableMapOf<Long, Long>()


    @Scheduled(cron = "0 23 3 * * ?")
    suspend fun sign() {
        val list = biliBiliService.findBySign(Status.ON)
        for (biliBiliEntity in list) {
            val firstRank = BiliBiliLogic.ranking()[0]
            BiliBiliLogic.report(biliBiliEntity, firstRank.aid, firstRank.cid, 300)
            BiliBiliLogic.share(biliBiliEntity, firstRank.aid)
            BiliBiliLogic.liveSign(biliBiliEntity)
        }
    }

    @Scheduled(fixedDelay = 2, timeUnit = TimeUnit.MINUTES)
    suspend fun liveMonitor() {
        val list = biliBiliService.findByLive(Status.ON)
        for (biliBiliEntity in list) {
            val result = BiliBiliLogic.followed(biliBiliEntity)
            if (result.failure()) continue
            val tgId = biliBiliEntity.tgId
            if (!liveMap.containsKey(tgId)) liveMap[tgId] = mutableMapOf()
            val map = liveMap[tgId]!!
            for (up in result.data()) {
                val id = up.id.toLong()
                val name = up.name
                val live = BiliBiliLogic.live(id.toString())
                if (live.id.isEmpty()) continue
                val b = live.status
                if (map.containsKey(id)) {
                    if (map[id] != b) {
                        map[id] = b
                        val msg = if (b) "直播啦！！" else "下播了！！"
                        telegramBot.silent().send("""
                            #哔哩哔哩开播提醒
                            哔哩哔哩开播提醒：
                            $name$msg
                            标题：${live.title}
                            链接：${live.url}
                        """.trimIndent(), tgId)
                    }
                } else map[id] = b
            }
        }
    }


    @Scheduled(fixedDelay = 2, timeUnit = TimeUnit.MINUTES)
    suspend fun userMonitor() {
        val biliBiliList = biliBiliService.findByPush(Status.ON)
        for (biliBiliEntity in biliBiliList) {
            val tgId = biliBiliEntity.tgId
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
                    val text = "#哔哩哔哩动态推送\n哔哩哔哩有新动态了！！\n${BiliBiliLogic.convertStr(biliBiliPojo)}"
                    if (biliBiliPojo.bvId.isNotEmpty() && telegramConfig.url.isNotEmpty()) {
                        val file = BiliBiliLogic.videoByBvId(biliBiliEntity, biliBiliPojo.bvId)
                        file.inputStream().use { iis ->
                                val sendVideo = SendVideo(tgId.toString(), InputFile(iis, "${biliBiliPojo.bvId}.mp4"))
                                sendVideo.caption = text
                                kotlin.runCatching {
                                    telegramBot.execute(sendVideo)
                                }.onFailure {
                                    telegramBot.silent().send("视频发送失败，转为文字发送\n$text", tgId)
                                }
                        }
                        file.delete()
                    } else telegramBot.silent().send(text, tgId)
                }
            }
            userMap[tgId] = list[0].id.toLong()
        }
    }

}