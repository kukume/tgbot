package me.kuku.telegram.scheduled

import kotlinx.coroutines.delay
import me.kuku.telegram.config.Cron
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.LeXinStepLogic
import me.kuku.telegram.logic.XiaomiStepLogic
import me.kuku.utils.MyUtils

@Cron("05:12:00")
suspend fun stepModify() {
    val list = StepService.findByAuto()
    for (stepEntity in list) {
        var step = stepEntity.step
        if (stepEntity.offset == Status.ON) {
            step = MyUtils.randomInt(step - 1000, step + 1000)
        }
        LogService.log(stepEntity, LogType.Step) {
            XiaomiStepLogic.modifyStepCount(stepEntity, step)
            LeXinStepLogic.modifyStepCount(stepEntity, step)
        }
        delay(3000)
    }
}