package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.AliDriveLogic
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class AliDriveScheduled(
    private val aliDriveLogic: AliDriveLogic,
    private val aliDriveService: AliDriveService,
    private val logService: LogService
) {

    @Scheduled(cron = "13 9 2 * * ?")
    suspend fun sign() {
        val list = aliDriveService.findBySign(Status.ON)
        for (aliDriveEntity in list) {
            logService.log(aliDriveEntity.tgId, LogType.AliDrive) {
                delay(3000)
                aliDriveLogic.sign(aliDriveEntity)
                if (aliDriveEntity.receive == Status.ON) {
                    text = aliDriveLogic.receive(aliDriveEntity)
                }
            }
        }
    }

    @Scheduled(cron = "41 4 2 * * ?")
    suspend fun pk() {
        val list = aliDriveService.findByJoinTeam(Status.ON)
        for (aliDriveEntity in list) {
            kotlin.runCatching {
                val team = aliDriveLogic.queryTeam(aliDriveEntity)
                if (team.joinTeam.isEmpty()) {
                    aliDriveLogic.joinTeam(aliDriveEntity, team.id)
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
                            aliDriveLogic.receive(aliDriveEntity, signInLog.day)
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
                            if (reward.status == "verification") {
                                delay(3000)
                                aliDriveLogic.receiveTask(aliDriveEntity, signInInfo.day)
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
                delay(1000 * 60)
                if (aliDriveEntity.receiveTask == Status.ON) {
                    text = aliDriveLogic.receiveTask(aliDriveEntity)
                }
            }

        }
    }

}
