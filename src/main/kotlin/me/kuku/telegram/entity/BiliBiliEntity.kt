package me.kuku.telegram.entity

import com.mongodb.client.model.Filters
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.kuku.telegram.mongoDatabase
import org.bson.types.ObjectId

val biliBiliCollection = mongoDatabase.getCollection<BiliBiliEntity>("bili_bili")

@Serializable
class BiliBiliEntity: BaseEntity() {
    @Contextual
    @SerialName("_id")
    var id: ObjectId? = null
    var cookie: String = ""
    var userid: String = ""
    var token: String = ""
    var push: Status = Status.OFF
    var sign: Status = Status.OFF
    var live: Status = Status.OFF
//    var coin: Status = Status.OFF
}

object BiliBiliService {

    suspend fun findByTgId(tgId: Long) = biliBiliCollection.findEnableEntityByTgId(tgId)

    suspend fun findByPush(push: Status) = biliBiliCollection.find(Filters.eq(BiliBiliEntity::push.name, push)).toList()

    suspend fun findBySign(sign: Status) = biliBiliCollection.find(Filters.eq(BiliBiliEntity::sign.name, sign)).toList()

    suspend fun findByLive(live: Status) = biliBiliCollection.find(Filters.eq(BiliBiliEntity::live.name, live)).toList()

    suspend fun save(biliEntity: BiliBiliEntity) = biliBiliCollection.save(biliEntity)

    suspend fun findAll(): List<BiliBiliEntity> = biliBiliCollection.find().toList()

    suspend fun deleteByTgId(tgId: Long) = biliBiliCollection.deleteEnableEntityByTgId(tgId)

}