package me.kuku.telegram.extension

import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import me.kuku.telegram.config.TelegramConfig
import me.kuku.telegram.entity.BotConfigEntity
import me.kuku.telegram.entity.BotConfigService
import me.kuku.telegram.utils.*
import org.springframework.stereotype.Component

@Component
class SettingExtension(
    private val telegramConfig: TelegramConfig,
    private val botConfigService: BotConfigService
) {

    fun settingMarkup(): InlineKeyboardMarkup {
        val blackSetting = inlineKeyboardButton("黑名单", "blackSetting")
        val adminSetting = inlineKeyboardButton("管理员", "adminSetting")
        val url = inlineKeyboardButton("设置推送url", "pushUrlSetting")
        return InlineKeyboardMarkup(
            arrayOf(blackSetting, adminSetting),
            arrayOf(url)
        )
    }

    fun AbilitySubscriber.setting() {
        sub("setting", privacy = Privacy.CREATOR) {
            sendMessage("请选择设置选项", settingMarkup())
        }
    }

    private suspend fun init(): BotConfigEntity {
        val token = telegramConfig.token
        return botConfigService.findByToken(token) ?: kotlin.run {
            val botConfigEntity = BotConfigEntity()
            botConfigEntity.token = token
            botConfigEntity.also { botConfigService.save(botConfigEntity) }
        }
    }

    fun TelegramSubscribe.blackSetting() {
        callback("blackSetting") {
            val queryBlackButton = inlineKeyboardButton("查询", "queryBlack")
            val addBlackButton = inlineKeyboardButton("增加", "addBlack")
            val deleteBlackButton = inlineKeyboardButton("删除", "deleteBlack")
            val markup = InlineKeyboardMarkup(
                arrayOf(queryBlackButton),
                arrayOf(addBlackButton),
                arrayOf(deleteBlackButton)
            )
            editMessageText("请选择黑名单操作：", markup)
        }

        callback("queryBlack") {
            val ss = init().blacklist.joinToString("\n")
            editMessageText("黑名单列表如下：\n$ss")
        }

        callback("addBlack") {
            editMessageText("请发送需要新增的黑名单id")
            val ss = nextMessage().text().toLongOrNull() ?: error("发送的不为数字")
            val entity = init()
            entity.blacklist.add(ss)
            botConfigService.save(entity)
            editMessageText("增加黑名单（$ss）成功")
        }

        callback("deleteBlack") {
            editMessageText("请发送需要删除的黑名单id")
            val ss = nextMessage().text().toLongOrNull() ?: error("发送的不为数字")
            val entity = init()
            entity.blacklist.remove(ss)
            botConfigService.save(entity)
            editMessageText("删除黑名单（${ss}）成功")
        }

    }

    fun TelegramSubscribe.adminSetting() {
        callback("adminSetting") {
            val queryBlackButton = inlineKeyboardButton("查询", "queryAdmin")
            val addBlackButton = inlineKeyboardButton("增加", "addAdmin")
            val deleteBlackButton = inlineKeyboardButton("删除", "deleteAdmin")
            val markup = InlineKeyboardMarkup(
                arrayOf(queryBlackButton),
                arrayOf(addBlackButton),
                arrayOf(deleteBlackButton)
            )
            editMessageText("请选择管理员操作：", markup)
        }

        callback("queryAdmin") {
            val ss = init().admins.joinToString("\n")
            editMessageText("管理员列表如下：\n$ss")
        }

        callback("addBlack") {
            editMessageText("请发送需要新增的管理员id")
            val ss = nextMessage().text().toLongOrNull() ?: error("发送的不为数字")
            val entity = init()
            entity.admins.add(ss)
            botConfigService.save(entity)
            editMessageText("增加管理员（$ss）成功")
        }

        callback("deleteBlack") {
            editMessageText("请发送需要删除的管理员id")
            val ss = nextMessage().text().toLongOrNull() ?: error("发送的不为数字")
            val entity = init()
            entity.admins.remove(ss)
            botConfigService.save(entity)
            editMessageText("删除管理员（$ss）成功")
        }
    }

    fun TelegramSubscribe.url() {
        callback("pushUrlSetting") {
            editMessageText("请发送推送url，例如<http://127.0.0.1:5460>，带上http，最后不要/")
            val url = nextMessage().text() + "/push"
            val entity = init()
            entity.pushUrl = url
            editMessageText("设置推送url成功")
        }
    }

}
