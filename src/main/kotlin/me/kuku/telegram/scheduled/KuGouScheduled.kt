package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.config.Cron
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.KuGouLogic

@Cron("03:41:00")
suspend fun kuGouSign() {
    val list = KuGouService.findBySign(Status.ON)
    for (kuGouEntity in list) {
        LogService.log(kuGouEntity, LogType.KuGou) {
            KuGouLogic.musicianSign(kuGouEntity)
            KuGouLogic.listenMusic(kuGouEntity)
            repeat(8) {
                delay(1000 * 25)
                KuGouLogic.watchAd(kuGouEntity)
            }
        }
        delay(3000)
    }
}