package me.kuku.telegram.extension

import me.kuku.telegram.utils.AbilitySubscriber
import org.springframework.stereotype.Service

@Service
class IndexExtension {

    fun AbilitySubscriber.start() {
        sub("start", "start") {
            sendMessage("""
                *kuku*的自动签到机器人。发送 /commands 可以查看所有指令
                机器人开源地址 https://github.com/kukume/tgbot
                求求点个star把
                有问题可以发issues或者email：`kuku@kuku.me`
            """.trimIndent(), parseMode = "markdown")
        }
    }

}
