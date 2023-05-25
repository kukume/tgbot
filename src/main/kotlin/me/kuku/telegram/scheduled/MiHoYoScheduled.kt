package me.kuku.telegram.scheduled

import com.pengrad.telegrambot.TelegramBot
import kotlinx.coroutines.delay
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.MiHoYoLogic
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MiHoYoScheduled(
    private val miHoYoService: MiHoYoService,
    private val logService: LogService,
    private val telegramBot: TelegramBot
) {


    @Scheduled(cron = "0 13 5 * * ?")
    suspend fun genShinSign() {
        val list = miHoYoService.findBySign(Status.ON)
        for (miHoYoEntity in list) {
            val logEntity = LogEntity().also {
                it.tgId = miHoYoEntity.tgId
                it.type = LogType.GenShin
            }
            kotlin.runCatching {
                MiHoYoLogic.sign(miHoYoEntity)
                logEntity.text = "成功"
            }.onFailure {
                logEntity.text = "失败"
                logEntity.errReason = it.message ?: "未知异常原因"
                logEntity.sendFailMessage(telegramBot, it.message)
            }
            logService.save(logEntity)
            delay(3000)
        }
    }


}
