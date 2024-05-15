package me.kuku.telegram.extension

import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import me.kuku.telegram.context.*
import me.kuku.telegram.entity.OtherPushEntity
import me.kuku.telegram.entity.OtherPushService
import org.springframework.stereotype.Component

@Component
class OtherPushExtension(
    private val otherPushService: OtherPushService
) {

    private fun Boolean.toStr() = if (this) "√" else "x"

    private suspend fun Context.otherPush() {
        val entity = otherPushService.findByTgId(tgId)
        val pushTypes = entity.pushTypes
        val switchEmail = inlineKeyboardButton("${pushTypes.contains(OtherPushEntity.OtherPushType.Email).toStr()}邮件推送" , "switchEmail")
        val keyboardMarkup = InlineKeyboardMarkup(
            arrayOf(switchEmail)
        )
        val text = """
            第三方推送，为了防止某些人fcm不正常，收不到telegram的推送，所以可以选择开启第三方推送，请切换推送开关
        """.trimIndent()
        if (this is AbilityContext) {
            sendMessage(text, keyboardMarkup)
        } else if (this is TelegramContext) {
            editMessageText(text, keyboardMarkup, returnButton = false)
        }
    }

    fun AbilitySubscriber.otherPush() {
        sub("otherpush") {
            otherPush()
        }
    }

    fun TelegramSubscribe.otherPush() {
        before { set(otherPushService.findByTgId(tgId)) }
        callback("switchEmail") {
            val otherPushEntity = firstArg<OtherPushEntity>()
            val pushTypes = otherPushEntity.pushTypes
            if (pushTypes.contains(OtherPushEntity.OtherPushType.Email)) {
                pushTypes.remove(OtherPushEntity.OtherPushType.Email)
                otherPushService.save(otherPushEntity)
                otherPush()
            } else {
                val receiveEmail = otherPushEntity.complex.receiveEmail
                if (receiveEmail.isNotEmpty()) {
                    editMessageText("您即将启用邮箱推送，请选择以下选项", InlineKeyboardMarkup(
                        arrayOf(inlineKeyboardButton("${receiveEmail}为接收邮箱", "switchEmail2")),
                        arrayOf(inlineKeyboardButton("更换新邮箱", "switchEmail3"))
                    ))
                } else {
                    editMessageText("请发送您接收的邮箱")
                    val email = nextMessage().text()
                    otherPushEntity.complex.receiveEmail = email
                    otherPushEntity.pushTypes.add(OtherPushEntity.OtherPushType.Email)
                    otherPushService.save(otherPushEntity)
                    otherPush()
                }
            }
        }
        callback("switchEmail2") {
            val otherPushEntity = firstArg<OtherPushEntity>()
            otherPushEntity.pushTypes.add(OtherPushEntity.OtherPushType.Email)
            otherPushService.save(otherPushEntity)
            otherPush()
        }
        callback("switchEmail3") {
            val otherPushEntity = firstArg<OtherPushEntity>()
            editMessageText("请发送您接收的邮箱")
            val email = nextMessage().text()
            otherPushEntity.complex.receiveEmail = email
            otherPushEntity.pushTypes.add(OtherPushEntity.OtherPushType.Email)
            otherPushService.save(otherPushEntity)
            otherPush()
        }
    }

}