package me.kuku.telegram.extension

import me.kuku.telegram.entity.ConfigEntity
import me.kuku.telegram.entity.ConfigService
import me.kuku.telegram.entity.Status
import me.kuku.telegram.utils.ability
import me.kuku.telegram.utils.callback
import me.kuku.telegram.utils.execute
import org.springframework.stereotype.Component
import org.telegram.abilitybots.api.bot.BaseAbilityBot
import org.telegram.abilitybots.api.util.AbilityExtension
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
class ConfigExtension(
    private val configService: ConfigService
): AbilityExtension {

    private fun keyboardMarkup(): InlineKeyboardMarkup {
        val positiveEnergyOpenButton = InlineKeyboardButton("正能量（新闻联播）推送（开）").apply { callbackData = "positiveEnergyOpen" }
        val positiveEnergyCloseButton = InlineKeyboardButton("正能量（新闻联播）推送（关）").apply { callbackData = "positiveEnergyClose" }
        return InlineKeyboardMarkup(listOf(
            listOf(positiveEnergyOpenButton, positiveEnergyCloseButton)
        ))
    }

    fun text(configEntity: ConfigEntity): String {
        return """
            配置管理，当前配置：
            正能量（新闻联播）推送：${configEntity.positiveEnergy.str()}
        """.trimIndent()
    }

    fun editMessage(bot: BaseAbilityBot, message: Message, configEntity: ConfigEntity) {
        val editMessageText = EditMessageText.builder()
            .text(text(configEntity)).replyMarkup(keyboardMarkup()).chatId(message.chatId).messageId(message.messageId).build()
        bot.execute(editMessageText)
    }

    fun config() = ability("config", "用户配置") {
        val tgId = user().id
        val configEntity = configService.findByTgId(tgId)?: run {
            val configEntity = ConfigEntity()
            configEntity.tgId = tgId
            configService.save(configEntity)
        }
        val markup = keyboardMarkup()
        val sendMessage = SendMessage()
        sendMessage.replyMarkup = markup
        sendMessage.chatId = chatId().toString()
        sendMessage.text = text(configEntity)
        execute(sendMessage)
    }

    fun positiveEnergyConfig() = callback {
        query("positiveEnergyOpen") {
            val configEntity = configService.findByTgId(query.from.id)!!
            configEntity.positiveEnergy = Status.ON
            configService.save(configEntity)
            editMessage(bot, query.message, configEntity)
        }
        query("positiveEnergyClose") {
            val configEntity = configService.findByTgId(query.from.id)!!
            configEntity.positiveEnergy = Status.OFF
            configService.save(configEntity)
            editMessage(bot, query.message, configEntity)
        }
    }



}
