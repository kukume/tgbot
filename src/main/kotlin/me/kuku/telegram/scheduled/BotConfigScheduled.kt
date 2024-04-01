package me.kuku.telegram.scheduled

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.request.SendMessage
import me.kuku.telegram.config.TelegramConfig
import me.kuku.telegram.context.asyncExecute
import me.kuku.telegram.entity.BotConfigService
import me.kuku.telegram.entity.Status
import me.kuku.telegram.utils.githubCommit
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class BotConfigScheduled(
    private val botConfigService: BotConfigService,
    private val telegramConfig: TelegramConfig,
    private val telegramBot: TelegramBot
) {

    @Scheduled(cron = "0 0 0 * * ?")
    suspend fun commitPush() {
        if (telegramConfig.creatorId == 0L) return
        val entity = botConfigService.init()
        if (entity.updatePush == Status.ON) {
            val yesterday = LocalDateTime.now().minusDays(1).toLocalDate()
            val commitList = githubCommit().filter {
                val localDateTime = it.localDateTime
                val head = yesterday.atStartOfDay()
                val tail = yesterday.atTime(23, 59, 59)
                localDateTime.isAfter(head) && localDateTime.isBefore(tail)
            }
            val list = mutableListOf<Array<InlineKeyboardButton>>()
            for (githubCommit in commitList) {
                list.add(arrayOf(InlineKeyboardButton("${githubCommit.date} - ${githubCommit.message}").callbackData("none")))
            }
            if (list.isNotEmpty()) {
                val sendMessage = SendMessage(telegramConfig.creatorId, """
                    #github提交推送
                    昨日共有${commitList.size}次提交
                """.trimIndent()).replyMarkup(InlineKeyboardMarkup(*list.toTypedArray()))
                telegramBot.asyncExecute(sendMessage)
            }
        }
    }



}