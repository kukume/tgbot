package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
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
            logService.log(entity.tgId, LogType.NodeSeek) {
                delay(3000)
                val num = NodeSeekLogic.sign(entity, entity.sign == NodeSeekEntity.Sign.Random)
                text = "成功，获得鸡腿${num}个"
            }
        }
    }

}
