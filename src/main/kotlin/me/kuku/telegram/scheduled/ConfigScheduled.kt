package me.kuku.telegram.scheduled

import me.kuku.telegram.config.TelegramBot
import me.kuku.telegram.entity.ConfigService
import me.kuku.telegram.entity.Status
import me.kuku.telegram.logic.ToolLogic
import me.kuku.utils.DateTimeFormatterUtils
import me.kuku.utils.IOUtils
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendVideo
import org.telegram.telegrambots.meta.api.objects.InputFile

@Component
class ConfigScheduled(
    private val toolLogic: ToolLogic,
    private val configService: ConfigService,
    private val telegramBot: TelegramBot
) {

    @Scheduled(cron = "0 11 19 * * ?")
    suspend fun positiveEnergyPush() {
        val entityList = configService.findByPositiveEnergy(Status.ON)
        if (entityList.isEmpty()) return
        val time = DateTimeFormatterUtils.formatNow("yyyyMMdd")
        val file = toolLogic.positiveEnergy(time)
        val iis = file.inputStream()
        try {
            for (configEntity in entityList) {
                val sendVideo =
                    SendVideo(configEntity.tgId.toString(), InputFile(iis, file.name))
                sendVideo.caption = "#新闻联播"
                telegramBot.execute(sendVideo)
            }
        } finally {
            IOUtils.close(iis)
            file.delete()
        }
    }

}
