package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.config.Cron
import me.kuku.telegram.entity.LinuxDoService
import me.kuku.telegram.entity.Status
import me.kuku.telegram.logic.LinuxDoLogic

@Cron("06:41:45")
suspend fun linuxDoIndex() {
    val list = LinuxDoService.findBySign(Status.ON)
    for (linuxDoEntity in list) {
        LinuxDoLogic.index(linuxDoEntity)
        delay(1000 * 10)
    }
}

@Cron("3h")
suspend fun linuxDoPost() {
    val list = LinuxDoService.findBySign(Status.ON)
    val topic = LinuxDoLogic.latestTopic().random()
    for (linuxDoEntity in list) {
        LinuxDoLogic.topic(linuxDoEntity, topic.suffix)
        delay(1000 * 10)
    }
}