package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.config.Cron
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.SmZdmLogic

@Cron("06:32:42")
suspend fun smZdmSign() {
    val entityList = SmZdmService.findBySign(Status.ON)
    for (smZdmEntity in entityList) {
        LogService.log(smZdmEntity, LogType.SmZdm) {
            delay(3000)
            SmZdmLogic.webSign(smZdmEntity, smZdmEntity.tgId)
            SmZdmLogic.appSign(smZdmEntity)
        }
    }
}