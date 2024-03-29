package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.KuGouLogic
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class KuGouScheduled(
    private val kuGouService: KuGouService,
    private val kuGouLogic: KuGouLogic,
    private val logService: LogService
) {

    @Scheduled(cron = "0 41 3 * * ?")
    suspend fun sign() {
        val list = kuGouService.findBySign(Status.ON)
        for (kuGouEntity in list) {
            logService.log(kuGouEntity, LogType.KuGou) {
                kuGouLogic.musicianSign(kuGouEntity)
                kuGouLogic.listenMusic(kuGouEntity)
                repeat(8) {
                    delay(1000 * 25)
                    kuGouLogic.watchAd(kuGouEntity)
                }
            }
            delay(3000)
        }
    }

}
