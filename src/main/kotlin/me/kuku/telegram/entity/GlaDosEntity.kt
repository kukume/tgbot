package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.kuku.telegram.mongoDatabase
import org.bson.types.ObjectId

val glaDosCollection = mongoDatabase.getCollection<GlaDosEntity>("glo_dos")

@Serializable
class GlaDosEntity: BaseEntity() {
    @Contextual
    @SerialName("_id")
    var id: ObjectId? = null
    var cookie: String = ""
    var sign: Status = Status.OFF
}

object GlaDosService {

    suspend fun save(glaDosEntity: GlaDosEntity) = glaDosCollection.save(glaDosEntity)

    suspend fun findBySign(sign: Status) = glaDosCollection.find(eq("sign", sign)).toList()

    suspend fun findByTgId(tgId: Long) = glaDosCollection.findEnableEntityByTgId(tgId)

    suspend fun deleteByTgId(tgId: Long) = glaDosCollection.deleteEnableEntityByTgId(tgId)

}
