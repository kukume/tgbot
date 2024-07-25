package me.kuku.telegram.scheduled

import me.kuku.telegram.config.Cron
import me.kuku.telegram.entity.ECloudService
import me.kuku.telegram.entity.LogService
import me.kuku.telegram.entity.LogType
import me.kuku.telegram.entity.Status
import me.kuku.telegram.logic.ECloudLogic

@Cron("02:14:23")
suspend fun eCloudSign() {
    val list = ECloudService.findBySign(Status.ON)
    for (eCloudEntity in list) {
        LogService.log(eCloudEntity, LogType.ECloud) {
            ECloudLogic.sign(eCloudEntity)
        }
    }
}