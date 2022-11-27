@file:Suppress("SpellCheckingInspection")

package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.config.TelegramBot
import me.kuku.telegram.entity.PixivService
import me.kuku.telegram.entity.Status
import me.kuku.telegram.logic.PixivLogic
import me.kuku.telegram.logic.PixivPojo
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.media.InputMedia
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto
import java.io.InputStream
import java.util.concurrent.TimeUnit

@Component
class PixivScheduled(
    private val pixivService: PixivService,
    private val telegramBot: TelegramBot
) {

    private val userMap = mutableMapOf<Long, Long>()

    @Scheduled(fixedDelay = 2, timeUnit = TimeUnit.MINUTES)
    suspend fun push() {
        val entityList = pixivService.findByPush(Status.ON)
        for (pixivEntity in entityList) {
            val tgId = pixivEntity.tgId
            val list = PixivLogic.followImage(pixivEntity)
            delay(3000)
            val newList = mutableListOf<PixivPojo>()
            if (userMap.containsKey(tgId)) {
                val oldId = userMap[tgId]!!
                for (pixivPojo in list) {
                    if (pixivPojo.id <= oldId) break
                    newList.add(pixivPojo)
                }
                for (pixivPojo in newList) {
                    val text = "#pixiv推送\n${PixivLogic.convertStr(pixivPojo)}"
                    val imageList = kotlin.runCatching {
                        PixivLogic.imageById(pixivPojo.id)
                    }.getOrNull() ?: continue
                    val sendList = mutableListOf<MutableList<String>>()
                    for (i in imageList.indices) {
                        val uu = imageList[i]
                        if ((i + 1) % 10 == 1) sendList.add(mutableListOf())
                        sendList.last().add(uu)
                    }
                    for (innerList in sendList) {
                        if (innerList.size == 1) {
                            val url = imageList[0]
                            val name = url.substring(url.lastIndexOf('/') + 1)
                            PixivLogic.imageIs(url).use {
                                val sendPhoto = SendPhoto(tgId.toString(), InputFile(it, "$name.jpg"))
                                sendPhoto.caption = text
                                telegramBot.execute(sendPhoto)
                            }
                        } else {
                            val ii = mutableListOf<InputStream>()
                            val inputMediaList = mutableListOf<InputMedia>()
                            try {
                                for (imageUrl in innerList) {
                                    val iis = PixivLogic.imageIs(imageUrl)
                                    val name = imageUrl.substring(imageUrl.lastIndexOf('/') + 1)
                                    ii.add(iis)
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
                    }

                }
            }
            userMap[tgId] = list[0].id
        }
    }

}