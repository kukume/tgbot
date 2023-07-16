package me.kuku.telegram.scheduled

import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.NodeSeekLogic
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class NodeSeekScheduled(
    private val nodeSeekService: NodeSeekService,
    private val logService: LogService
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
                val num = NodeSeekLogic.sign(entity, entity.sign == NodeSeekEntity.Sign.Random)
                logEntity.text = "成功，获得鸡腿${num}个"
            }.onFailure {
                logEntity.text = "失败"
                logEntity.errReason = it.message ?: "未知异常原因"
                logEntity.sendFailMessage(it.message)
            }
            logService.save(logEntity)
        }
    }

}
