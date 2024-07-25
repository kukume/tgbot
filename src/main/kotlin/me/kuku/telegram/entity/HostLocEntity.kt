package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.toList
import me.kuku.telegram.mongoDatabase
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

val hostLocCollection = mongoDatabase.getCollection<HostLocEntity>("host_loc")

class HostLocEntity: BaseEntity() {
    @BsonId
    var id: ObjectId? = null
    var cookie: String = ""
    var push: Status = Status.OFF
    var sign: Status = Status.OFF
}

object HostLocService {
    suspend fun findByTgId(tgId: Long): HostLocEntity? = TODO()

    suspend fun findByPush(push: Status) = hostLocCollection.find(eq("push", push)).toList()

    suspend fun findBySign(sign: Status) = hostLocCollection.find(eq("sign", sign)).toList()

    suspend fun save(hostLocEntity: HostLocEntity) = hostLocCollection.save(hostLocEntity)

    suspend fun findAll() = hostLocCollection.find().toList()

    suspend fun deleteByTgId(tgId: Long): Unit = TODO()
}