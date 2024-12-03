package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.LeXinStepLogic
import me.kuku.telegram.logic.XiaomiStepLogic
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class StepScheduled(
    private val stepService: StepService,
    private val logService: LogService
) {

    @Scheduled(cron = "0 12 5 * * ?")
    suspend fun ss() {
        val list = stepService.findByAuto()
        for (stepEntity in list) {
            var step = stepEntity.step
            if (stepEntity.offset == Status.ON) {
                step = Random.nextInt(step - 1000, step + 1000)
            }
            logService.log(stepEntity, LogType.Step) {
                XiaomiStepLogic.modifyStepCount(stepEntity, step)
                LeXinStepLogic.modifyStepCount(stepEntity, step)
            }
            delay(3000)
        }
    }

}
