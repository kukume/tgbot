package me.kuku.telegram.scheduled

import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.SmZdmLogic
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SmZdmScheduled(
    private val smZdmService: SmZdmService,
    private val configService: ConfigService,
    private val smZdmLogic: SmZdmLogic,
    private val logService: LogService
) {

    @Scheduled(cron = "42 32 6 * * ?")
    suspend fun sign() {
        val entityList = smZdmService.findBySign(Status.ON)
        for (smZdmEntity in entityList) {
            val logEntity = LogEntity().also {
                it.tgId = smZdmEntity.tgId
                it.type = LogType.SmZdm
            }
            kotlin.runCatching {
                smZdmLogic.webSign(smZdmEntity, configService.findByTgId(smZdmEntity.tgId)?.rrOcrKey())
                smZdmLogic.appSign(smZdmEntity)
                logEntity.text = "成功"
            }.onFailure {
                logEntity.text = "失败"
                logEntity.errReason = it.message ?: "未知异常原因"
                logEntity.sendFailMessage(it.message)
            }
            logService.save(logEntity)
        }
    }


}
