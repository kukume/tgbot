package me.kuku.telegram.scheduled

import com.pengrad.telegrambot.TelegramBot
import me.kuku.telegram.entity.EssenceService
import me.kuku.telegram.logic.QqLogic
import me.kuku.telegram.utils.sendPic
import me.kuku.utils.DateTimeFormatterUtils
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class QqScheduled(
    private val essenceService: EssenceService,
    private val qqLogic: QqLogic,
    private val telegramBot: TelegramBot
) {


    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    suspend fun syncEssence() {
        val queryList = essenceService.findAll()
        for (essenceEntity in queryList) {
            val messages = essenceEntity.messages
            val group = essenceEntity.group
            val essenceList = qqLogic.groupEssence(null, group, size = 50).reversed()
            for (essenceMessage in essenceList) {
                val findMessage = messages.find {
                    it.msgRandom == essenceMessage.msgRandom && it.msgSeq == essenceMessage.msgSeq
                            && it.group == essenceMessage.group
                }
                if (findMessage == null) {
                    val text = essenceMessage.text()
                    val picUrlList = essenceMessage.msgContent.filter { it.msgType == 3 }.map { it.imageUrl }
                    telegramBot.sendPic(essenceEntity.chatId, """
                            #qq群精华消息同步
                            群号：$group
                            发送人qq：${essenceMessage.senderUin}
                            发送人昵称：${essenceMessage.senderNick}
                            发送时间：${DateTimeFormatterUtils.format((essenceMessage.senderTime.toString() + "000").toLong(), "yyyy-MM-dd HH:mm:ss")}
                            内容：
                            $text
                    """.trimIndent(), picUrlList, essenceEntity.messageThreadId)
                    messages.add(essenceMessage)
                }
            }
            essenceService.save(essenceEntity)
        }
    }

}
