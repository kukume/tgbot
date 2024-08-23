package me.kuku.telegram.extension

import me.kuku.telegram.context.AbilitySubscriber
import me.kuku.telegram.context.nextMessage
import me.kuku.telegram.entity.NetEaseSmallEntity
import me.kuku.telegram.entity.NetEaseSmallService

fun AbilitySubscriber.netEaseSmall() {
    sub("netEaseSmall") {
        sendMessage("""
            请发送网易云小号，格式为：用户名----密码，一行一个
        """.trimIndent())
        val text = nextMessage().text()
        val firstArr = text.split("\n")
        for (line in firstArr) {
            val lineSplit = line.split("----")
            val username = lineSplit[0]
            val password = lineSplit[1]
            val netEaseSmallEntity = NetEaseSmallService.findByUsername(username) ?: NetEaseSmallEntity()
            netEaseSmallEntity.username = username
            netEaseSmallEntity.password = password
            NetEaseSmallService.save(netEaseSmallEntity)
        }
        sendMessage("保存网易云小号成功")
    }
}