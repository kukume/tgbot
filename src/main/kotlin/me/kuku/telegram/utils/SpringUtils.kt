package me.kuku.telegram.utils

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
@Lazy(false)
class SpringUtils: ApplicationContextAware {

    companion object {

        lateinit var applicationContext: ApplicationContext

        inline fun <reified T: Any> getBean(name: String): T {
            return applicationContext.getBean(name) as T
        }

        inline fun <reified T: Any> getBean(): T = applicationContext.getBean(T::class.java)

        fun <T : Any> getBean(clazz: KClass<T>): T = applicationContext.getBean(clazz.java)

    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        SpringUtils.applicationContext = applicationContext
    }
}
