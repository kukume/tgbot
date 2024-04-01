@file:Suppress("SpellCheckingInspection")

package me.kuku.telegram.scheduled

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.InputMediaPhoto
import com.pengrad.telegrambot.request.SendMediaGroup
import com.pengrad.telegrambot.request.SendPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.kuku.telegram.context.asyncExecute
import me.kuku.telegram.entity.PixivService
import me.kuku.telegram.entity.Status
import me.kuku.telegram.logic.PixivLogic
import me.kuku.telegram.logic.PixivPojo
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
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
                            withContext(Dispatchers.IO) {
                                PixivLogic.imageIs(url).readAllBytes()
                            }.let {
                                val sendPhoto = SendPhoto(tgId, it).fileName("$name.jpg")
                                    .caption(text)
                                telegramBot.asyncExecute(sendPhoto)
                            }
                        } else {
                            val inputMediaList = mutableListOf<InputMediaPhoto>()
                            for (imageUrl in innerList) {
                                val iis = PixivLogic.imageIs(imageUrl)
                                val name = imageUrl.substring(imageUrl.lastIndexOf('/') + 1)
                                val mediaPhoto = InputMediaPhoto(withContext(Dispatchers.IO) {
                                    iis.readAllBytes()
                                })
                                    .fileName(name).caption(text)
                                inputMediaList.add(mediaPhoto)
                            }
                            val sendMediaGroup = SendMediaGroup(tgId, *inputMediaList.toTypedArray())
                            telegramBot.asyncExecute(sendMediaGroup)
                        }
                    }

                }
            }
            userMap[tgId] = list[0].id
        }
    }

}
