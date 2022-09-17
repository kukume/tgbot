package me.kuku.telegram.scheduled

import me.kuku.telegram.config.TelegramBot
import me.kuku.telegram.entity.Status
import me.kuku.telegram.entity.WeiboService
import me.kuku.telegram.logic.WeiboLogic
import me.kuku.telegram.logic.WeiboPojo
import me.kuku.utils.OkHttpKtUtils
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.send.SendVideo
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.media.InputMedia
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto
import java.io.InputStream
import java.util.concurrent.TimeUnit

@Component
class WeiboScheduled(
    private val weiboService: WeiboService,
    private val telegramBot: TelegramBot
) {

    private val userMap = mutableMapOf<Long, Long>()

    @Scheduled(cron = "0 51 4 * * ?")
    suspend fun sign() {
        val list = weiboService.findBySign(Status.ON)
        for (weiboEntity in list) {
            WeiboLogic.superTalkSign(weiboEntity)
        }
    }

    @Scheduled(fixedDelay = 2, timeUnit = TimeUnit.MINUTES)
    suspend fun userMonitor() {
        val weiboList = weiboService.findByPush(Status.ON)
        for (weiboEntity in weiboList) {
            val tgId = weiboEntity.tgId
            val result = WeiboLogic.friendWeibo(weiboEntity)
            val list = result.data ?: continue
            val newList = mutableListOf<WeiboPojo>()
            if (userMap.containsKey(tgId)) {
                for (weiboPojo in list) {
                    if (weiboPojo.id <= userMap[tgId]!!) break
                    newList.add(weiboPojo)
                }
                for (weiboPojo in newList) {
                    val text = "#微博动态推送\n有新微博了！！\n${WeiboLogic.convert(weiboPojo)}"
                    val videoUrl = if (weiboPojo.videoUrl.isNotEmpty()) weiboPojo.videoUrl
                    else if (weiboPojo.forwardVideoUrl.isNotEmpty()) weiboPojo.forwardVideoUrl
                    else ""
                    if (videoUrl.isNotEmpty()) {
                        OkHttpKtUtils.getByteStream(videoUrl).use {
                            val sendVideo = SendVideo(tgId.toString(), InputFile(it, "${weiboPojo.bid}.mp4"))
                            sendVideo.caption = text
                            telegramBot.execute(sendVideo)
                        }
                    } else if (weiboPojo.imageUrl.isNotEmpty() || weiboPojo.forwardImageUrl.isNotEmpty()) {
                        val imageList = weiboPojo.imageUrl
                        imageList.addAll(weiboPojo.forwardImageUrl)
                        if (imageList.size == 1) {
                            OkHttpKtUtils.getByteStream(imageList[0]).use {
                                val sendPhoto = SendPhoto(tgId.toString(), InputFile(it, "${weiboPojo.bid}.jpg"))
                                sendPhoto.caption = text
                                telegramBot.execute(sendPhoto)
                            }
                        } else {
                            val ii = mutableListOf<InputStream>()
                            val inputMediaList = mutableListOf<InputMedia>()
                            try {
                                for (imageUrl in imageList) {
                                    val iis = OkHttpKtUtils.getByteStream(imageUrl)
                                    ii.add(iis)
                                    val name = imageUrl.substring(imageUrl.lastIndexOf('/') + 1)
                                    val mediaPhoto =
                                        InputMediaPhoto.builder().newMediaStream(iis).media("attach://$name").mediaName(name).isNewMedia(true).build()
                                    mediaPhoto.caption = text
                                    mediaPhoto.captionEntities
                                    inputMediaList.add(mediaPhoto)
                                }
                                val sendMediaGroup = SendMediaGroup(tgId.toString(), inputMediaList)
                                telegramBot.execute(sendMediaGroup)
                            } finally {
                                ii.forEach { it.close() }
                            }
                        }
                    } else telegramBot.silent().send(text, tgId)
                }
            }
            userMap[tgId] = list[0].id
        }
    }


}