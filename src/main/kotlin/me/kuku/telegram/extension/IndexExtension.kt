package me.kuku.telegram.extension

import com.pengrad.telegrambot.model.request.ParseMode
import me.kuku.telegram.context.AbilitySubscriber

fun AbilitySubscriber.start() {
    sub("start") {
        sendMessage("""
                *kuku*的自动签到机器人。
                机器人开源地址 https://github.com/kukume/tgbot
                求求点个star把
                有问题可以发issues
            """.trimIndent(), parseMode = ParseMode.Markdown)
    }
}