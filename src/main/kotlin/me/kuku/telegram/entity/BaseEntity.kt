package me.kuku.telegram.entity

import com.fasterxml.jackson.annotation.JsonFormat
import kotlinx.coroutines.reactive.awaitFirst
import me.kuku.telegram.context.AbilityContext
import me.kuku.telegram.context.TelegramContext
import me.kuku.telegram.utils.SpringUtils
import org.springframework.aop.framework.AopProxyUtils
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.Repository
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import reactor.core.publisher.Flux
import java.time.LocalDateTime
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.functions

open class BaseEntity {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @CreatedDate
    var createTime: LocalDateTime = LocalDateTime.now()
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @LastModifiedDate
    var updateTime: LocalDateTime = LocalDateTime.now()
    @Indexed(name = "tgId")
    open var tgId: Long = 0
    var tgName: String? = null

    context(TelegramContext)
    suspend inline fun <reified T: BaseEntity> init(): T{
        this@BaseEntity.tgId = this@TelegramContext.tgId
        this@BaseEntity.tgName = tgId.tgName()
        return this as T
    }

    context(AbilityContext)
    suspend inline fun <reified T: BaseEntity> init(): T{
        this@BaseEntity.tgId = this@AbilityContext.tgId
        this@BaseEntity.tgName = tgId.tgName()
        return this as T
    }

}

enum class Status {
    OFF, ON;

    fun str() : String{
        return if (this.name == "ON") "√" else "×"
    }

    fun reverse(): Status {
        return if (this.name == "ON") OFF else ON
    }

    operator fun not(): Status {
        return this.reverse()
    }

    override fun toString(): String {
        return str()
    }
}

private val switchRepository by lazy {
    SpringUtils.getBean<SwitchRepository>()
}

suspend fun Long.tgName(): String? {
    val findList = switchRepository.findByTgIdAndStatus(this, Status.ON)
    return  if (findList.isEmpty()) null
    else findList.first().name
}

suspend fun Repository<out BaseEntity, *>.findEnableEntityByTgId(tgId: Long): BaseEntity? {
    val clazz = AopProxyUtils.proxiedUserInterfaces(this)[0].kotlin
    val name = tgId.tgName()
    val function = clazz.functions.find { it.name == "findByTgIdAndTgName" }
        ?: error("当前身份是${name ?: "主身份"}，但是该功能没有提供多账号查询函数")
    val result = function.callSuspend(this, tgId, name)
    return if (result is Flux<*>) {
        result.collectList().awaitFirst().firstOrNull() as? BaseEntity
    } else {
        result as? BaseEntity
    }
}

suspend fun Repository<out BaseEntity, *>.deleteEnableEntityByTgId(tgId: Long) {
    val clazz = AopProxyUtils.proxiedUserInterfaces(this)[0].kotlin
    val name = tgId.tgName()
    val function = clazz.functions.find { it.name == "deleteByTgIdAndTgName" }
        ?: error("当前身份是${name ?: "主身份"}，但是该功能没有提供多账号查询函数")
    function.callSuspend(this, tgId, name)
}

@NoRepositoryBean
interface CoroutineCrudMultipleRepository<T, ID>: CoroutineCrudRepository<T, ID> {

    fun findByTgIdAndTgName(tgId: Long, tgName: String?): T?
    suspend fun deleteByTgIdAndTgName(tgId: Long, tgName: String?)

}