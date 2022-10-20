package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.KuGouLogic
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class KuGouScheduled(
    private val kuGouService: KuGouService,
    private val kuGouLogic: KuGouLogic,
    private val logService: LogService
) {

    @Scheduled(cron = "0 41 3 * * ?")
    suspend fun sign() {
        val list = kuGouService.findBySign(Status.ON)
        for (kuGouEntity in list) {
            val logEntity = LogEntity().also {
                it.type = LogType.KuGou
                it.tgId = kuGouEntity.tgId
            }
            kotlin.runCatching {
                kuGouLogic.musicianSign(kuGouEntity)
                kuGouLogic.listenMusic(kuGouEntity)
                logEntity.text = "成功"
            }.onFailure {
                logEntity.text = "失败"
            }
            logService.save(logEntity)
            delay(3000)
        }
    }

}