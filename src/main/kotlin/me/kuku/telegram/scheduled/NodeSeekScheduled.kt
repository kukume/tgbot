package me.kuku.telegram.scheduled

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendMessage
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.NodeSeekLogic
import me.kuku.telegram.logic.NodeSeekPost
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class NodeSeekScheduled(
    private val nodeSeekService: NodeSeekService,
    private val logService: LogService,
    private val telegramBot: TelegramBot
) {

    @Scheduled(cron = "0 25 4 * * ?")
    suspend fun sign() {
        val entityList = nodeSeekService.findAll().filter { it.sign != NodeSeekEntity.Sign.None }
        for (entity in entityList) {
            val logEntity = LogEntity().also {
                it.tgId = entity.tgId
                it.type = LogType.NodeSeek
            }
            kotlin.runCatching {
                NodeSeekLogic.sign(entity, entity.sign == NodeSeekEntity.Sign.Random)
                logEntity.text = "成功"
            }.onFailure {
                logEntity.text = "失败"
                logEntity.errReason = it.message ?: "未知异常原因"
                logEntity.sendFailMessage(it.message)
            }
            logService.save(logEntity)
        }
    }

    private var nodeSeekId = 0

    @Scheduled(fixedDelay = 2, timeUnit = TimeUnit.MINUTES)
    suspend fun locPush() {
        val list = NodeSeekLogic.post()
        if (list.isEmpty()) return
        val newList = mutableListOf<NodeSeekPost>()
        if (nodeSeekId != 0) {
            for (nodeSeekPost in list) {
                if (nodeSeekPost.id() <= nodeSeekId) break
                newList.add(nodeSeekPost)
            }
        }
        nodeSeekId = list[0].id()
        for (nodeSeekPost in newList) {
            val nodeSeekList = nodeSeekService.findByPush(Status.ON)
            for (entity in nodeSeekList) {
                val str = """
                    #NodeSeek新帖推送
                    标题：${nodeSeekPost.title}
                    昵称：#${nodeSeekPost.username}
                    链接：${nodeSeekPost.url}
                    分类：${nodeSeekPost.category}
                """.trimIndent()
                val sendMessage = SendMessage(entity.tgId, str)
                telegramBot.execute(sendMessage)
            }
        }
    }

}
