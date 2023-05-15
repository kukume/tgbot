package me.kuku.telegram.scheduled

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendVideo
import me.kuku.telegram.entity.ConfigService
import me.kuku.telegram.entity.Status
import me.kuku.telegram.logic.ToolLogic
import me.kuku.utils.DateTimeFormatterUtils
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ConfigScheduled(
    private val toolLogic: ToolLogic,
    private val configService: ConfigService,
    private val telegramBot: TelegramBot
) {

    @Scheduled(cron = "0 0 20 * * ?")
    suspend fun positiveEnergyPush() {
        val entityList = configService.findByPositiveEnergy(Status.ON)
        if (entityList.isEmpty()) return
        val time = DateTimeFormatterUtils.formatNow("yyyyMMdd")
        val file = toolLogic.positiveEnergy(time)
        try {
            for (configEntity in entityList) {
                val sendVideo =
                    SendVideo(configEntity.tgId.toString(), file).caption("#新闻联播")
                telegramBot.execute(sendVideo)
            }
        } finally {
            file.delete()
        }
    }

}
