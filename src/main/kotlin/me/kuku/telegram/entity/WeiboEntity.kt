package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.toList
import me.kuku.telegram.mongoDatabase
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

val weiboCollection = mongoDatabase.getCollection<WeiboEntity>("weibo")

class WeiboEntity: BaseEntity() {
    @BsonId
    var id: ObjectId? = null
    var cookie: String = ""
    var push: Status = Status.OFF
    var sign: Status = Status.OFF
}


object WeiboService {

    suspend fun findByTgId(tgId: Long): WeiboEntity? = TODO()

    suspend fun findByPush(push: Status) = weiboCollection.find(eq("push", push)).toList()

    suspend fun findBySign(sign: Status) = weiboCollection.find(eq("sign", sign)).toList()

    suspend fun save(weiboEntity: WeiboEntity): WeiboEntity = weiboCollection.save(weiboEntity)

    suspend fun deleteByTgId(tgId: Long) = Unit

}