package me.kuku.telegram.extension

import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendDocument
import me.kuku.telegram.context.*
import me.kuku.telegram.entity.BotConfigEntity
import me.kuku.telegram.entity.BotConfigService
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileOutputStream

@Component
class SettingExtension(
    private val botConfigService: BotConfigService
) {

    suspend fun settingMarkup(): InlineKeyboardMarkup {
        val blackSetting = inlineKeyboardButton("黑名单", "blackSetting")
        val adminSetting = inlineKeyboardButton("管理员", "adminSetting")
        val url = inlineKeyboardButton("设置推送url", "pushUrlSetting")
        val rrOcr = inlineKeyboardButton("设置全局rrocr的key", "settingGlobeRrOcr")
        val twoCaptcha = inlineKeyboardButton("设置全局2captcha的key", "settingGlobeTwoCaptcha")
        val sendLog = inlineKeyboardButton("发送日志", "settingsSendLog")
        val clearLog = inlineKeyboardButton("清空日志", "settingsClearLog")
        val updatePush = inlineKeyboardButton("${init().updatePush}github更新推送", "githubUpdatePushSwitch")
        return InlineKeyboardMarkup(
            arrayOf(blackSetting, adminSetting),
            arrayOf(url),
            arrayOf(rrOcr),
            arrayOf(twoCaptcha),
            arrayOf(sendLog, clearLog),
            arrayOf(updatePush)
        )
    }

    suspend fun Context.indexMessage() {
        val entity = init()
        val text = """
            请选择设置选项
            [rrocr](https://www.rrocr.com)：${entity.rrOcrKey.ifEmpty { "未设置" }}
            [2captcha](https://2captcha.com)：${entity.twoCaptchaKey.ifEmpty { "未设置" }}
        """.trimIndent()
        if (this is AbilityContext) {
            sendMessage(text, settingMarkup(), ParseMode.Markdown)
        } else if (this is TelegramContext) {
            editMessageText(text, settingMarkup(), returnButton = false, parseMode = ParseMode.Markdown)
        }
    }

    fun AbilitySubscriber.setting() {
        sub("setting", privacy = Privacy.CREATOR) {
            indexMessage()
        }
    }

    private suspend fun init(): BotConfigEntity {
        return botConfigService.init()
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
            val ss = init().blacklist.joinToString("\n") { "`$it`" }
            editMessageText("黑名单列表如下：\n$ss", parseMode = ParseMode.Markdown)
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
            val ss = init().admins.joinToString("\n") { "`$it`" }
            editMessageText("管理员列表如下：\n$ss", parseMode = ParseMode.Markdown)
        }

        callback("addAdmin") {
            editMessageText("请发送需要新增的管理员id")
            val ss = nextMessage().text().toLongOrNull() ?: error("发送的不为数字")
            val entity = init()
            entity.admins.add(ss)
            botConfigService.save(entity)
            editMessageText("增加管理员（$ss）成功")
        }

        callback("deleteAdmin") {
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
            botConfigService.save(entity)
            editMessageText("设置推送url成功")
        }
        callback("settingGlobeRrOcr") {
            editMessageText("请发送全局的RrOcr的key")
            val key = nextMessage().text()
            val entity = init()
            entity.rrOcrKey = key
            botConfigService.save(entity)
            editMessageText("设置rrocr的key成功")
        }
        callback("settingGlobeTwoCaptcha") {
            editMessageText("请发送全局的2captcha的key")
            val key = nextMessage().text()
            val entity = init()
            entity.twoCaptchaKey = key
            botConfigService.save(entity)
            editMessageText("设置2captcha的key成功")
        }
    }

    fun TelegramSubscribe.log() {
        callback("settingsSendLog") {
            val file = File("tmp" + File.separator + "spring.log")
            val sendDocument = SendDocument(tgId, file)
            bot.asyncExecute(sendDocument)
            editMessageText("发送日志文件成功")
        }
        callback("settingsClearLog") {
            val file = File("tmp" + File.separator + "spring.log")
            FileOutputStream(file).write("".toByteArray())
            editMessageText("清空日志文件成功")
        }
    }

    fun TelegramSubscribe.updateLogSwitch() {
        callback("githubUpdatePushSwitch") {
            val entity = init()
            entity.updatePush = !entity.updatePush
            botConfigService.save(entity)
            indexMessage()
        }
    }

}
