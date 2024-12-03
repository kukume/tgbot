package me.kuku.telegram.scheduled

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.request.SendMessage
import io.ktor.util.logging.*
import me.kuku.telegram.context.asyncExecute
import me.kuku.telegram.entity.LeiShenService
import me.kuku.telegram.entity.Status
import me.kuku.telegram.logic.LeiShenLogic
import me.kuku.telegram.context.inlineKeyboardButton
import me.kuku.telegram.context.sendTextMessage
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class LeiShenScheduled(
    private val leiShenService: LeiShenService,
    private val telegramBot: TelegramBot
) {

    private val logger = LoggerFactory.getLogger(LeiShenScheduled::class.java)


    @Scheduled(fixedDelay = 2, timeUnit = TimeUnit.HOURS)
    suspend fun leiShenRemind() {
        val entities = leiShenService.findByStatus(Status.ON)
        for (entity in entities) {
            kotlin.runCatching {
                val expiryTime = entity.expiryTime
                try {
                    if (System.currentTimeMillis() > expiryTime) {
                        val newEntity = LeiShenLogic.login(entity.username, entity.password)
                        entity.accountToken = newEntity.accountToken
                        entity.nnToken = newEntity.nnToken
                        entity.expiryTime = newEntity.expiryTime
                        leiShenService.save(entity)
                    }
                } catch (e: Exception) {
                    entity.status = Status.OFF
                    leiShenService.save(entity)
                    telegramBot.sendTextMessage(entity.tgId, """
                    #雷神加速器登录失败提醒
                    您的雷神加速器cookie已失效，重新登录失败，原因：${e.message}
                """.trimIndent())
                    return@runCatching
                }
                val userInfo = try {
                    LeiShenLogic.userInfo(entity)
                } catch (e: Exception) {
                    entity.expiryTime = 0
                    leiShenService.save(entity)
                    return@runCatching
                }
                if (userInfo.pauseStatusId == 0) {
                    val sendMessage = SendMessage(entity.tgId, """
                    #雷神加速器未暂停时间提醒 2小时提醒一次
                    您的雷神加速器未暂停时间，如果您未在玩游戏，请尽快暂停
                """.trimIndent()).replyMarkup(InlineKeyboardMarkup(inlineKeyboardButton("暂停时间", "leiShenPause")))
                    telegramBot.asyncExecute(sendMessage)
                }
            }.onFailure {
                logger.error(it)
            }
        }
    }



}
