package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.toList
import me.kuku.telegram.mongoDatabase
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

val iqyCollection = mongoDatabase.getCollection<IqyEntity>("iqy")

class IqyEntity: BaseEntity() {
    @BsonId
    var id: ObjectId? = null
    var platform: String = ""
    var deviceId: String = ""
    var cookie: String = ""
    var authCookie: String = ""
    var p00001: String = ""
    var qyId: String = ""
    var userid: Long = 0
    var sign: Status = Status.OFF
}

object IqyService {

    suspend fun findByTgId(tgId: Long): IqyEntity = TODO()

    suspend fun save(entity: IqyEntity) = iqyCollection.save(entity)

    suspend fun findBySign(status: Status) = iqyCollection.find(eq("sign", status)).toList()

    suspend fun deleteByTgId(tgId: Long): Unit = TODO()

}