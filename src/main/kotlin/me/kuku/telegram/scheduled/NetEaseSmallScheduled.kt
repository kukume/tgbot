package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.entity.NetEaseService
import me.kuku.telegram.entity.Status
import me.kuku.telegram.logic.NetEaseLogic
import me.kuku.telegram.logic.NetEaseSmallLogic
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class NetEaseSmallScheduled(
    private val netEaseSmallLogic: NetEaseSmallLogic,
    private val netEaseService: NetEaseService
) {

//    @Scheduled(fixedDelay = 12, initialDelay = 0, timeUnit = TimeUnit.HOURS)
    suspend fun listenMusic() {
        netEaseSmallLogic.check()
        val list = netEaseService.findByListen(Status.ON)
        for (netEaseEntity in list) {
            val myMusicList = try {
                NetEaseLogic.myMusic(netEaseEntity)
            } catch (e: Exception) {
                continue
            }
            for (netEaseSong in myMusicList) {
                netEaseSmallLogic.listenMusic(netEaseSong.songId)
                delay(2000)
            }
        }
    }

}