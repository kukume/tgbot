package me.kuku.telegram.scheduled

import me.kuku.telegram.config.Cron
import me.kuku.telegram.entity.IqyService
import me.kuku.telegram.entity.LogService
import me.kuku.telegram.entity.LogType
import me.kuku.telegram.entity.Status
import me.kuku.telegram.logic.IqyLogic

@Cron("05:41:08")
suspend fun iqySign() {
    val list = IqyService.findBySign(Status.ON)
    for (entity in list) {
        LogService.log(entity, LogType.Iqy) {
            IqyLogic.taskSign(entity)
            IqyLogic.finishTaskWatch(entity)
        }
    }
}