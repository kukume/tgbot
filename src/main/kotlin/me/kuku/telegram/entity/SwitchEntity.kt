package me.kuku.telegram.entity

import me.kuku.telegram.utils.SpringUtils
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.functions

@Document("switch")
class SwitchEntity {
    @Id
    var id: String? = null
    var name: String = ""
    var tgId: Long = 0
    var status: Status = Status.OFF
}


interface SwitchRepository: CoroutineCrudRepository<SwitchEntity, String> {
    suspend fun findByTgId(tgId: Long): List<SwitchEntity>

    suspend fun findByTgIdAndName(tgId: Long, name: String): List<SwitchEntity>

    suspend fun findByTgIdAndStatus(tgId: Long, status: Status): List<SwitchEntity>

}

@Service
class SwitchService(
    private val switchRepository: SwitchRepository
) {

    suspend fun findByTgId(tgId: Long) = switchRepository.findByTgId(tgId)

    suspend fun findByTgIdAndName(tgId: Long, name: String) = switchRepository.findByTgIdAndName(tgId, name)

    suspend fun save(switchEntity: SwitchEntity) = switchRepository.save(switchEntity)

    suspend fun delete(switchEntity: SwitchEntity) = switchRepository.delete(switchEntity)

    suspend fun deleteById(id: String) = switchRepository.deleteById(id)

    suspend fun findById(id: String) = switchRepository.findById(id)

    suspend fun findByTgIdAndStatus(tgId: Long, status: Status) = switchRepository.findByTgIdAndStatus(tgId, status)

    suspend fun editName(tgId: Long, oldName: String, name: String) {
        for (clazz in clazzList) {
            val function = clazz.declaredFunctions.find { it.name == "findByTgIdAndTgName" } ?: continue
            val instance = SpringUtils.getBean(clazz)
            val any = function.callSuspend(instance, tgId, oldName)
            if (any != null) {
                if (any is List<*>) {
                    any.forEach { single ->
                        val method = single!!::class.java.getMethod("setTgName", String::class.java)
                        method.invoke(single, name)
                        val saveFunction = clazz.functions.find { it.name == "save" } ?: return@forEach
                        saveFunction.callSuspend(instance, single)
                    }
                } else {
                    val method = any::class.java.getMethod("setTgName", String::class.java)
                    method.invoke(any, name)
                    val saveFunction = clazz.functions.find { it.name == "save" } ?: continue
                    saveFunction.callSuspend(instance, any)
                }
            }
        }
    }

    suspend fun deleteName(tgId: Long, name: String) {
        for (clazz in clazzList) {
            val function = clazz.declaredFunctions.find { it.name == "deleteByTgIdAndTgName" } ?: continue
            val instance = SpringUtils.getBean(clazz)
            function.callSuspend(instance, tgId, name)
        }
    }

}

private val clazzList = mutableListOf(
    BaiduRepository::class, BiliBiliRepository::class,
    DouYuRepository::class, ECloudRepository::class,
    HostLocRepository::class, HuYaRepository::class,
    KuGouRepository::class, LeiShenRepository::class,
    LogRepository::class, MiHoYoRepository::class,
    NodeSeekRepository::class, SmZdmRepository::class, StepRepository::class,
    WeiboRepository::class)
