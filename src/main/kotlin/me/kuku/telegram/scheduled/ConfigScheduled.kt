package me.kuku.telegram.scheduled

import com.fasterxml.jackson.annotation.JsonProperty
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.request.SendVideo
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import me.kuku.telegram.entity.ConfigService
import me.kuku.telegram.entity.Status
import me.kuku.telegram.logic.ToolLogic
import me.kuku.utils.DateTimeFormatterUtils
import me.kuku.utils.client
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.net.ConnectException
import java.util.concurrent.TimeUnit

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

    private var xianBaoId = 0

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    suspend fun xianBaoPush() {
        val list = try {
            client.get("http://new.xianbao.fun/plus/json/push.json?230406").body<List<XianBao>>()
        } catch (e: Exception) {
            return
        }
        if (list.isEmpty()) return
        val newList = mutableListOf<XianBao>()
        if (xianBaoId != 0) {
            for (xianBao in list) {
                if (xianBao.id <= xianBaoId) break
                newList.add(xianBao)
            }
        }
        xianBaoId = list[0].id
        val pushList = configService.findByXianBaoPush(Status.ON)
        for (xianBao in newList) {
            delay(3000)
            for (configEntity in pushList) {
                val str = """
                    #线报酷推送
                    标题：${xianBao.title}
                    时间：${xianBao.datetime}
                    源链接：${xianBao.yuanUrl}
                    线报酷链接：${xianBao.urlIncludeDomain()}
                """.trimIndent()
                val sendMessage = SendMessage(configEntity.tgId, str)
                telegramBot.execute(sendMessage)
            }
        }
    }

}


class XianBao {
    var id: Int = 0
    var title: String = ""
    var content: String = ""
    var datetime: String = ""
    @JsonProperty("shorttime")
    var shortTime: String = ""
    @JsonProperty("shijianchuo")
    var time: String = ""
    @JsonProperty("cateid")
    var cateId: String = ""
    @JsonProperty("catename")
    var cateName: String = ""
    var comments: String = ""
    @JsonProperty("louzhu")
    var louZhu: String = ""
    @JsonProperty("louzhuregtime")
    var regTime: String? = null
    var url: String = ""
    @JsonProperty("yuanurl")
    var yuanUrl: String = ""

    fun urlIncludeDomain() = "http://new.xianbao.fun/$url"


}
