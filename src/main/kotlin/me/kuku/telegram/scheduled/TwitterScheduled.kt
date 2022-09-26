package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.config.TelegramBot
import me.kuku.telegram.entity.Status
import me.kuku.telegram.entity.TwitterService
import me.kuku.telegram.logic.TwitterLogic
import me.kuku.telegram.logic.TwitterPojo
import me.kuku.utils.MyUtils
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
import java.lang.Long.max
import java.util.concurrent.TimeUnit

@Component
class TwitterScheduled(
    private val twitterService: TwitterService,
    private val telegramBot: TelegramBot
) {

    private val userMap = mutableMapOf<Long, Long>()

    @Scheduled(fixedDelay = 2, timeUnit = TimeUnit.MINUTES)
    suspend fun push() {
        val entityList = twitterService.findByPush(Status.ON)
        for (entity in entityList) {
            val tgId = entity.tgId
            val list = TwitterLogic.friendTweet(entity).sortedBy { -it.id }
            delay(3000)
            if (list.isEmpty()) continue
            val newList = mutableListOf<TwitterPojo>()
            if (userMap.containsKey(tgId)) {
                val oldId = userMap[tgId]!!
                for (twitterPojo in list) {
                    if (twitterPojo.id <= oldId) break
                    newList.add(twitterPojo)
                }
                for (twitterPojo in newList) {
                    val text = "#Twitter新推推送\n${TwitterLogic.convertStr(twitterPojo)}"
                    val videoUrl = if (twitterPojo.videoList.isNotEmpty()) twitterPojo.videoList[0]
                    else if (twitterPojo.forwardVideoList.isNotEmpty()) twitterPojo.forwardVideoList[0]
                    else ""
                    try {
                        if (videoUrl.isNotEmpty()) {
                            OkHttpKtUtils.getByteStream(videoUrl).use {
                                val sendVideo = SendVideo(tgId.toString(), InputFile(it, "${twitterPojo.id}.mp4"))
                                sendVideo.caption = text
                                telegramBot.execute(sendVideo)
                            }
                        } else if (twitterPojo.photoList.isNotEmpty() || twitterPojo.forwardPhotoList.isNotEmpty()) {
                            val imageList = twitterPojo.photoList
                            imageList.addAll(twitterPojo.forwardPhotoList)
                            if (imageList.size == 1) {
                                OkHttpKtUtils.getByteStream(imageList[0]).use {
                                    val sendPhoto = SendPhoto(tgId.toString(), InputFile(it, "${twitterPojo.id}.jpg"))
                                    sendPhoto.caption = text
                                    telegramBot.execute(sendPhoto)
                                }
                            } else {
                                val ii = mutableListOf<InputStream>()
                                val inputMediaList = mutableListOf<InputMedia>()
                                try {
                                    for (i in imageList.indices) {
                                        if (i > 9) break
                                        val imageUrl = imageList[i]
                                        val iis = OkHttpKtUtils.getByteStream(imageUrl)
                                        ii.add(iis)
                                        val name = MyUtils.randomLetter(5) + ".jpg"
                                        val mediaPhoto =
                                            InputMediaPhoto.builder().isNewMedia(true).newMediaStream(iis).mediaName(name).media("attach://$name").build()
                                        mediaPhoto.caption = text
                                        inputMediaList.add(mediaPhoto)
                                    }
                                    val sendMediaGroup = SendMediaGroup(tgId.toString(), inputMediaList)
                                    telegramBot.execute(sendMediaGroup)
                                } finally {
                                    ii.forEach { it.close() }
                                }
                            }
                        } else telegramBot.silent().send(text, tgId)
                    } catch (e: Exception) {
                        telegramBot.silent().send(text, tgId)
                    }
                }
            }
            userMap[tgId] = max(list[0].id, userMap[tgId] ?: 0)
        }
    }

}