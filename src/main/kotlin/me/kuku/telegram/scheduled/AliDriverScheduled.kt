package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.AliDriverLogic
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class AliDriverScheduled(
    private val aliDriverService: AliDriverService,
    private val logService: LogService
) {

    @Scheduled(cron = "13 9 4 * * ?")
    suspend fun sign() {
        val list = aliDriverService.findBySign(Status.ON)
        for (aliDriverEntity in list) {
            logService.log(aliDriverEntity.tgId, LogType.AliDriver) {
                delay(3000)
                AliDriverLogic.sign(aliDriverEntity)
                if (aliDriverEntity.receive == Status.ON) {
                    AliDriverLogic.receive(aliDriverEntity)
                }
            }
        }
    }

    @Scheduled(cron = "41 4 2 * * ?")
    suspend fun pk() {
        val list = aliDriverService.findByJoinTeam(Status.ON)
        for (aliDriverEntity in list) {
            kotlin.runCatching {
                val team = AliDriverLogic.queryTeam(aliDriverEntity)
                if (team.joinTeam.isEmpty()) {
                    AliDriverLogic.joinTeam(aliDriverEntity, team.id)
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
            val list = aliDriverService.findBySign(Status.ON)
            for (aliDriverEntity in list) {
                val signList = AliDriverLogic.sign(aliDriverEntity)
                for (signInLog in signList.signInLogs) {
                    if (!signInLog.isReward) {
                        AliDriverLogic.receive(aliDriverEntity, signInLog.day)
                    }
                }
            }
        }
    }

}
