package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.SmZdmLogic
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SmZdmScheduled(
    private val smZdmService: SmZdmService,
    private val smZdmLogic: SmZdmLogic,
    private val logService: LogService
) {

    @Scheduled(cron = "42 32 6 * * ?")
    suspend fun sign() {
        val entityList = smZdmService.findBySign(Status.ON)
        for (smZdmEntity in entityList) {
            logService.log(smZdmEntity, LogType.SmZdm) {
                delay(3000)
                smZdmLogic.webSign(smZdmEntity, smZdmEntity.tgId)
                smZdmLogic.appSign(smZdmEntity)
            }
        }
    }


}
