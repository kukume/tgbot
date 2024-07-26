package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.kuku.telegram.mongoDatabase
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

val smZdmCollection = mongoDatabase.getCollection<SmZdmEntity>("sm_zdm")

@Serializable
class SmZdmEntity: BaseEntity() {
    @Contextual
    @SerialName("_id")
    var id: ObjectId? = null
    var cookie: String = ""
    var sign: Status = Status.OFF
}

object SmZdmService {

    suspend fun findByTgId(tgId: Long) = smZdmCollection.findEnableEntityByTgId(tgId)

    suspend fun save(smZdmEntity: SmZdmEntity) = smZdmCollection.save(smZdmEntity)

    suspend fun delete(smZdmEntity: SmZdmEntity) = smZdmCollection.deleteOne(eq(smZdmEntity.id))

    suspend fun findBySign(sign: Status) = smZdmCollection.find(eq("sign", sign)).toList()

    suspend fun deleteByTgId(tgId: Long) = smZdmCollection.deleteEnableEntityByTgId(tgId)

}
