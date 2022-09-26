package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.entity.KuGouService
import me.kuku.telegram.entity.Status
import me.kuku.telegram.logic.KuGouLogic
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class KuGouScheduled(
    private val kuGouService: KuGouService,
    private val kuGouLogic: KuGouLogic
) {

    @Scheduled(cron = "0 41 3 * * ?")
    suspend fun sign() {
        val list = kuGouService.findBySign(Status.ON)
        for (kuGouEntity in list) {
            kuGouLogic.musicianSign(kuGouEntity)
            kuGouLogic.listenMusic(kuGouEntity)
            delay(3000)
        }
    }

}