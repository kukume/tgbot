package me.kuku.telegram.extension

import me.kuku.telegram.utils.ability
import me.kuku.telegram.utils.sendMd
import org.springframework.stereotype.Service
import org.telegram.abilitybots.api.util.AbilityExtension

@Service
class IndexExtension: AbilityExtension {

    fun start() = ability("start", "start") {
        sendMd("""
            *kuku*的自动签到机器人。发送 /commands 可以查看所有指令哦
        """.trimIndent())
    }

    fun about() = ability("about", "关于") {
        sendMd("""
            机器人开源地址 https://github.com/kukume/tgbot
            点个star把
        """.trimIndent())
    }

}