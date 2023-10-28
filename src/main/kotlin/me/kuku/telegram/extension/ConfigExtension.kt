package me.kuku.telegram.extension

import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import me.kuku.telegram.context.AbilitySubscriber
import me.kuku.telegram.context.TelegramSubscribe
import me.kuku.telegram.context.inlineKeyboardButton
import me.kuku.telegram.context.nextMessage
import me.kuku.telegram.entity.ConfigEntity
import me.kuku.telegram.entity.ConfigService
import me.kuku.telegram.entity.Status
import org.springframework.stereotype.Component

@Component
class ConfigExtension(
    private val configService: ConfigService
) {

    private fun keyboardMarkup(): InlineKeyboardMarkup {
        val positiveEnergyOpenButton = InlineKeyboardButton("新闻联播推送（开）").callbackData("positiveEnergyOpen")
        val positiveEnergyCloseButton = InlineKeyboardButton("新闻联播推送（关）").callbackData("positiveEnergyClose")
        val settingRrOcrButton = inlineKeyboardButton("设置rrcor的key", "settingRrOcr")
        val settingTwoCaptcha = inlineKeyboardButton("配置2captcha的key", "settingTwoCaptcha")
        val v2exOpen = inlineKeyboardButton("v2ex推送（开）", "v2exPushOpen")
        val v2exClose = inlineKeyboardButton("v2ex推送（关）", "v2exPushClose")
        val xianBaoOpen = inlineKeyboardButton("线报推送（开）", "xianBaoOpen")
        val xianBaoClose = inlineKeyboardButton("线报推送（关）", "xianBaoClose")
        return InlineKeyboardMarkup(
            arrayOf(positiveEnergyOpenButton, positiveEnergyCloseButton),
            arrayOf(v2exOpen, v2exClose),
            arrayOf(xianBaoOpen, xianBaoClose),
            arrayOf(settingRrOcrButton, settingTwoCaptcha)
        )
    }

    fun text(configEntity: ConfigEntity): String {
        return """
            配置管理，当前配置：
            新闻联播推送：${configEntity.positiveEnergy.str()}
            rrocr的key：${configEntity.rrOcrKey}（https://www.rrocr.com）
            2captcha的key：${configEntity.twoCaptchaKey}（https://2captcha.com）
            v2ex推送：${configEntity.v2exPush.str()}
            线报推送：${configEntity.xianBaoPush.str()}（http://new.xianbao.fun）
        """.trimIndent()
    }

    fun AbilitySubscriber.config() {
        sub("config") {
            val configEntity = configService.findByTgId(tgId)?: run {
                val configEntity = ConfigEntity()
                configEntity.tgId = tgId
                configService.save(configEntity)
            }
            val markup = keyboardMarkup()
            sendMessage(text(configEntity), replyKeyboard = markup)
        }
    }

    fun TelegramSubscribe.positiveEnergyConfig() {
        before { set(configService.findByTgId(tgId)!!) }
        callback("positiveEnergyOpen") { firstArg<ConfigEntity>().positiveEnergy = Status.ON }
        callback("positiveEnergyClose") { firstArg<ConfigEntity>().positiveEnergy = Status.OFF }
        callback("v2exPushOpen") { firstArg<ConfigEntity>().v2exPush = Status.ON }
        callback("v2exPushClose") { firstArg<ConfigEntity>().v2exPush = Status.OFF }
        callback("xianBaoOpen") { firstArg<ConfigEntity>().xianBaoPush = Status.ON }
        callback("xianBaoClose") { firstArg<ConfigEntity>().xianBaoPush = Status.OFF }
        callback("settingRrOcr") {
            editMessageText("请发送rrocr的key")
            val key = nextMessage().text()
            firstArg<ConfigEntity>().rrOcrKey = key
        }
        callback("settingTwoCaptcha") {
            editMessageText("请发送2captcha的key")
            val key = nextMessage().text()
            firstArg<ConfigEntity>().twoCaptchaKey = key
        }
        after {
            val configEntity = firstArg<ConfigEntity>()
            configService.save(configEntity)
            editMessageText(text(configEntity), keyboardMarkup(), returnButton = false)
        }
    }


}
