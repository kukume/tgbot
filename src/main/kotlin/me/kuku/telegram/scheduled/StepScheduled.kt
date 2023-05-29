package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.LeXinStepLogic
import me.kuku.telegram.logic.XiaomiStepLogic
import me.kuku.utils.MyUtils
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class StepScheduled(
    private val stepService: StepService,
    private val logService: LogService
) {

    @Scheduled(cron = "0 12 5 * * ?")
    suspend fun ss() {
        val list = stepService.findByAuto()
        for (stepEntity in list) {
            val logEntity = LogEntity().also {
                it.tgId = stepEntity.tgId
                it.type = LogType.Step
            }
            var step = stepEntity.step
            if (stepEntity.offset == Status.ON) {
                step = MyUtils.randomInt(step - 1000, step + 1000)
            }
            kotlin.runCatching {
                XiaomiStepLogic.modifyStepCount(stepEntity, step)
                LeXinStepLogic.modifyStepCount(stepEntity, step)
                logEntity.text = "成功"
            }.onFailure {
                logEntity.text = "失败"
                logEntity.errReason = it.message ?: "未知异常原因"
                logEntity.sendFailMessage(it.message)
            }
            logService.save(logEntity)
            delay(3000)
        }
    }

}
