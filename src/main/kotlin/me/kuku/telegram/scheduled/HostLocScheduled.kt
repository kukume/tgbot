package me.kuku.telegram.scheduled

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendMessage
import kotlinx.coroutines.delay
import me.kuku.telegram.context.asyncExecute
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.HostLocLogic
import me.kuku.telegram.logic.HostLocPost
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.net.ConnectException
import java.util.concurrent.TimeUnit

@Component
class HostLocScheduled(
    private val hostLocService: HostLocService,
    private val telegramBot: TelegramBot,
    private val logService: LogService
) {
    private var locId = 0

    @Scheduled(fixedDelay = 2, timeUnit = TimeUnit.MINUTES)
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
        val hostLocList = hostLocService.findByPush(Status.ON)
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

    @Scheduled(cron = "0 12 4 * * ?")
    suspend fun sign() {
        val list = hostLocService.findBySign(Status.ON)
        for (hostLocEntity in list) {
            logService.log(hostLocEntity, LogType.HostLoc) {
                delay(3000)
                HostLocLogic.sign(hostLocEntity.cookie)
            }
        }
    }

}
