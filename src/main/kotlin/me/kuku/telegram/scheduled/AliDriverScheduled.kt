package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.config.TelegramBot
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.AliDriverLogic
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class AliDriverScheduled(
    private val aliDriverService: AliDriverService,
    private val telegramBot: TelegramBot,
    private val logService: LogService
) {

    @Scheduled(cron = "13 9 4 * * ?")
    suspend fun sign() {
        val list = aliDriverService.findBySign(Status.ON)
        for (aliDriverEntity in list) {
            val logEntity = LogEntity().apply {
                tgId = aliDriverEntity.tgId
                type = LogType.AliDriver
            }
            kotlin.runCatching {
                delay(3000)
                AliDriverLogic.sign(aliDriverEntity)
                if (aliDriverEntity.receive == Status.ON) {
                    AliDriverLogic.receive(aliDriverEntity)
                }
            }.onFailure {
                logEntity.text = "失败"
                logEntity.sendFailMessage(telegramBot)
            }
            logService.save(logEntity)
        }
    }

}
