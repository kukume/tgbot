package me.kuku.telegram.extension

import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import me.kuku.telegram.entity.ConfigEntity
import me.kuku.telegram.entity.ConfigService
import me.kuku.telegram.entity.Status
import me.kuku.telegram.utils.*
import org.springframework.stereotype.Component

@Component
class ConfigExtension(
    private val configService: ConfigService
) {

    private fun keyboardMarkup(): InlineKeyboardMarkup {
        val positiveEnergyOpenButton = InlineKeyboardButton("正能量（新闻联播）推送（开）").callbackData("positiveEnergyOpen")
        val positiveEnergyCloseButton = InlineKeyboardButton("正能量（新闻联播）推送（关）").callbackData("positiveEnergyClose")
        val settingRrOcrButton = inlineKeyboardButton("设置rrcor的key", "settingRrOcr")
        return InlineKeyboardMarkup(
            arrayOf(positiveEnergyOpenButton, positiveEnergyCloseButton),
            arrayOf(settingRrOcrButton)
        )
    }

    fun text(configEntity: ConfigEntity): String {
        return """
            配置管理，当前配置：
            正能量（新闻联播）推送：${configEntity.positiveEnergy.str()}
            rrocr的key：${configEntity.rrOcrKey}
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
        callback("settingRrOcr") {
            editMessageText("请发送rrocr的key")
            val key = nextMessage().text()
            firstArg<ConfigEntity>().rrOcrKey = key
        }
        after {
            val configEntity = firstArg<ConfigEntity>()
            configService.save(configEntity)
            editMessageText(text(configEntity))
        }
    }


}
