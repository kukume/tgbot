package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.kuku.telegram.mongoDatabase
import org.bson.types.ObjectId

val douYuCollection = mongoDatabase.getCollection<DouYuEntity>("dou_yu")

@Serializable
class DouYuEntity: BaseEntity() {
    @Contextual
    @SerialName("_id")
    var id: ObjectId? = null
    var cookie: String = ""
    var appCookie: String = ""
    var live: Status = Status.OFF
    var fishGroup: Status = Status.OFF
    var push: Status = Status.OFF
    var titleChange: Status = Status.OFF
}

object DouYuService {
    suspend fun findByLive(live: Status) = douYuCollection.find(eq(DouYuEntity::live.name, live)).toList()

    suspend fun save(douYuEntity: DouYuEntity) = douYuCollection.save(douYuEntity)

    suspend fun findByTgId(tgId: Long) = douYuCollection.findEnableEntityByTgId(tgId)

    suspend fun findAll() = douYuCollection.find().toList()

    suspend fun deleteByTgId(tgId: Long) = douYuCollection.deleteEnableEntityByTgId(tgId)

    suspend fun findByFishGroup(fishGroup: Status) = douYuCollection.find(eq(DouYuEntity::fishGroup.name, fishGroup)).toList()

    suspend fun findByPush(push: Status) = douYuCollection.find(eq(DouYuEntity::push.name, push)).toList()

    suspend fun findByTitleChange(titleChange: Status) = douYuCollection.find(eq(DouYuEntity::titleChange.name, titleChange)).toList()

}
