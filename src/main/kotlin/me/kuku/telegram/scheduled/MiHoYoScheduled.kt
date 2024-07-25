package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.config.Cron
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.MiHoYoLogic

@Cron("08:13:00")
suspend fun genShinSign() {
    val list = MiHoYoService.findBySign(Status.ON)
    for (miHoYoEntity in list) {
        LogService.log(miHoYoEntity, LogType.GenShin) {
            MiHoYoLogic.sign(miHoYoEntity, miHoYoEntity.tgId)
        }
        delay(3000)
    }
}

@Cron("08:23:00")
suspend fun mysSign() {
    val list = MiHoYoService.findByMysSign(Status.ON)
    for (miHoYoEntity in list) {
        LogService.log(miHoYoEntity, LogType.Mys) {
            MiHoYoLogic.mysSign(miHoYoEntity)
        }
        delay(3000)
    }
}