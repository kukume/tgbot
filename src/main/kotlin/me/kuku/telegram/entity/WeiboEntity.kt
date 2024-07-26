package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.kuku.telegram.mongoDatabase
import org.bson.types.ObjectId

val weiboCollection = mongoDatabase.getCollection<WeiboEntity>("weibo")

@Serializable
class WeiboEntity: BaseEntity() {
    @Contextual
    @SerialName("_id")
    var id: ObjectId? = null
    var cookie: String = ""
    var push: Status = Status.OFF
    var sign: Status = Status.OFF
}


object WeiboService {

    suspend fun findByTgId(tgId: Long) = weiboCollection.findEnableEntityByTgId(tgId)

    suspend fun findByPush(push: Status) = weiboCollection.find(eq("push", push)).toList()

    suspend fun findBySign(sign: Status) = weiboCollection.find(eq("sign", sign)).toList()

    suspend fun save(weiboEntity: WeiboEntity): WeiboEntity = weiboCollection.save(weiboEntity)

    suspend fun deleteByTgId(tgId: Long) = weiboCollection.deleteEnableEntityByTgId(tgId)

}