package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.config.Cron
import me.kuku.telegram.config.telegramBot
import me.kuku.telegram.context.sendTextMessage
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.AliDriveBatch
import me.kuku.telegram.logic.AliDriveLogic
import java.time.DayOfWeek
import java.time.LocalDate

@Cron("02:09:13")
suspend fun aliDriveSign() {
    val list = AliDriveService.findBySign(Status.ON)
    for (aliDriveEntity in list) {
        LogService.log(aliDriveEntity, LogType.AliDrive) {
            delay(3000)
            AliDriveLogic.sign(aliDriveEntity)
            if (aliDriveEntity.receive == Status.ON) {
                show = AliDriveLogic.receive(aliDriveEntity)
            }
        }
    }
}

@Cron("05:04:10")
suspend fun lastDayReceive() {
    val now = LocalDate.now()
    val nextMonth = now.plusMonths(1)
    val lastDay = LocalDate.of(nextMonth.year, nextMonth.month, 1).minusDays(1)
    if (now.dayOfMonth == lastDay.dayOfMonth) {
        val list = AliDriveService.findBySign(Status.ON)
        for (aliDriveEntity in list) {
            LogService.log(aliDriveEntity, LogType.ALiDriveReceive) {
                val signList = AliDriveLogic.sign(aliDriveEntity)
                for (signInLog in signList.signInLogs) {
                    if (!signInLog.isReward) {
                        delay(3000)
                        kotlin.runCatching {
                            AliDriveLogic.receive(aliDriveEntity, signInLog.day)
                        }
                    }
                }
            }
        }
        val taskList = AliDriveService.findByTask(Status.ON)
        for (aliDriveEntity in taskList) {
            LogService.log(aliDriveEntity, LogType.ALiDriveReceiveTask) {
                val signList = AliDriveLogic.signInList(aliDriveEntity)
                for (signInInfo in signList.signInInfos) {
                    signInInfo.rewards.getOrNull(1)?.let { reward ->
                        if (reward.status == "finished") {
                            delay(3000)
                            kotlin.runCatching {
                                AliDriveLogic.receiveTask(aliDriveEntity, signInInfo.day.toInt())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Cron("03:01:43")
suspend fun aliDriveTask() {
    val list = AliDriveService.findByTask(Status.ON)
    for (aliDriveEntity in list) {
        LogService.log(aliDriveEntity, LogType.AliDriveTask) {
            AliDriveLogic.finishTask(aliDriveEntity)
            delay(3000)
        }

    }
}

@Cron("05:30:05")
suspend fun receiveTodayTask() {
    val list = AliDriveService.findByTask(Status.ON)
    for (aliDriveEntity in list) {
        kotlin.runCatching {
            delay(3000)
            var signInInfo = AliDriveLogic.signInInfo(aliDriveEntity)
            var reward = signInInfo.rewards[1]
            if (reward.remind == "开启「自动同步电脑文件夹至少一小时」") {
                signInInfo = AliDriveLogic.signInInfo(aliDriveEntity, AliDriveLogic.backupDesktopDevice(aliDriveEntity))
                reward = signInInfo.rewards[1]
            }
            if (reward.status !in listOf("finished", "verification")) {
                error("阿里云盘任务完成失败，任务名称：${reward.remind}")
            }
        }.onSuccess {
            if (aliDriveEntity.receiveTask == Status.ON) {
                LogService.log(aliDriveEntity, LogType.AliDriveReceiveTaskToday) {
                    AliDriveLogic.signInList(aliDriveEntity)
                    show = AliDriveLogic.receiveTask(aliDriveEntity)
                }
            }
        }.onFailure {
            telegramBot.sendTextMessage(aliDriveEntity.tgId,
                "#阿里云盘任务检测\n${it.message}")
        }
    }
}

@Cron("04:50:32")
suspend fun finishDeviceRoom() {
    val list = AliDriveService.findByDeviceRoom(Status.ON)
    for (aliDriveEntity in list) {
        LogService.log(aliDriveEntity, LogType.AliDriveDeviceRoom) {
            delay(3000)
            show = AliDriveLogic.finishDeviceRoom(aliDriveEntity)
        }
    }
}

@Cron("05:51:32")
suspend fun receiveDeviceRoom() {
    val list = AliDriveService.findByDeviceRoom(Status.ON)
    for (aliDriveEntity in list) {
        delay(3000)
        val deviceRoom = AliDriveLogic.deviceRoom(aliDriveEntity).stream().limit(5).toList()
        for (aliDriveDeviceRoom in deviceRoom) {
            if (aliDriveDeviceRoom.canCollectEnergy) {
                AliDriveLogic.receiveDeviceRoom(aliDriveEntity, aliDriveDeviceRoom.id)
            }
        }
    }
}

@Cron("03:23:01")
suspend fun finishCard() {
    val now = LocalDate.now()
    val dayOfWeek = now.dayOfWeek
    if (dayOfWeek != DayOfWeek.MONDAY) return
    val list = AliDriveService.findByCard(Status.ON)
    for (aliDriveEntity in list) {
        delay(3000)
        kotlin.runCatching {
            AliDriveLogic.finishCard(aliDriveEntity)
        }.onFailure {
            telegramBot.sendTextMessage(aliDriveEntity.tgId,
                "#自动签到失败提醒\n阿里云盘完成补签卡任务失败，\n${it.message}")
        }
    }
}

@Cron("03:43:01")
suspend fun receiveCard() {
    val now = LocalDate.now()
    val dayOfWeek = now.dayOfWeek
    if (dayOfWeek != DayOfWeek.MONDAY) return
    val list = AliDriveService.findByCard(Status.ON)
    for (aliDriveEntity in list) {
        delay(3000)
        LogService.log(aliDriveEntity, LogType.AliDriveCard) {
            AliDriveLogic.receiveCard(aliDriveEntity)
        }
    }
}

@Cron("01:21:10")
suspend fun clearRubbish() {
    val list = AliDriveService.findAll().filter { it.uploads.isNotEmpty() }
    for (aliDriveEntity in list) {
        delay(3000)
        runCatching {
            AliDriveLogic.batchDeleteFile(aliDriveEntity, aliDriveEntity.uploads
                .map { AliDriveBatch.DeleteFileBody(it.driveId.toString(), it.fileId) })
            aliDriveEntity.uploads.clear()
            AliDriveService.save(aliDriveEntity)
        }
    }
}