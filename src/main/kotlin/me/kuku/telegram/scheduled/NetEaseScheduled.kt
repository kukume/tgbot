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
            logService.log(netEaseEntity.tgId, LogType.NetEase) {
                delay(3000)
                NetEaseLogic.listenMusic(netEaseEntity)
                delay(3000)
                val result = NetEaseLogic.sign(netEaseEntity)
                if (result.failure()) {
                    error(result.message)
                }
            }
        }
    }

    @Scheduled(cron = "0 32 8 * * ?")
    suspend fun musicianSign() {
        val list = netEaseService.findByMusicianSign(Status.ON)
        for (netEaseEntity in list) {
            logService.log(netEaseEntity.tgId, LogType.NetEaseMusician) {
                var b = false
                var errorReason: String? = null
                for (i in 0..1) {
                    val result = NetEaseLogic.musicianSign(netEaseEntity)
                    if (result.success()) {
                        b = true
                        delay(3000)
                        NetEaseLogic.publish(netEaseEntity)
                        delay(3000)
                        NetEaseLogic.publishMLog(netEaseEntity)
                        delay(3000)
                        NetEaseLogic.myMusicComment(netEaseEntity)
                        delay(1000 * 60)
                    } else {
                        errorReason = result.message
                    }
                }
                if (!b) {
                    error(errorReason ?: "失败")
                }
            }
        }
    }

    @Scheduled(cron = "0 40 8 * * ?")
    suspend fun vipSign() {
        val list = netEaseService.findByVipSign(Status.ON)
        for (netEaseEntity in list.reversed()) {
            logService.log(netEaseEntity.tgId, LogType.NetEaseVip) {
                NetEaseLogic.vipSign(netEaseEntity)
                NetEaseLogic.receiveTaskReward(netEaseEntity)
            }
        }
    }

}
