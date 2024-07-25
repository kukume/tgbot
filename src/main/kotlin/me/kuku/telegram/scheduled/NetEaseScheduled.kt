package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.config.Cron
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.NetEaseLogic

@Cron("07:12")
suspend fun netEaseSign() {
    val list = NetEaseService.findBySign(Status.ON)
    for (netEaseEntity in list) {
        LogService.log(netEaseEntity, LogType.NetEase) {
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

@Cron("08:32:00")
suspend fun netEaseMusicianSign() {
    val list = NetEaseService.findByMusicianSign(Status.ON)
    for (netEaseEntity in list) {
        LogService.log(netEaseEntity, LogType.NetEaseMusician) {
            try {
                execMusicianSign(netEaseEntity)
            } catch (e: Exception) {
                execMusicianSign(netEaseEntity)
            }
        }
    }
}

@Cron("08:40:00")
suspend fun vipSign() {
    val list = NetEaseService.findByVipSign(Status.ON)
    for (netEaseEntity in list.reversed()) {
        LogService.log(netEaseEntity, LogType.NetEaseVip) {
            delay(3000)
            NetEaseLogic.vipSign(netEaseEntity)
            NetEaseLogic.receiveTaskReward(netEaseEntity)
        }
    }
}