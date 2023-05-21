package me.kuku.telegram.extension

import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.request.DeleteMessage
import kotlinx.coroutines.delay
import me.kuku.telegram.entity.EssenceEntity
import me.kuku.telegram.entity.EssenceService
import me.kuku.telegram.logic.QqLogic
import me.kuku.telegram.utils.*
import me.kuku.utils.DateTimeFormatterUtils
import org.springframework.stereotype.Component

@Component
class QqExtension(
    private val qqLogic: QqLogic,
    private val essenceService: EssenceService
) {

    private suspend fun essence(tgId: Long, group: Long, page: Int = 0, size: Int = 10): InlineKeyboardMarkup {
        val groupEssenceList = qqLogic.groupEssence(tgId, group, page, size)
        if (groupEssenceList.isEmpty()) errorAnswerCallbackQuery("没有群精华消息了")
        val list = mutableListOf<Array<InlineKeyboardButton>>()
        for (essenceMessage in groupEssenceList) {
            list.add(arrayOf(inlineKeyboardButton("${essenceMessage.senderNick}：${essenceMessage.str()}",
                "sendQqGroupEssenceMessage-${essenceMessage.group}-$page-$size-${essenceMessage.msgSeq}-${essenceMessage.msgRandom}")))
        }
        list.add(arrayOf(inlineKeyboardButton("上一页", "queryQqGroupEssenceMessage|${page - 1}|$group"),
            inlineKeyboardButton("下一页", "queryQqGroupEssenceMessage|${page + 1}|$group")))
        return InlineKeyboardMarkup(*list.toTypedArray())
    }

    fun AbilitySubscriber.groupEssence() {
        sub("essence", locality = Locality.ALL) {
            kotlin.runCatching {
                val inlineKeyboardMarkup = essence(tgId,
                    firstArg().toLongOrNull() ?: error("错误的群号"), 0)
                val sendResponse =
                    sendMessage("当前页数为0\n请选择精华消息以同步到本会话，2分钟后本消息以及指令消息删除，无论您是否操作", inlineKeyboardMarkup)
                val messageId = sendResponse.message().messageId()
                delay(1000 * 60 * 2)
                val deleteMessage = DeleteMessage(chatId, messageId)
                bot.execute(deleteMessage)
                message.delete()
            }.onFailure {
                sendMessage("""
                    /addEssence {group} - 添加群精华监控
                    /deleteEssence {group} - 删除群精华监控
                """.trimIndent())
            }
        }
        sub("addEssence", input = 1, locality = Locality.ALL) {
            val group = firstArg().toLongOrNull() ?: error("群号不符合规范")
            val tempEntity =
                essenceService.findByChatIdAndMessageThreadIdAndGroup(chatId, message.messageThreadId(), group)
            if (tempEntity != null) error("已添加该群，请勿重复添加")
            val essenceEntity = EssenceEntity()
            essenceEntity.chatId = chatId
            essenceEntity.group = group
            essenceEntity.messageThreadId = message.messageThreadId()
            essenceService.save(essenceEntity)
            sendMessage("添加群${group}精华消息自动转发成功")
        }
        sub("deleteEssence", input = 1, locality = Locality.ALL) {
            val group = firstArg().toLongOrNull() ?: error("群号不符合规范")
            essenceService.deleteByChatIdAndMessageThreadIdAndGroup(chatId, message.messageThreadId(), group)
            sendMessage("删除该群精华消息监控成功")
        }
    }

    fun TelegramSubscribe.groupEssence() {
        callbackStartsWith("queryQqGroupEssenceMessage|") {
            val arr = query.data().split("|")
            val page = arr[1].toInt()
            val group = arr[2].toLong()
            if (page < 0) errorAnswerCallbackQuery("你家的页数可以小于0？")
            val inlineKeyboardMarkup = essence(tgId, group, page)
            editMessageText("当前页数为$page\n请选择精华消息以同步到本会话，2分钟后本消息以及指令消息删除，无论您是否操作", inlineKeyboardMarkup)
        }
        callbackStartsWith("sendQqGroupEssenceMessage-") {
            val arr = query.data().split("-")
            val group = arr[1].toLong()
            val page = arr[2].toInt()
            val size = arr[3].toInt()
            val msgSeq = arr[4].toInt()
            val msgRandom = arr[5].toLong()
            val mes = qqLogic.groupEssence(tgId, group, page, size).find {
                it.msgSeq == msgSeq && it.msgRandom == msgRandom
            } ?: error("没有找到指定的群精华消息，请重新发送")
            val text = mes.text()
            val picUrlList = mes.msgContent.filter { it.msgType == 3 }.map { it.imageUrl }
            answerCallbackQuery("获取成功")
            bot.sendPic(chatId, """
                #qq群精华消息
                群号：$group
                发送人qq：${mes.senderUin}
                发送人昵称：${mes.senderNick}
                发送时间：${DateTimeFormatterUtils.format((mes.senderTime.toString() + "000").toLong(), "yyyy-MM-dd HH:mm:ss")}
                内容：
                $text
            """.trimIndent(), picUrlList, message.messageThreadId())
        }
    }

}
