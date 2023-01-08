package me.kuku.telegram.scheduled

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import me.kuku.telegram.config.TelegramBot
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.WeiboLogic
import me.kuku.telegram.logic.WeiboPojo
import me.kuku.utils.client
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendVideo
import org.telegram.telegrambots.meta.api.objects.InputFile
import java.io.InputStream
import java.util.concurrent.TimeUnit

@Component
class WeiboScheduled(
    private val weiboService: WeiboService,
    private val telegramBot: TelegramBot,
    private val logService: LogService
) {

    private val userMap = mutableMapOf<Long, Long>()

    @Scheduled(cron = "0 51 4 * * ?")
    suspend fun sign() {
        val list = weiboService.findBySign(Status.ON)
        for (weiboEntity in list) {
            val logEntity = LogEntity().also {
                it.type = LogType.Weibo
                it.tgId = weiboEntity.tgId
            }
            kotlin.runCatching {
                WeiboLogic.superTalkSign(weiboEntity)
                logEntity.text = "成功"
            }.onFailure {
                logEntity.text = "失败"
                logEntity.sendFailMessage(telegramBot)
            }
            logService.save(logEntity)
            delay(3000)
        }
    }

    @Scheduled(fixedDelay = 2, timeUnit = TimeUnit.MINUTES)
    suspend fun userMonitor() {
        val weiboList = weiboService.findByPush(Status.ON)
        for (weiboEntity in weiboList) {
            val tgId = weiboEntity.tgId
            delay(3000)
            val list = WeiboLogic.followWeibo(weiboEntity)
            val newList = mutableListOf<WeiboPojo>()
            if (userMap.containsKey(tgId)) {
                for (weiboPojo in list) {
                    if (weiboPojo.id <= userMap[tgId]!!) break
                    newList.add(weiboPojo)
                }
                for (weiboPojo in newList) {
                    val ownText = if (weiboPojo.longText) WeiboLogic.longText(weiboEntity, weiboPojo.bid) else weiboPojo.text
                    val forwardText = if (weiboPojo.forwardLongText) WeiboLogic.longText(weiboEntity, weiboPojo.forwardBid) else weiboPojo.forwardText
                    val text = "#微博动态推送\n${WeiboLogic.convert(weiboPojo, ownText, forwardText)}"
                    val videoUrl = if (weiboPojo.videoUrl.isNotEmpty()) weiboPojo.videoUrl
                    else if (weiboPojo.forwardVideoUrl.isNotEmpty()) weiboPojo.forwardVideoUrl
                    else ""
                    try {
                        if (videoUrl.isNotEmpty()) {
                            client.get(videoUrl).body<InputStream>().use {
                                val sendVideo = SendVideo(tgId.toString(), InputFile(it, "${weiboPojo.bid}.mp4"))
                                sendVideo.caption = text
                                telegramBot.execute(sendVideo)
                            }
                        } else if (weiboPojo.imageUrl.isNotEmpty() || weiboPojo.forwardImageUrl.isNotEmpty()) {
                            val imageList = weiboPojo.imageUrl
                            imageList.addAll(weiboPojo.forwardImageUrl)
                            telegramBot.sendPic(tgId, text, imageList)
                        } else telegramBot.silent().send(text, tgId)
                    } catch (e: Exception) {
                        telegramBot.silent().send(text, tgId)
                    }
                }
            }
            userMap[tgId] = list[0].id
        }
    }


}
