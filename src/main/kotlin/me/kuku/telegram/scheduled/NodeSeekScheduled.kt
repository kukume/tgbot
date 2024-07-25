package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.config.Cron
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.NodeSeekLogic

@Cron("02:25:00")
suspend fun nodeSeekSign() {
    val entityList = NodeSeekService.findAll().filter { it.sign != NodeSeekEntity.Sign.None }
    for (entity in entityList) {
        delay(3000)
        NodeSeekLogic.sign(entity, entity.sign == NodeSeekEntity.Sign.Random)
    }
}

@Cron("05:25:00")
suspend fun querySign() {
    val entityList = NodeSeekService.findAll().filter { it.sign != NodeSeekEntity.Sign.None }
    for (entity in entityList) {
        LogService.log(entity, LogType.NodeSeek) {
            delay(3000)
            val num = NodeSeekLogic.querySign(entity)
            text = "成功，获得鸡腿${num}个"
        }
    }
}