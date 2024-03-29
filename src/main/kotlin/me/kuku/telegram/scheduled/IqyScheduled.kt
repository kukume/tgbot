package me.kuku.telegram.scheduled

import me.kuku.telegram.entity.IqyService
import me.kuku.telegram.entity.LogService
import me.kuku.telegram.entity.LogType
import me.kuku.telegram.entity.Status
import me.kuku.telegram.logic.IqyLogic
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class IqyScheduled(
    private val iqyService: IqyService,
    private val logService: LogService
) {

    @Scheduled(cron = "8 41 5 * * ?")
    suspend fun sign() {
        val list = iqyService.findBySign(Status.ON)
        for (entity in list) {
            logService.log(entity, LogType.Iqy) {
                IqyLogic.taskSign(entity)
                IqyLogic.finishTaskWatch(entity)
            }
        }
    }



}