package me.kuku.telegram.extension

import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import me.kuku.telegram.context.AbilitySubscriber
import me.kuku.telegram.context.inlineKeyboardButton
import org.springframework.stereotype.Component

@Component
class OtherPushExtension(
) {

    private fun Boolean.toStr() = if (this) "√" else "x"

    fun AbilitySubscriber.otherPush() {
        sub("otherpush") {
            val switchEmail = inlineKeyboardButton("邮件推送" , "switchEmail")
            val keyboardMarkup = InlineKeyboardMarkup(
                arrayOf(switchEmail)
            )
            sendMessage("""
                第三方推送，为了防止某些人fcm不正常，收不到telegram的推送，所以可以选择开启第三方推送，请切换推送开关
            """.trimIndent(), keyboardMarkup)
        }
    }

}