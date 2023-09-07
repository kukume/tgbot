@file:Suppress("DuplicatedCode")

package me.kuku.telegram.utils

import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.superclasses

private val superClassCache: MutableMap<KClass<*>, Set<KClass<*>>> = mutableMapOf()

class TelegramExceptionContext<R: Throwable> (
    val throwable: R,
    val telegramContext: TelegramContext
)

class AbilityExceptionContext<R: Throwable> (
    val throwable: R,
    val abilityContext: AbilityContext
)

private typealias AbilityBody = suspend AbilityExceptionContext<out Throwable>.() -> Unit
private typealias TelegramBody = suspend TelegramExceptionContext<out Throwable>.() -> Unit

class TelegramExceptionHandler {

    val abilityExceptions = mutableMapOf<KClass<out Throwable>, MutableList<AbilityBody>>()

    val telegramExceptions = mutableMapOf<KClass<out Throwable>, MutableList<TelegramBody>>()

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T: Throwable> handler(noinline block: suspend TelegramExceptionContext<T>.() -> Unit) {
        val key = T::class
        telegramExceptions[key] = (telegramExceptions[key] ?: mutableListOf()).also { it.add(block as TelegramBody) }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T: Throwable> abilityHandler(noinline block: suspend AbilityExceptionContext<T>.() -> Unit) {
        val key = T::class
        abilityExceptions[key] = (abilityExceptions[key] ?: mutableListOf()).also { it.add(block as AbilityBody) }
    }

}

private val logger = LoggerFactory.getLogger(TelegramExceptionHandler::class.java)

suspend fun TelegramExceptionHandler.invokeHandler(telegramContext: TelegramContext, block: suspend () -> Unit) {
    kotlin.runCatching {
        block()
    }.onFailure {
        val nowThrowableClass = it::class
        val exceptions = this.telegramExceptions
        val context = TelegramExceptionContext(it, telegramContext)
        val throwableClassSet = superClassCache[nowThrowableClass] ?: superclasses(nowThrowableClass).also { set -> superClassCache[nowThrowableClass] = set }
        val newMap = exceptions.filterKeys(throwableClassSet::contains).toSortedMap { o1, o2 ->
            if (o1.isSubclassOf(o2)) -1
            else if (o2.isSubclassOf(o1)) 1
            else 0
        }
        if (newMap.isNotEmpty()) {
            val value = newMap.iterator().next().value
            for (func in value) {
                func.invoke(context)
            }
        }
        logger.error("Unexpected error occurred in telegram subscribe", it)
//        throw it
    }
}

suspend fun TelegramExceptionHandler.invokeHandler(abilityContext: AbilityContext, block: suspend () -> Unit) {
    kotlin.runCatching {
        block()
    }.onFailure {
        val nowThrowableClass = it::class
        val exceptions = this.abilityExceptions
        val context = AbilityExceptionContext(it, abilityContext)
        for (entry in exceptions) {
            val throwableClass = entry.key
            val throwableClassSet = superClassCache[nowThrowableClass] ?: superclasses(nowThrowableClass).also { set -> superClassCache[nowThrowableClass] = set }
            if (throwableClassSet.contains(throwableClass)) {
                for (func in entry.value) {
                    func.invoke(context)
                }
            }
        }
        logger.error("Unexpected error occurred in telegram subscribe", it)
//        throw it
    }
}

fun superclasses(kClass: KClass<*>?, set: MutableSet<KClass<*>> = mutableSetOf()): Set<KClass<*>> {
    kClass?.let { set.add(it) }
    val superclasses = kClass?.superclasses ?: return set
    set.addAll(superclasses)
    for (superclass in superclasses) {
        val suSuper = superclass.superclasses
        if (suSuper.isNotEmpty()) {
            suSuper.forEach { superclasses(it, set) }
        }
    }
    return set
}
