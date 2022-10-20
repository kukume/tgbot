package me.kuku.telegram.extension

import me.kuku.telegram.entity.LogService
import me.kuku.telegram.utils.ability
import me.kuku.telegram.utils.callbackStartWith
import me.kuku.telegram.utils.execute
import me.kuku.telegram.utils.inlineKeyboardButton
import me.kuku.utils.DateTimeFormatterUtils
import org.springframework.stereotype.Component
import org.telegram.abilitybots.api.util.AbilityExtension
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class LogExtension(
    private val logService: LogService
): AbilityExtension {

    private suspend fun replyMarkup(before: LocalDateTime, after: LocalDateTime): InlineKeyboardMarkup {
        val logList = logService.findByCreateTimeBetween(before, after)
        val list = mutableListOf<List<InlineKeyboardButton>>()
        for (logEntity in logList) {
            val single = listOf(InlineKeyboardButton("${logEntity.type.value} - ${logEntity.text}").apply { callbackData = "logNone" })
            list.add(single)
        }
        if (list.isEmpty())
            list.add(listOf(InlineKeyboardButton("无").apply { callbackData = "logNone" }))
        val up = before.minusDays(1).toLocalDate()
        val upStr = DateTimeFormatterUtils.format(up, "yyyy-MM-dd")
        val down = before.plusDays(1).toLocalDate()
        val downStr = DateTimeFormatterUtils.format(down, "yyyy-MM-dd")
        val deepButton = listOf(inlineKeyboardButton("上一天", "log-$upStr"), inlineKeyboardButton("下一天", "log-$downStr"))
        list.add(deepButton)
        return InlineKeyboardMarkup(list)
    }


    fun logShow() = ability("log", "查看自动签到日志") {
        val sendMessage = SendMessage()
        sendMessage.chatId = chatId().toString()
        val before = LocalDate.now().atTime(0, 0)
        sendMessage.replyMarkup = replyMarkup(before, before.plusDays(1))
        sendMessage.text = "${DateTimeFormatterUtils.format(before, "yyyy-MM-dd")}的自动签到日志"
        execute(sendMessage)
    }

    fun logSwitch() = callbackStartWith("log-") {
        val data = it.data.substring(4)
        val before = DateTimeFormatterUtils.parseToLocalDate(data, "yyyy-MM-dd")
        val editMessageText = EditMessageText()
        editMessageText.chatId = it.message.chatId.toString()
        editMessageText.text = "${before}的自动签到日志"
        editMessageText.messageId = it.message.messageId
        val beforeTime = before.atTime(0, 0)
        editMessageText.replyMarkup = replyMarkup(beforeTime, beforeTime.plusDays(1))
        execute(editMessageText)
    }


}