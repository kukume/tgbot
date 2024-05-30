package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.NetEaseLogic
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class NetEaseScheduled(
    private val netEaseService: NetEaseService,
    private val logService: LogService
) {

    @Scheduled(cron = "0 12 7 * * ?")
    suspend fun sign() {
        val list = netEaseService.findBySign(Status.ON)
        for (netEaseEntity in list) {
            logService.log(netEaseEntity, LogType.NetEase) {
                delay(3000)
                NetEaseLogic.listenMusic(netEaseEntity)
                delay(3000)
                NetEaseLogic.sign(netEaseEntity)
            }
        }
    }

    private suspend fun execMusicianSign(netEaseEntity: NetEaseEntity) {
        NetEaseLogic.musicianSign(netEaseEntity)
        delay(3000)
        NetEaseLogic.myMusicComment(netEaseEntity)
        delay(3000)
        NetEaseLogic.publishAndShareMySongAndComment(netEaseEntity)
        delay(1000 * 60)
        NetEaseLogic.publishMLog(netEaseEntity)
    }

    @Scheduled(cron = "0 32 8 * * ?")
    suspend fun musicianSign() {
        val list = netEaseService.findByMusicianSign(Status.ON)
        for (netEaseEntity in list) {
            logService.log(netEaseEntity, LogType.NetEaseMusician) {
                try {
                    execMusicianSign(netEaseEntity)
                } catch (e: Exception) {
                    execMusicianSign(netEaseEntity)
                }
            }
        }
    }

    @Scheduled(cron = "0 40 8 * * ?")
    suspend fun vipSign() {
        val list = netEaseService.findByVipSign(Status.ON)
        for (netEaseEntity in list.reversed()) {
            logService.log(netEaseEntity, LogType.NetEaseVip) {
                delay(3000)
                NetEaseLogic.vipSign(netEaseEntity)
                NetEaseLogic.receiveTaskReward(netEaseEntity)
            }
        }
    }

}
