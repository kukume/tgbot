package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.config.Cron
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.BaiduLogic

@Cron("02:41:00")
suspend fun baiduSign() {
    val list = BaiduService.findBySign(Status.ON)
    for (baiduEntity in list) {
        LogService.log(baiduEntity, LogType.Baidu) {
            for (i in 0 until 12) {
                delay(1000 * 15)
                BaiduLogic.ybbWatchAd(baiduEntity)
            }
            for (i in 0 until 4) {
                delay(1000 * 30)
                BaiduLogic.ybbWatchAd(baiduEntity, "v3")
            }
            BaiduLogic.ybbSign(baiduEntity)
            delay(2000)
            BaiduLogic.ybbExchangeVip(baiduEntity)
            BaiduLogic.tieBaSign(baiduEntity)
        }
    }
}