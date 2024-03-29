package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.entity.GlaDosService
import me.kuku.telegram.entity.LogService
import me.kuku.telegram.entity.LogType
import me.kuku.telegram.entity.Status
import me.kuku.telegram.logic.GlaDosLogic
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class GlaDosScheduled(
    private val glaDosService: GlaDosService,
    private val logService: LogService
) {

    @Scheduled(cron = "1 51 5 * * ?")
    suspend fun sign() {
        val list = glaDosService.findBySign(Status.ON)
        for (glaDosEntity in list) {
            logService.log(glaDosEntity, LogType.GlaDos) {
                delay(3000)
                GlaDosLogic.sign(glaDosEntity)
            }
        }
    }


}
