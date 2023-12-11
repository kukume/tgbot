package me.kuku.telegram.scheduled

import com.pengrad.telegrambot.TelegramBot
import kotlinx.coroutines.delay
import me.kuku.telegram.context.sendTextMessage
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.AliDriveBatch
import me.kuku.telegram.logic.AliDriveLogic
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.LocalDate

@Component
class AliDriveScheduled(
    private val aliDriveLogic: AliDriveLogic,
    private val aliDriveService: AliDriveService,
    private val logService: LogService,
    private val telegramBot: TelegramBot
) {

    @Scheduled(cron = "13 9 2 * * ?")
    suspend fun sign() {
        val list = aliDriveService.findBySign(Status.ON)
        for (aliDriveEntity in list) {
            logService.log(aliDriveEntity.tgId, LogType.AliDrive) {
                delay(3000)
                aliDriveLogic.sign(aliDriveEntity)
                if (aliDriveEntity.receive == Status.ON) {
                    show = aliDriveLogic.receive(aliDriveEntity)
                }
            }
        }
    }

    @Scheduled(cron = "10 4 5 * * ?")
    suspend fun lastDayReceive() {
        val now = LocalDate.now()
        val nextMonth = now.plusMonths(1)
        val lastDay = LocalDate.of(nextMonth.year, nextMonth.month, 1).minusDays(1)
        if (now.dayOfMonth == lastDay.dayOfMonth) {
            val list = aliDriveService.findBySign(Status.ON)
            for (aliDriveEntity in list) {
                logService.log(aliDriveEntity.tgId, LogType.ALiDriveReceive) {
                    val signList = aliDriveLogic.sign(aliDriveEntity)
                    for (signInLog in signList.signInLogs) {
                        if (!signInLog.isReward) {
                            delay(3000)
                            kotlin.runCatching {
                                aliDriveLogic.receive(aliDriveEntity, signInLog.day)
                            }
                        }
                    }
                }
            }
            val taskList = aliDriveService.findByTask(Status.ON)
            for (aliDriveEntity in taskList) {
                logService.log(aliDriveEntity.tgId, LogType.ALiDriveReceiveTask) {
                    val signList = aliDriveLogic.signInList(aliDriveEntity)
                    for (signInInfo in signList.signInInfos) {
                        signInInfo.rewards.lastOrNull()?.let { reward ->
                            if (reward.status == "finished") {
                                delay(3000)
                                kotlin.runCatching {
                                    aliDriveLogic.receiveTask(aliDriveEntity, signInInfo.day)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Scheduled(cron = "43 1 3 * * ?")
    suspend fun task() {
        val list = aliDriveService.findByTask(Status.ON)
        for (aliDriveEntity in list) {
            logService.log(aliDriveEntity.tgId, LogType.AliDriveTask) {
                aliDriveLogic.finishTask(aliDriveEntity)
                delay(3000)
            }

        }
    }

    @Scheduled(cron = "43 30 4 * * ?")
    suspend fun receiveTodayTask() {
        val list = aliDriveService.findByTask(Status.ON)
        for (aliDriveEntity in list) {
            kotlin.runCatching {
                delay(3000)
                var signInInfo = aliDriveLogic.signInInfo(aliDriveEntity)
                var reward = signInInfo.rewards[1]
                if (reward.remind == "开启「自动同步电脑文件夹至少一小时」") {
                    signInInfo = aliDriveLogic.signInInfo(aliDriveEntity, aliDriveLogic.backupDesktopDevice(aliDriveEntity))
                    reward = signInInfo.rewards[1]
                }
                if (reward.status !in listOf("finished", "verification")) {
                    error("阿里云盘任务完成失败，任务名称：${reward.remind}")
                }
            }.onSuccess {
                if (aliDriveEntity.receiveTask == Status.ON) {
                    logService.log(aliDriveEntity.tgId, LogType.AliDriveReceiveTaskToday) {
                        aliDriveLogic.signInList(aliDriveEntity)
                            show = aliDriveLogic.receiveTask(aliDriveEntity)
                    }
                }
            }.onFailure {
                telegramBot.sendTextMessage(aliDriveEntity.tgId,
                    "#阿里云盘任务检测\n${it.message}")
            }
        }
    }

    @Scheduled(cron = "32 50 4 * * ?")
    suspend fun finishDeviceRoom() {
        val list = aliDriveService.findByDeviceRoom(Status.ON)
        for (aliDriveEntity in list) {
            logService.log(aliDriveEntity.tgId, LogType.AliDriveDeviceRoom) {
                delay(3000)
                show = aliDriveLogic.finishDeviceRoom(aliDriveEntity)
            }
        }
    }

    @Scheduled(cron = "32 51 5 * * ?")
    suspend fun receiveDeviceRoom() {
        val list = aliDriveService.findByDeviceRoom(Status.ON)
        for (aliDriveEntity in list) {
            delay(3000)
            val deviceRoom = aliDriveLogic.deviceRoom(aliDriveEntity).stream().limit(5).toList()
            for (aliDriveDeviceRoom in deviceRoom) {
                if (aliDriveDeviceRoom.canCollectEnergy) {
                    aliDriveLogic.receiveDeviceRoom(aliDriveEntity, aliDriveDeviceRoom.id)
                }
            }
        }
    }

    @Scheduled(cron = "1 23 3 * * ?")
    suspend fun finishCard() {
        val now = LocalDate.now()
        val dayOfWeek = now.dayOfWeek
        if (dayOfWeek != DayOfWeek.MONDAY) return
        val list = aliDriveService.findByCard(Status.ON)
        for (aliDriveEntity in list) {
            delay(3000)
            kotlin.runCatching {
                aliDriveLogic.finishCard(aliDriveEntity)
            }.onFailure {
                telegramBot.sendTextMessage(aliDriveEntity.tgId,
                    "#自动签到失败提醒\n阿里云盘完成补签卡任务失败，\n${it.message}")
            }
        }
    }

    @Scheduled(cron = "1 43 3 * * ?")
    suspend fun receiveCard() {
        val now = LocalDate.now()
        val dayOfWeek = now.dayOfWeek
        if (dayOfWeek != DayOfWeek.MONDAY) return
        val list = aliDriveService.findByCard(Status.ON)
        for (aliDriveEntity in list) {
            delay(3000)
            logService.log(aliDriveEntity.tgId, LogType.AliDriveCard) {
                aliDriveLogic.receiveCard(aliDriveEntity)
            }
        }
    }

    @Scheduled(cron = "10 21 1 * * ?")
    suspend fun clearRubbish() {
        val list = aliDriveService.findAll().filter { it.uploads.isNotEmpty() }
        for (aliDriveEntity in list) {
            delay(3000)
            runCatching {
                aliDriveLogic.batchDeleteFile(aliDriveEntity, aliDriveEntity.uploads
                    .map { AliDriveBatch.DeleteFileBody(it.driveId.toString(), it.fileId) })
                aliDriveEntity.uploads.clear()
                aliDriveService.save(aliDriveEntity)
            }
        }
    }

}
