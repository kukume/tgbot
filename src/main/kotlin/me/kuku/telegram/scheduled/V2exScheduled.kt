package me.kuku.telegram.scheduled

import com.pengrad.telegrambot.request.SendMessage
import me.kuku.telegram.config.Cron
import me.kuku.telegram.config.telegramBot
import me.kuku.telegram.context.asyncExecute
import me.kuku.telegram.entity.ConfigService
import me.kuku.telegram.entity.Status
import me.kuku.telegram.logic.V2exLogic
import me.kuku.telegram.logic.V2exTopic
import java.net.ConnectException

private var v2exId = 0

@Cron("1m")
suspend fun v2exPush() {
    val list = try {
        V2exLogic.latestTopic()
    } catch (e: ConnectException) {
        return
    }
    if (list.isEmpty()) return
    val newList = mutableListOf<V2exTopic>()
    if (v2exId != 0) {
        for (topic in list) {
            if (topic.id <= v2exId) break
            newList.add(topic)
        }
    }
    v2exId = list[0].id
    val pushList = ConfigService.findByV2exPush(Status.ON)
    for (v2exTopic in newList) {
        for (configEntity in pushList) {
            val str = """
                    #v2ex新帖推送
                    标题：${v2exTopic.title}
                    链接：${v2exTopic.url}
                """.trimIndent()
            val sendMessage = SendMessage(configEntity.tgId, str)
            telegramBot.asyncExecute(sendMessage)
        }
    }
}