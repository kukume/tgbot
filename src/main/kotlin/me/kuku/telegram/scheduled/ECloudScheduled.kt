package me.kuku.telegram.scheduled

import me.kuku.telegram.entity.ECloudService
import me.kuku.telegram.entity.LogService
import me.kuku.telegram.entity.LogType
import me.kuku.telegram.entity.Status
import me.kuku.telegram.logic.ECloudLogic
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ECloudScheduled(
    private val eCloudService: ECloudService,
    private val logService: LogService
) {

    @Scheduled(cron = "23 14 2 * * ?")
    suspend fun sign() {
        val list = eCloudService.findBySign(Status.ON)
        for (eCloudEntity in list) {
            logService.log(eCloudEntity.tgId, LogType.ECloud) {
                ECloudLogic.sign(eCloudEntity)
            }
        }
    }

}