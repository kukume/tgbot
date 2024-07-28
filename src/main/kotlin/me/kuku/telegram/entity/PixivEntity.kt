package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.kuku.telegram.mongoDatabase
import org.bson.types.ObjectId

val pixivCollection = mongoDatabase.getCollection<PixivEntity>("pixiv")

@Serializable
class PixivEntity: BaseEntity() {
    @Contextual
    @SerialName("_id")
    var id: ObjectId? = null
    var cookie: String = ""
    var push: Status = Status.OFF
}


object PixivService {

    suspend fun findByTgId(tgId: Long) = pixivCollection.findEnableEntityByTgId(tgId)

    suspend fun findByPush(push: Status) = pixivCollection.find(eq("push", push)).toList()

    suspend fun save(piXivEntity: PixivEntity) = pixivCollection.save(piXivEntity)

    suspend fun deleteByTgId(tgId: Long) = pixivCollection.deleteEnableEntityByTgId(tgId)

}