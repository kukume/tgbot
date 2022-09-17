package me.kuku.telegram.config

import kotlinx.coroutines.runBlocking
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Role
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor
import org.springframework.scheduling.config.TaskManagementConfigUtils
import org.springframework.scheduling.support.ScheduledMethodRunnable
import org.springframework.stereotype.Component
import org.springframework.util.Assert
import org.springframework.util.ReflectionUtils
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.UndeclaredThrowableException
import kotlin.coroutines.Continuation
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.kotlinFunction

@Component
class ScheduledSuspendConfig {

    @Bean(name = [TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME])
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    fun scheduledAnnotationBeanPostProcessor(): ScheduledAnnotationBeanPostProcessor {
        return MyScheduledAnnotationBeanPostProcessor()
    }

}


class MyScheduledAnnotationBeanPostProcessor: ScheduledAnnotationBeanPostProcessor() {

    override fun createRunnable(target: Any, method: Method): Runnable {
        val parameters = method.parameters
        Assert.isTrue(
            parameters.isEmpty() || ((parameters.size == 1) && (parameters[0].type == Continuation::class.java)),
            "Only no-arg methods may be annotated with @Scheduled");
        val invocableMethod = AopUtils.selectInvocableMethod(method, target.javaClass)
        return MyScheduledMethodRunnable(target, invocableMethod)
    }

}

class MyScheduledMethodRunnable(target: Any, method: Method): ScheduledMethodRunnable(target, method) {
    override fun run() {
        try {
            ReflectionUtils.makeAccessible(method)
            runBlocking {
                method.kotlinFunction?.callSuspend(target)
            }
        } catch (ex: InvocationTargetException) {
            ReflectionUtils.rethrowRuntimeException(ex.targetException)
        } catch (ex: IllegalAccessException) {
            throw UndeclaredThrowableException(ex)
        }
    }
}