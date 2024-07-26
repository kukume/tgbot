package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.kuku.telegram.mongoDatabase
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

val pushCollection = mongoDatabase.getCollection<PushEntity>("push")

@Serializable
class PushEntity {
    @Contextual
    @SerialName("_id")
    var id: ObjectId? = null
    var tgId: Long = 0
    var key: String = ""
}

object PushService {

    suspend fun save(entity: PushEntity) = pushCollection.save(entity)

    suspend fun findByTgId(tgId: Long) = pushCollection.find(eq("tgId", tgId)).firstOrNull()

    suspend fun findByKey(key: String) = pushCollection.find(eq("key", key)).firstOrNull()


}
