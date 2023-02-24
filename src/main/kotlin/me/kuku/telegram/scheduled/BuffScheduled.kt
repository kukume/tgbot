package me.kuku.telegram.scheduled

import me.kuku.telegram.config.TelegramBot
import me.kuku.telegram.entity.BuffService
import me.kuku.telegram.logic.BuffLogic
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class BuffScheduled(
    private val buffService: BuffService,
    private val telegramBot: TelegramBot
) {


    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    suspend fun buff() {
        val buffEntityList = buffService.findAll().filter { it.monitors.isNotEmpty() }
        for (buffEntity in buffEntityList) {
            for (monitor in buffEntity.monitors) {
                val paintWearInterval = monitor.paintWearInterval
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
                            telegramBot.silent().send("""
                                #网易buff提醒
                                您已下订单或者购买：${accessory.name}，磨损：${accessory.paintWear}，价格：${accessory.price}，请尽快付款购买或者发送交易报价
                            """.trimIndent(), buffEntity.tgId)
                        }.onFailure {
                            telegramBot.silent().send("""
                                #网易buff提醒
                                饰品：${accessory.name}，磨损：${accessory.paintWear}，价格：${accessory.price}，购买失败，如需要请手动购买
                            """.trimIndent(), buffEntity.tgId)
                        }
                    }
                }
            }
        }
    }


}
