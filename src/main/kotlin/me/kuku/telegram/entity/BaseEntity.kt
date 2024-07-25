package me.kuku.telegram.entity

import com.fasterxml.jackson.annotation.JsonFormat
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import me.kuku.telegram.context.AbilityContext
import me.kuku.telegram.context.TelegramContext
import java.time.LocalDateTime
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

open class BaseEntity {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    var createTime: LocalDateTime = LocalDateTime.now()
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    var updateTime: LocalDateTime = LocalDateTime.now()
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

suspend fun <T: Any> MongoCollection<T>.save(entity: T): T {
    val find = entity::class.memberProperties.find { it.name == "id" } as? KProperty1<T, *> ?: error("")
    this.replaceOne(Filters.eq("_id", find.get(entity)), entity, ReplaceOptions().upsert(true))
    return entity
}

//suspend fun Repository<out BaseEntity, *>.findEnableEntityByTgId(tgId: Long): BaseEntity? {
//    val clazz = AopProxyUtils.proxiedUserInterfaces(this)[0].kotlin
//    val name = tgId.tgName()
//    val function = clazz.functions.find { it.name == "findByTgIdAndTgName" }
//        ?: error("当前身份是${name ?: "主身份"}，但是该功能没有提供多账号查询函数")
//    val result = function.callSuspend(this, tgId, name)
//    return if (result is Flux<*>) {
//        result.collectList().awaitFirst().firstOrNull() as? BaseEntity
//    } else {
//        result as? BaseEntity
//    }
//}
//
//suspend fun Repository<out BaseEntity, *>.deleteEnableEntityByTgId(tgId: Long) {
//    val clazz = AopProxyUtils.proxiedUserInterfaces(this)[0].kotlin
//    val name = tgId.tgName()
//    val function = clazz.functions.find { it.name == "deleteByTgIdAndTgName" }
//        ?: error("当前身份是${name ?: "主身份"}，但是该功能没有提供多账号查询函数")
//    function.callSuspend(this, tgId, name)
//}
