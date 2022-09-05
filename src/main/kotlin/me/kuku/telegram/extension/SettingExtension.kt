package me.kuku.telegram.extension

import me.kuku.telegram.config.TelegramBot
import me.kuku.telegram.utils.*
import org.springframework.stereotype.Component
import org.telegram.abilitybots.api.objects.Privacy
import org.telegram.abilitybots.api.util.AbilityExtension
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
class SettingExtension(
    private val telegramBot: TelegramBot
): AbilityExtension {

    fun settingMarkup(): InlineKeyboardMarkup {
        val blackSetting = inlineKeyboardButton("黑名单", "blackSetting")
        val adminSetting = inlineKeyboardButton("管理员", "adminSetting")
        return InlineKeyboardMarkup(listOf(
            listOf(blackSetting, adminSetting)
        ))
    }

    fun returnButton(): List<InlineKeyboardButton> {
        return listOf(inlineKeyboardButton("返回", "returnSetting"))
    }

    fun setting() = ability("setting", "机器人设置", privacy = Privacy.CREATOR) {
        val sendMessage = SendMessage()
        sendMessage.text = "请选择设置选项"
        sendMessage.replyMarkup = settingMarkup()
        sendMessage.chatId = chatId().toString()
        execute(sendMessage)
    }

    fun returnSetting() = callback("returnSetting") {
        val messageId = it.message.messageId
        val editMessageText = EditMessageText.builder().text("请选择设置选项").chatId(it.message.chatId).messageId(messageId)
            .replyMarkup(settingMarkup()).build()
        execute(editMessageText)
    }

    fun blackSetting() = callback {
        query("blackSetting") {
            val queryBlackButton = inlineKeyboardButton("查询", "queryBlack")
            val addBlackButton = inlineKeyboardButton("增加", "addBlack")
            val deleteBlackButton = inlineKeyboardButton("删除", "deleteBlack")
            val editMessageText = EditMessageText.builder().text("请选择黑名单操作：").chatId(it.message.chatId).messageId(it.message.messageId)
                .replyMarkup(
                    InlineKeyboardMarkup(
                        listOf(
                            listOf(queryBlackButton),
                            listOf(addBlackButton),
                            listOf(deleteBlackButton),
                            returnButton()
                        )
                    )
                ).build()
            execute(editMessageText)
        }

        query("queryBlack") {
            val ss = telegramBot.blacklist().joinToString("\n")
            val sendMessage = SendMessage.builder().text("黑名单列表如下：\n$ss").chatId(it.message.chatId).build()
            execute(sendMessage)
        }

        query("addBlack") {
            val chatId = it.message.chatId
            execute(SendMessage(chatId.toString(), "请发送需要新增的黑名单id"))
            val ss = it.waitNextMessage().text.toLongOrNull() ?: error("发送的不为数字")
            telegramBot.blacklist().add(ss)
            execute(SendMessage(chatId.toString(), "增加黑名单成功"))
        }

        query("deleteBlack") {
            val chatId = it.message.chatId
            execute(SendMessage(chatId.toString(), "请发送需要删除的黑名单id"))
            val ss = it.waitNextMessage().text.toLongOrNull() ?: error("发送的不为数字")
            telegramBot.blacklist().remove(ss)
            execute(SendMessage(chatId.toString(), "删除黑名单成功"))
        }

    }

    fun adminSetting() = callback {
        query("adminSetting") {
            val queryBlackButton = inlineKeyboardButton("查询", "queryAdmin")
            val addBlackButton = inlineKeyboardButton("增加", "addAdmin")
            val deleteBlackButton = inlineKeyboardButton("删除", "deleteAdmin")
            val editMessageText = EditMessageText.builder().text("请选择管理员操作：").chatId(it.message.chatId).messageId(it.message.messageId)
                .replyMarkup(
                    InlineKeyboardMarkup(
                        listOf(
                            listOf(queryBlackButton),
                            listOf(addBlackButton),
                            listOf(deleteBlackButton),
                            returnButton()
                        )
                    )
                ).build()
            execute(editMessageText)
        }

        query("queryAdmin") {
            val ss = telegramBot.admins().joinToString("\n")
            val sendMessage = SendMessage.builder().text("管理员列表如下：\n$ss").chatId(it.message.chatId).build()
            execute(sendMessage)
        }

        query("addBlack") {
            val chatId = it.message.chatId
            execute(SendMessage(chatId.toString(), "请发送需要新增的管理员id"))
            val ss = it.waitNextMessage().text.toLongOrNull() ?: error("发送的不为数字")
            telegramBot.admins().add(ss)
            execute(SendMessage(chatId.toString(), "增加管理员成功"))
        }

        query("deleteBlack") {
            val chatId = it.message.chatId
            execute(SendMessage(chatId.toString(), "请发送需要删除的管理员id"))
            val ss = it.waitNextMessage().text.toLongOrNull() ?: error("发送的不为数字")
            telegramBot.admins().remove(ss)
            execute(SendMessage(chatId.toString(), "删除管理员成功"))
        }
    }

}