package me.kuku.telegram.scheduled

import com.pengrad.telegrambot.request.SendMessage
import kotlinx.coroutines.delay
import me.kuku.telegram.config.Cron
import me.kuku.telegram.config.telegramBot
import me.kuku.telegram.context.asyncExecute
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.HostLocLogic
import me.kuku.telegram.logic.HostLocPost
import java.net.ConnectException

private var locId = 0

@Cron("2m")
suspend fun locPush() {
    val list = try {
        HostLocLogic.post()
    } catch (e: ConnectException) {
        return
    }
    if (list.isEmpty()) return
    val newList = mutableListOf<HostLocPost>()
    if (locId != 0) {
        for (hostLocPost in list) {
            if (hostLocPost.id <= locId) break
            newList.add(hostLocPost)
        }
    }
    locId = list[0].id
    val hostLocList = HostLocService.findByPush(Status.ON)
    for (hostLocPost in newList) {
        delay(3000)
        for (hostLocEntity in hostLocList) {
            val str = """
                    #HostLoc新帖推送
                    标题：${hostLocPost.title}
                    昵称：#${hostLocPost.name}
                    链接：${hostLocPost.url}
                    内容：${HostLocLogic.postContent(hostLocPost.url, hostLocEntity.cookie)}
                """.trimIndent()
            val sendMessage = SendMessage(hostLocEntity.tgId, str)
            telegramBot.asyncExecute(sendMessage)
        }
    }
}

@Cron("04:12:00")
suspend fun hostLocSign() {
    val list = HostLocService.findBySign(Status.ON)
    for (hostLocEntity in list) {
        LogService.log(hostLocEntity, LogType.HostLoc) {
            delay(3000)
            HostLocLogic.sign(hostLocEntity.cookie)
        }
    }
}