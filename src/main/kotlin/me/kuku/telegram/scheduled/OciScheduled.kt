package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.config.TelegramBot
import me.kuku.telegram.entity.OciService
import me.kuku.telegram.logic.OciLogic
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class OciScheduled(
    private val ociService: OciService,
    private val telegramBot: TelegramBot
) {

    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    suspend fun createInstance() {
        val list = ociService.findAll()
        for (ociEntity in list) {
            val deleteIndex = mutableListOf<Int>()
            val createInstanceList = ociEntity.createInstanceList
            for ((i, createInstanceCache) in createInstanceList.withIndex()) {
                kotlin.runCatching {
                    val instance = OciLogic.launchInstance(
                        ociEntity, createInstanceCache.imageId, createInstanceCache.cpu,
                        createInstanceCache.memory, createInstanceCache.volumeSize, createInstanceCache.shape,
                        createInstanceCache.rootPassword
                    )
                    delay(1000 * 15)
                    val vnic = OciLogic.vnicByInstance(ociEntity, instance)
                    val publicIp = vnic.publicIp
                    telegramBot.silent().send("""
                        #甲骨文创建机器成功
                        cpu：${createInstanceCache.cpu}
                        memory：${createInstanceCache.memory}
                        volume-size：${createInstanceCache.volumeSize}
                        ip：${publicIp}
                        password：${createInstanceCache.rootPassword}
                    """.trimIndent(), ociEntity.tgId)
                    deleteIndex.add(i)
                }
            }
            if (deleteIndex.isNotEmpty()) {
                deleteIndex.forEach { createInstanceList.removeAt(it) }
                ociService.save(ociEntity)
            }
        }
    }

}
