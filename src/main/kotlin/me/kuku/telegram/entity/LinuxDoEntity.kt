package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.kuku.telegram.mongoDatabase
import org.bson.types.ObjectId

val linuxDoCollection = mongoDatabase.getCollection<LinuxDoEntity>("linux_do")

@Serializable
class LinuxDoEntity: BaseEntity() {
    @Contextual
    @SerialName("_id")
    var id: ObjectId? = null
    var cookie: String = ""
    var sign: Status = Status.OFF
}

object LinuxDoService {

    suspend fun findByTgId(tgId: Long) = linuxDoCollection.findEnableEntityByTgId(tgId)

    suspend fun save(linuxDoEntity: LinuxDoEntity) = linuxDoCollection.save(linuxDoEntity)

    suspend fun deleteByTgId(tgId: Long) = linuxDoCollection.deleteEnableEntityByTgId(tgId)

    suspend fun findBySign(sign: Status) = linuxDoCollection.find(eq("sign", sign)).toList()

}