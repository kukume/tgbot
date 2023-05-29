package me.kuku.telegram.scheduled

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendMessage
import kotlinx.coroutines.delay
import me.kuku.telegram.entity.YouPinEntity
import me.kuku.telegram.entity.YouPinService
import me.kuku.telegram.logic.YouPinLogic
import me.kuku.utils.DateTimeFormatterUtils
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class YouPinScheduled(
    private val youPinService: YouPinService,
    private val telegramBot: TelegramBot
) {

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    suspend fun monitor() {
        val youPinEntityList = youPinService.findAll().filter { it.monitors.isNotEmpty() }
        for (entity in youPinEntityList) {
            for (monitor in entity.monitors.filter { it.type == YouPinEntity.Monitor.Type.Push }) {
                delay(3000)
                val market = try {
                    YouPinLogic.market(entity, monitor.templateId, 1,
                        minAbrade = monitor.minAbrade, maxAbrade = monitor.maxAbrade)
                } catch (e: Exception) {
                    continue
                }
                val list = market.commodities ?: continue
                if (list.isNotEmpty()) {
                    val commodity = list[0]
                    telegramBot.execute(
                        SendMessage(entity.tgId, """
                        #悠悠有品饰品价格推送
                        符合条件价格最便宜的
                        现在时间是${DateTimeFormatterUtils.formatNow("yyyy-MM-dd HH:mm:ss")}
                        饰品：${commodity.name}
                        磨损度：${commodity.abrade}
                        价格：${commodity.price}
                        备注：${commodity.remark}
                    """.trimIndent())
                    )
                }
                val newMarket = YouPinLogic.market(entity, monitor.templateId, 1,
                    minAbrade = monitor.minAbrade, maxAbrade = monitor.maxAbrade, listSortType = 4)
                newMarket.commodities?.getOrNull(0)?.let {
                    telegramBot.execute(
                        SendMessage(entity.tgId, """
                        #悠悠有品饰品价格推送
                        符合条件押金最便宜的
                        现在时间是${DateTimeFormatterUtils.formatNow("yyyy-MM-dd HH:mm:ss")}
                        饰品：${it.name}
                        磨损度：${it.abrade}
                        押金：${it.leaseDeposit}
                        备注：${it.remark}
                    """.trimIndent())
                    )
                }
            }
        }
    }

}
