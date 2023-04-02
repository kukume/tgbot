package me.kuku.telegram.extension

import me.kuku.telegram.config.TelegramBot
import me.kuku.telegram.utils.*
import org.springframework.stereotype.Component
import org.telegram.abilitybots.api.objects.Privacy
import org.telegram.abilitybots.api.util.AbilityExtension
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
        val url = inlineKeyboardButton("设置推送url", "pushUrlSetting")
        return InlineKeyboardMarkup(listOf(
            listOf(blackSetting, adminSetting),
            listOf(url)
        ))
    }

    fun returnButton(): List<InlineKeyboardButton> {
        return listOf(inlineKeyboardButton("返回", "returnSetting"))
    }

    fun AbilitySubscriber.setting() {
        sub("setting", "机器人设置", privacy = Privacy.CREATOR) {
            sendMessage("请选择设置选项", settingMarkup())
        }
    }

    fun returnSetting() = callback("returnSetting") {
        val messageId = query.message.messageId
        val editMessageText = EditMessageText.builder().text("请选择设置选项").chatId(query.message.chatId).messageId(messageId)
            .replyMarkup(settingMarkup()).build()
        bot.execute(editMessageText)
    }

    fun TelegramSubscribe.blackSetting() {
        callback("blackSetting") {
            val queryBlackButton = inlineKeyboardButton("查询", "queryBlack")
            val addBlackButton = inlineKeyboardButton("增加", "addBlack")
            val deleteBlackButton = inlineKeyboardButton("删除", "deleteBlack")
            val markup = InlineKeyboardMarkup(
                listOf(
                    listOf(queryBlackButton),
                    listOf(addBlackButton),
                    listOf(deleteBlackButton)
                )
            )
            editMessageText("请选择黑名单操作：", markup)
        }

        callback("queryBlack") {
            val ss = telegramBot.blacklist().joinToString("\n")
            editMessageText("黑名单列表如下：\n$ss")
        }

        callback("addBlack") {
            editMessageText("请发送需要新增的黑名单id")
            val ss = nextMessage().text.toLongOrNull() ?: error("发送的不为数字")
            telegramBot.blacklist().add(ss)
            editMessageText("增加黑名单（$ss）成功")
        }

        callback("deleteBlack") {
            editMessageText("请发送需要删除的黑名单id")
            val ss = nextMessage().text.toLongOrNull() ?: error("发送的不为数字")
            telegramBot.blacklist().remove(ss)
            editMessageText("删除黑名单（${ss}）成功")
        }

    }

    fun TelegramSubscribe.adminSetting() {
        callback("adminSetting") {
            val queryBlackButton = inlineKeyboardButton("查询", "queryAdmin")
            val addBlackButton = inlineKeyboardButton("增加", "addAdmin")
            val deleteBlackButton = inlineKeyboardButton("删除", "deleteAdmin")
            val markup = InlineKeyboardMarkup(
                listOf(
                    listOf(queryBlackButton),
                    listOf(addBlackButton),
                    listOf(deleteBlackButton),
                    returnButton()
                )
            )
            editMessageText("请选择管理员操作：", markup)
        }

        callback("queryAdmin") {
            val ss = telegramBot.admins().joinToString("\n")
            editMessageText("管理员列表如下：\n$ss")
        }

        callback("addBlack") {
            editMessageText("请发送需要新增的管理员id")
            val ss = nextMessage().text.toLongOrNull() ?: error("发送的不为数字")
            telegramBot.admins().add(ss)
            editMessageText("增加管理员（$ss）成功")
        }

        callback("deleteBlack") {
            editMessageText("请发送需要删除的管理员id")
            val ss = nextMessage().text.toLongOrNull() ?: error("发送的不为数字")
            telegramBot.admins().remove(ss)
            editMessageText("删除管理员（$ss）成功")
        }
    }

    fun TelegramSubscribe.url() {
        callback("pushUrlSetting") {
            editMessageText("请发送推送url，例如<http://127.0.0.1:5460>，带上http，最后不要/")
            val url = nextMessage().text + "/push"
            val stringVar = telegramBot.db().getVar<String>("pushUrl")
            stringVar.set(url)
            editMessageText("设置推送url成功")
        }
    }

}
