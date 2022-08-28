package me.kuku.telegram.scheduled

import me.kuku.telegram.entity.MiHoYoService
import me.kuku.telegram.entity.Status
import me.kuku.telegram.logic.MiHoYoLogic
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MiHoYoScheduled(
    private val miHoYoService: MiHoYoService
) {


    @Scheduled(cron = "0 13 5 * * ?")
    suspend fun genShinSign() {
        val list = miHoYoService.findBySign(Status.ON)
        for (miHoYoEntity in list) {
            MiHoYoLogic.sign(miHoYoEntity)
        }
    }


}