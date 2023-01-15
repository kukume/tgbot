package me.kuku.telegram.scheduled

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import me.kuku.telegram.config.TelegramBot
import me.kuku.telegram.entity.DouYinService
import me.kuku.telegram.entity.Status
import me.kuku.telegram.logic.DouYinLogic
import me.kuku.telegram.logic.DouYinWork
import me.kuku.utils.MyUtils
import me.kuku.utils.client
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendVideo
import org.telegram.telegrambots.meta.api.objects.InputFile
import java.io.InputStream
import java.util.concurrent.TimeUnit

@Component
class DouYinScheduled(
    private val douYinService: DouYinService,
    private val telegramBot: TelegramBot
) {

    private val userMap = mutableMapOf<Long, Long>()

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    suspend fun push() {
        val douYinList = douYinService.findByPush(Status.ON)
        for (douYinEntity in douYinList) {
            delay(5000)
            val list = try {
                DouYinLogic.followWork(douYinEntity)
            } catch (e: Exception) {
                continue
            }
            if (list.isEmpty()) continue
            val tgId = douYinEntity.tgId
            val newList = mutableListOf<DouYinWork>()
            if (userMap.containsKey(tgId)) {
                val oldId = userMap[tgId]!!
                for (douYinWork in list) {
                    if (douYinWork.id <= oldId) break
                    newList.add(douYinWork)
                }
                for (douYinWork in newList) {
                    val url = douYinWork.videoUrlList.last()
                    client.get(url).body<InputStream>().use {
                        val sendVideo = SendVideo(tgId.toString(), InputFile(it, "${MyUtils.randomLetter(6)}.mp4"))
                        sendVideo.caption = "#抖音推送\n#${douYinWork.nickname}\n${douYinWork.desc}\n链接：${douYinWork.url}"
                        telegramBot.execute(sendVideo)
                    }
                }
            }
            userMap[tgId] = list[0].id
        }
    }


}
