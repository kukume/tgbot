package me.kuku.telegram.extension

import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import me.kuku.telegram.entity.LogService
import me.kuku.telegram.utils.*
import me.kuku.utils.DateTimeFormatterUtils
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class LogExtension(
    private val logService: LogService
) {

    private suspend fun replyMarkup(before: LocalDateTime, after: LocalDateTime, tgId: Long): InlineKeyboardMarkup {
        val logList = logService.findByCreateTimeBetweenAndTgId(before, after, tgId)
        val list = mutableListOf<Array<InlineKeyboardButton>>()
        for (logEntity in logList) {
            val single = arrayOf(InlineKeyboardButton("${logEntity.type.value} - ${logEntity.text}").callbackData("logNone"))
            list.add(single)
        }
        if (list.isEmpty())
            list.add(arrayOf(InlineKeyboardButton("无").callbackData("logNone")))
        val up = before.minusDays(1).toLocalDate()
        val upStr = DateTimeFormatterUtils.format(up, "yyyy-MM-dd")
        val down = before.plusDays(1).toLocalDate()
        val downStr = DateTimeFormatterUtils.format(down, "yyyy-MM-dd")
        val deepButton = arrayOf(inlineKeyboardButton("上一天", "log-$upStr"), inlineKeyboardButton("下一天", "log-$downStr"))
        list.add(deepButton)
        return InlineKeyboardMarkup(*list.toTypedArray())
    }


    fun AbilitySubscriber.logShow() {
        sub("log") {
            val before = LocalDate.now().atTime(0, 0)
            sendMessage("${DateTimeFormatterUtils.format(before, "yyyy-MM-dd")}的自动签到日志",
                replyMarkup(before, before.plusDays(1), tgId))
        }
    }

    fun TelegramSubscribe.logSwitch() {
        callbackStartsWith("log-") {
            val data = query.data().substring(4)
            val before = DateTimeFormatterUtils.parseToLocalDate(data, "yyyy-MM-dd")
            val beforeTime = before.atTime(0, 0)
            editMessageText("${before}的自动签到日志", replyMarkup(beforeTime, beforeTime.plusDays(1), tgId),
                returnButton = false)
        }
        callback("logNone") {
            answerCallbackQuery("这是给你看的，不是给你点的")
        }
    }


}
