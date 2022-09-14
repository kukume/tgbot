package me.kuku.telegram.scheduled

import me.kuku.telegram.config.TelegramBot
import me.kuku.telegram.entity.Status
import me.kuku.telegram.entity.WeiboService
import me.kuku.telegram.logic.WeiboLogic
import me.kuku.telegram.logic.WeiboPojo
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class WeiboScheduled(
    private val weiboService: WeiboService,
    private val telegramBot: TelegramBot
) {

    private val userMap = mutableMapOf<Long, Long>()

    @Scheduled(cron = "0 51 4 * * ?")
    suspend fun sign() {
        val list = weiboService.findBySign(Status.ON)
        for (weiboEntity in list) {
            WeiboLogic.superTalkSign(weiboEntity)
        }
    }

    @Scheduled(fixedDelay = 2, timeUnit = TimeUnit.MINUTES)
    suspend fun userMonitor() {
        val weiboList = weiboService.findByPush(Status.ON)
        for (weiboEntity in weiboList) {
            val tgId = weiboEntity.tgId
            val result = WeiboLogic.friendWeibo(weiboEntity)
            val list = result.data ?: continue
            val newList = mutableListOf<WeiboPojo>()
            if (userMap.containsKey(tgId)) {
                for (weiboPojo in list) {
                    if (weiboPojo.id <= userMap[tgId]!!) break
                    newList.add(weiboPojo)
                }
                for (weiboPojo in newList) {
                    telegramBot.silent().send("#微博动态推送\n有新微博了！！\n${WeiboLogic.convert(weiboPojo)}", tgId)
                }
            }
            userMap[tgId] = list[0].id
        }
    }


}