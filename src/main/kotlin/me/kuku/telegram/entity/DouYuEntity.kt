package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.toList
import me.kuku.telegram.mongoDatabase
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

val douYuCollection = mongoDatabase.getCollection<DouYuEntity>("dou_yu")

class DouYuEntity: BaseEntity() {
    @BsonId
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

    suspend fun findByTgId(tgId: Long): DouYuEntity = TODO()

    suspend fun findAll() = douYuCollection.find().toList()

    suspend fun deleteByTgId(tgId: Long): Unit = TODO()

    suspend fun findByFishGroup(fishGroup: Status) = douYuCollection.find(eq(DouYuEntity::fishGroup.name, fishGroup)).toList()

    suspend fun findByPush(push: Status) = douYuCollection.find(eq(DouYuEntity::push.name, push)).toList()

    suspend fun findByTitleChange(titleChange: Status) = douYuCollection.find(eq(DouYuEntity::titleChange.name, titleChange)).toList()

}
