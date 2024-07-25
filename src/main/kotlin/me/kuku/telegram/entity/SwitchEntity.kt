package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import me.kuku.telegram.mongoDatabase
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

val switchCollection = mongoDatabase.getCollection<SwitchEntity>("switch")

class SwitchEntity {
    @BsonId
    var id: ObjectId? = null
    var name: String = ""
    var tgId: Long = 0
    var status: Status = Status.OFF
}

object SwitchService {

    suspend fun findByTgId(tgId: Long) = switchCollection.find(eq("tgId", tgId)).toList()

    suspend fun findByTgIdAndName(tgId: Long, name: String) =
        switchCollection.find(and(eq("tgId", tgId), eq("name", name))).toList()

    suspend fun save(switchEntity: SwitchEntity) = switchCollection.save(switchEntity)

    suspend fun delete(switchEntity: SwitchEntity) = switchCollection.deleteOne(eq(switchEntity.id))

    suspend fun deleteById(id: ObjectId) = switchCollection.deleteOne(eq(id))

    suspend fun findById(id: ObjectId) = switchCollection.find(eq(id)).firstOrNull()

    suspend fun findByTgIdAndStatus(tgId: Long, status: Status) =
        switchCollection.find(and(eq("tgId", tgId), eq("status", status))).toList()

    suspend fun editName(tgId: Long, oldName: String, name: String) {
        /*for (clazz in clazzList) {
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
        }*/
    }

    suspend fun deleteName(tgId: Long, name: String) {
        /*for (clazz in clazzList) {
            val function = clazz.declaredFunctions.find { it.name == "deleteByTgIdAndTgName" } ?: continue
            val instance = SpringUtils.getBean(clazz)
            function.callSuspend(instance, tgId, name)
        }*/
    }

}
