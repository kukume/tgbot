package me.kuku.telegram.scheduled

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendMessage
import kotlinx.coroutines.delay
import me.kuku.telegram.entity.BuffService
import me.kuku.telegram.entity.BuffType
import me.kuku.telegram.logic.Accessory
import me.kuku.telegram.logic.BuffLogic
import me.kuku.utils.DateTimeFormatterUtils
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class BuffScheduled(
    private val buffService: BuffService,
    private val telegramBot: TelegramBot
) {

    private val pushCache = mutableMapOf<Long, Accessory>()


    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    suspend fun buff() {
        val buffEntityList = buffService.findAll().filter { it.monitors.isNotEmpty() }
        for (buffEntity in buffEntityList) {
            for (monitor in buffEntity.monitors.filter { it.type == BuffType.Buy }) {
                val paintWearInterval = monitor.paintWearInterval
                if ((paintWearInterval.min == 0.0 && paintWearInterval.max == 0.0) || monitor.maxPrice == 0.0) continue
                val accessoryList = try {
                    BuffLogic.sell(buffEntity, monitor.goodsId, paintWearInterval.min, paintWearInterval.max)
                } catch (e: Exception) {
                    continue
                }
                for (accessory in accessoryList) {
                    if (accessory.paintWear >= paintWearInterval.min && accessory.paintWear <= paintWearInterval.max &&
                            accessory.price <= monitor.maxPrice) {
                        kotlin.runCatching {
                            BuffLogic.buy(buffEntity, accessory.sellId, accessory.goodsId, accessory.price, monitor.payMethod)
                            telegramBot.execute(SendMessage(buffEntity.tgId, """
                                #网易buff提醒
                                您已下订单或者购买：${accessory.name}，磨损：${accessory.paintWear}，价格：${accessory.price}，请尽快付款购买或者发送交易报价
                            """.trimIndent()))
                        }.onFailure {
                            telegramBot.execute(SendMessage(buffEntity.tgId, """
                                #网易buff提醒
                                饰品：${accessory.name}，磨损：${accessory.paintWear}，价格：${accessory.price}，购买失败，如需要请手动购买
                            """.trimIndent()))
                        }
                    }
                }
            }
        }
    }

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    suspend fun monitor() {
        val buffEntityList = buffService.findAll().filter { it.monitors.isNotEmpty() }
        for (buffEntity in buffEntityList) {
            for (monitor in buffEntity.monitors.filter { it.type == BuffType.Push }) {
                val paintWearInterval = monitor.paintWearInterval
                delay(3000)
                val list = BuffLogic.sell(buffEntity, monitor.goodsId, paintWearInterval.min(),
                    paintWearInterval.max())
                if (list.isNotEmpty()) {
                    val accessory = list[0]
                    val saveAccessory = pushCache[buffEntity.tgId]
                    if (saveAccessory == null || saveAccessory != accessory) {
                        pushCache[buffEntity.tgId] = accessory
                        telegramBot.execute(SendMessage(buffEntity.tgId, """
                            #网易Buff饰品价格推送
                            现在时间是${DateTimeFormatterUtils.formatNow("yyyy-MM-dd HH:mm:ss")}
                            饰品：${accessory.name}
                            磨损度：${accessory.paintWear}
                            价格：${accessory.price}
                            描述：${accessory.description}
                        """.trimIndent()))
                    } else continue
                }
            }
        }
    }


}
