package me.kuku.telegram.extension

import com.pengrad.telegrambot.model.request.ParseMode
import me.kuku.telegram.utils.AbilitySubscriber
import org.springframework.stereotype.Service

@Service
class IndexExtension {

    fun AbilitySubscriber.start() {
        sub("start") {
            sendMessage("""
                *kuku*的自动签到机器人。
                机器人开源地址 https://github.com/kukume/tgbot
                求求点个star把
                有问题可以发issues或者email：`kuku@kuku.me`
            """.trimIndent(), parseMode = ParseMode.Markdown)
        }
    }

}
