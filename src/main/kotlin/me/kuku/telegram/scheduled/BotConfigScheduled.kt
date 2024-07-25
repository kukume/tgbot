package me.kuku.telegram.scheduled

import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.request.SendMessage
import me.kuku.telegram.config.Cron
import me.kuku.telegram.config.TelegramConfig
import me.kuku.telegram.config.telegramBot
import me.kuku.telegram.context.asyncExecute
import me.kuku.telegram.entity.BotConfigService
import me.kuku.telegram.entity.Status
import me.kuku.telegram.utils.githubCommit
import java.time.LocalDateTime

@Cron("00:00:00")
suspend fun commitPush() {
    if (TelegramConfig.creatorId == 0L) return
    val entity = BotConfigService.init()
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
            val sendMessage = SendMessage(TelegramConfig.creatorId, """
                    #github提交推送
                    昨日共有${commitList.size}次提交
                """.trimIndent()).replyMarkup(InlineKeyboardMarkup(*list.toTypedArray()))
            telegramBot.asyncExecute(sendMessage)
        }
    }
}