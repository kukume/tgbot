package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.entity.NetEaseService
import me.kuku.telegram.entity.Status
import me.kuku.telegram.logic.NetEaseLogic
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class NetEaseScheduled(
    private val netEaseService: NetEaseService
) {

    @Scheduled(cron = "0 12 7 * * ?")
    suspend fun sign() {
        val list = netEaseService.findBySign(Status.ON)
        for (netEaseEntity in list) {
            kotlin.runCatching {
                delay(3000)
                NetEaseLogic.sign(netEaseEntity)
                delay(3000)
                NetEaseLogic.listenMusic(netEaseEntity)
            }
        }
    }

    @Scheduled(cron = "0 32 8 * * ?")
    suspend fun musicianSign() {
        val list = netEaseService.findByMusicianSign(Status.ON)
        for (netEaseEntity in list) {
            kotlin.runCatching {
                for (i in 0..1) {
                    NetEaseLogic.musicianSign(netEaseEntity)
                    delay(3000)
                    NetEaseLogic.publish(netEaseEntity)
                    delay(3000)
                    NetEaseLogic.publishMLog(netEaseEntity)
                    delay(1000 * 60)
                }
            }
        }
    }

}