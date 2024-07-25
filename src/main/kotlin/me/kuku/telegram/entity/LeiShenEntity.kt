package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.toList
import me.kuku.telegram.mongoDatabase
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

val leiShenCollection = mongoDatabase.getCollection<LeiShenEntity>("lei_shen")

class LeiShenEntity: BaseEntity() {
    @BsonId
    var id: ObjectId? = null
    var username: String = ""
    var password: String = ""
    var accountToken: String = ""
    var nnToken: String = ""
    var status: Status = Status.OFF
    var expiryTime: Long = 0
}

object LeiShenService {

    suspend fun findByTgId(tgId: Long): LeiShenEntity? = TODO()

    suspend fun findByStatus(status: Status) = leiShenCollection.find(eq("status", status)).toList()

    suspend fun deleteByTgId(tgId: Long) = Unit

    suspend fun save(entity: LeiShenEntity) = leiShenCollection.save(entity)

}


