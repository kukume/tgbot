package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.entity.LinuxDoService
import me.kuku.telegram.entity.Status
import me.kuku.telegram.logic.LinuxDoLogic
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class LinuxDoScheduled(
    private val linuxDoService: LinuxDoService,
    private val linuxDoLogic: LinuxDoLogic
) {


    @Scheduled(cron = "45 41 6 * * ?")
    suspend fun index() {
        val list = linuxDoService.findBySign(Status.ON)
        for (linuxDoEntity in list) {
            linuxDoLogic.index(linuxDoEntity)
            delay(1000 * 10)
        }
    }

    @Scheduled(fixedDelay = 3, initialDelay = 1, timeUnit = TimeUnit.HOURS)
    suspend fun post() {
        val list = linuxDoService.findBySign(Status.ON)
        val topic = LinuxDoLogic.latestTopic().random()
        for (linuxDoEntity in list) {
            linuxDoLogic.topic(linuxDoEntity, topic.suffix)
            delay(1000 * 10)
        }
    }

}