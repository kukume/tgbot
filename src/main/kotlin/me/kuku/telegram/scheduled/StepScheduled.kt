package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.entity.StepService
import me.kuku.telegram.logic.LeXinStepLogic
import me.kuku.telegram.logic.XiaomiStepLogic
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class StepScheduled(
    private val stepService: StepService
) {

    @Scheduled(cron = "0 12 5 * * ?")
    suspend fun ss() {
        val list = stepService.findByAuto()
        for (stepEntity in list) {
            val step = stepEntity.step
            XiaomiStepLogic.modifyStepCount(stepEntity, step)
            LeXinStepLogic.modifyStepCount(stepEntity, step)
            delay(3000)
        }
    }

}