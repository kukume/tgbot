package me.kuku.telegram.config

import me.kuku.telegram.yamlConfig
import me.kuku.utils.JobManager
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.kotlinFunction

annotation class Cron(val value: String)

object CronConfiguration {

    fun init() {
        val cronList = yamlConfig.config("ktor.telegram").property("cron").getList()
        val list = mutableListOf<CronExecution>()
        for (singleCron in cronList) {
            val index = singleCron.lastIndexOf('.')
            val className = singleCron.substring(0, index)
            val name = singleCron.substring(index + 1)
            val clazz = Class.forName(className)
            clazz.declaredMethods.find { it.name == name }?.kotlinFunction?.let {
                (it.annotations.find { anno -> anno is Cron } as? Cron)?.let { cronAnno ->
                    list.add(CronExecution(it, cronAnno.value))
                }

            }
        }
        for (cronExecution in list) {
            val cronStr = cronExecution.cron
            if (cronStr.last() in listOf('s', 'm', 'h', 'd')) {
                JobManager.every(cronStr) {
                    cronExecution.function.callSuspend()
                }
            } else {
                JobManager.atTime(cronStr, always = true) {
                    cronExecution.function.callSuspend()
                }
            }
        }
    }


}

private data class CronExecution(val function: KFunction<*>, val cron: String)