package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.config.Cron
import me.kuku.telegram.entity.GlaDosService
import me.kuku.telegram.entity.LogService
import me.kuku.telegram.entity.LogType
import me.kuku.telegram.entity.Status
import me.kuku.telegram.logic.GlaDosLogic

@Cron("05:51:01")
suspend fun glaDosSign() {
    val list = GlaDosService.findBySign(Status.ON)
    for (glaDosEntity in list) {
        LogService.log(glaDosEntity, LogType.GlaDos) {
            delay(3000)
            GlaDosLogic.sign(glaDosEntity)
        }
    }
}