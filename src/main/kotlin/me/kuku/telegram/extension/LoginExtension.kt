package me.kuku.telegram.extension

import me.kuku.telegram.logic.BiliBiliLogic
import me.kuku.telegram.utils.ability
import me.kuku.telegram.utils.callback
import me.kuku.telegram.utils.execute
import me.kuku.utils.OkHttpKtUtils
import me.kuku.utils.toUrlEncode
import org.springframework.stereotype.Service
import org.telegram.abilitybots.api.util.AbilityExtension
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Service
class LoginExtension: AbilityExtension {

    private fun loginKeyboardMarkup(): InlineKeyboardMarkup {
        val baiduButton = InlineKeyboardButton("百度").also { it.callbackData = "baiduLogin" }
        val biliBiliButton = InlineKeyboardButton("哔哩哔哩").also { it.callbackData = "biliBiliLogin" }
        val douYuButton = InlineKeyboardButton("斗鱼").also { it.callbackData = "douYuLogin" }
        val hostLocButton = InlineKeyboardButton("HostLoc").also { it.callbackData = "douYuLogin" }
        val huYaButton = InlineKeyboardButton("虎牙").also { it.callbackData = "huYaLogin" }
        val kuGouButton = InlineKeyboardButton("酷狗").also { it.callbackData = "kuGouLogin" }
        val miHoYoButton = InlineKeyboardButton("米忽悠").also { it.callbackData = "miHoYoLogin" }
        val netEaseButton = InlineKeyboardButton("网易云音乐").also { it.callbackData = "netEaseButton" }
        val xiaomiStepButton = InlineKeyboardButton("小米运动").also { it.callbackData = "xiaomiStepButton" }
        val leXinStepButton = InlineKeyboardButton("乐心运动").also { it.callbackData = "leXinStepButton" }
        val weiboStepButton = InlineKeyboardButton("微博").also { it.callbackData = "weiboButton" }
        return InlineKeyboardMarkup(listOf(
            listOf(baiduButton, biliBiliButton),
            listOf(douYuButton, hostLocButton),
            listOf(huYaButton, kuGouButton),
            listOf(miHoYoButton, netEaseButton),
            listOf(xiaomiStepButton, leXinStepButton),
            listOf(weiboStepButton)
        ))
    }

    fun login() = ability("login", "登录") {
        val markup = loginKeyboardMarkup()
        val sendMessage = SendMessage()
        sendMessage.replyMarkup = markup
        sendMessage.chatId = chatId().toString()
        sendMessage.text = "请选择登录选项"
        execute(sendMessage)
    }

    fun biliBiliLogin() = callback("biliBiliLogin") {
        val qrCodeUrl = BiliBiliLogic.loginByQr1()
        val bytes = OkHttpKtUtils.getBytes("https://api.kukuqaq.com/qrcode?text=${qrCodeUrl.toUrlEncode()}")
        val photo =
            SendPhoto(it.message.chatId.toString(), InputFile(bytes.inputStream(), "哔哩哔哩登录二维码.jpg"))
        execute(photo)
        silent().send("请使用哔哩哔哩APP扫码登录", it.message.chatId)
    }

}