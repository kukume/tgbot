package me.kuku.telegram.scheduled

import me.kuku.telegram.config.Cron
import me.kuku.telegram.entity.NetEaseService
import me.kuku.telegram.entity.Status
import me.kuku.telegram.logic.NetEaseLogic
import me.kuku.telegram.logic.NetEaseSmallLogic

@Cron("2m")
suspend fun netEaseSmallListenMusic() {
    NetEaseSmallLogic.check()
    val list = NetEaseService.findByMusicianSign(Status.ON)
    for (netEaseEntity in list) {
        val myMusicList = NetEaseLogic.myMusic(netEaseEntity)
        for (netEaseSong in myMusicList) {
            NetEaseSmallLogic.listenMusic(netEaseSong.songId.toInt())
        }
    }
}