package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.toList
import me.kuku.telegram.mongoDatabase
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

val smZdmCollection = mongoDatabase.getCollection<SmZdmEntity>("sm_zdm")

class SmZdmEntity: BaseEntity() {
    @BsonId
    var id: ObjectId? = null
    var cookie: String = ""
    var sign: Status = Status.OFF
}

object SmZdmService {

    suspend fun findByTgId(tgId: Long): SmZdmEntity? = TODO()

    suspend fun save(smZdmEntity: SmZdmEntity) = smZdmCollection.save(smZdmEntity)

    suspend fun delete(smZdmEntity: SmZdmEntity) = smZdmCollection.deleteOne(eq(smZdmEntity.id))

    suspend fun findBySign(sign: Status) = smZdmCollection.find(eq("sign", sign)).toList()

    suspend fun deleteByTgId(tgId: Long) = Unit

}
