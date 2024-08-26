package me.kuku.telegram.extension

import me.kuku.telegram.context.AbilitySubscriber
import me.kuku.telegram.context.Privacy
import me.kuku.telegram.context.nextMessage
import me.kuku.telegram.entity.NetEaseSmallEntity
import me.kuku.telegram.entity.NetEaseSmallService
import org.springframework.stereotype.Service

@Service
class NetEaseSmallExtension(
    private val netEaseSmallService: NetEaseSmallService
) {

    fun AbilitySubscriber.netEaseSmall() {
        sub(name = "neteasesmall", privacy = Privacy.CREATOR) {
            sendMessage("""
            请发送网易云小号，格式为：用户名----密码，一行一个
        """.trimIndent())
            val text = nextMessage().text()
            val firstArr = text.split("\n")
            for (line in firstArr) {
                val lineSplit = line.split("----")
                val username = lineSplit[0]
                var password = lineSplit[1]
                password.indexOf('-').takeIf { it > 0 }?.let {
                    password = password.substring(0, it)
                }
                val netEaseSmallEntity = netEaseSmallService.findByUsername(username) ?: NetEaseSmallEntity()
                netEaseSmallEntity.username = username
                netEaseSmallEntity.password = password
                netEaseSmallService.save(netEaseSmallEntity)
            }
            sendMessage("保存网易云小号成功")
        }
    }

}