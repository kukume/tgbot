package me.kuku.telegram.extension

import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.ParseMode
import me.kuku.telegram.context.AbilitySubscriber
import me.kuku.telegram.context.TelegramSubscribe
import me.kuku.telegram.context.inlineKeyboardButton
import me.kuku.telegram.context.nextMessage
import me.kuku.telegram.entity.ConfigEntity
import me.kuku.telegram.entity.ConfigService
import org.springframework.stereotype.Component

@Component
class ConfigExtension(
    private val configService: ConfigService
) {

    private fun keyboardMarkup(configEntity: ConfigEntity): InlineKeyboardMarkup {
        val positiveEnergySwitch = InlineKeyboardButton("${configEntity.positiveEnergy}新闻联播推送")
            .callbackData("positiveEnergySwitch")
        val settingTwoCaptcha = inlineKeyboardButton("配置2captcha的key", "settingTwoCaptcha")
        val v2exPushSwitch = inlineKeyboardButton("${configEntity.v2exPush}v2ex推送", "v2exPushSwitch")
        val xianBaoSwitch = inlineKeyboardButton("${configEntity.xianBaoPush}线报推送", "xianBaoSwitch")
        val epicFreeGameSwitch = inlineKeyboardButton("${configEntity.epicFreeGamePush}epic免费游戏推送",
            "epicFreeGameSwitch")
        return InlineKeyboardMarkup(
            arrayOf(positiveEnergySwitch),
            arrayOf(v2exPushSwitch),
            arrayOf(xianBaoSwitch),
            arrayOf(epicFreeGameSwitch),
            arrayOf(settingTwoCaptcha)
        )
    }

    fun text(configEntity: ConfigEntity): String {
        return """
            谨慎充值打码网站，有跑路风险
            配置管理，当前配置：
            [2captcha](https://2captcha.com)的key：`${configEntity.twoCaptchaKey}`
        """.trimIndent()
    }

    fun AbilitySubscriber.config() {
        sub("config") {
            val configEntity = configService.findByTgId(tgId)?: configService.save(ConfigEntity().init())
            val markup = keyboardMarkup(configEntity)
            sendMessage(text(configEntity), replyKeyboard = markup, parseMode = ParseMode.MarkdownV2)
        }
    }

    fun TelegramSubscribe.positiveEnergyConfig() {
        before { set(configService.findByTgId(tgId)!!) }
        callback("positiveEnergySwitch") { firstArg<ConfigEntity>().also { it.positiveEnergy = !it.positiveEnergy } }
        callback("v2exPushSwitch") { firstArg<ConfigEntity>().also { it.v2exPush = !it.v2exPush } }
        callback("xianBaoSwitch") { firstArg<ConfigEntity>().also { it.xianBaoPush = !it.xianBaoPush } }
        callback("epicFreeGameSwitch") { firstArg<ConfigEntity>().also { it.epicFreeGamePush = !it.epicFreeGamePush } }
        callback("settingTwoCaptcha") {
            editMessageText("请发送2captcha的key")
            val key = nextMessage().text()
            firstArg<ConfigEntity>().twoCaptchaKey = key
        }
        after {
            val configEntity = firstArg<ConfigEntity>()
            configService.save(configEntity)
            editMessageText(text(configEntity), keyboardMarkup(configEntity), returnButton = false)
        }
    }


}
