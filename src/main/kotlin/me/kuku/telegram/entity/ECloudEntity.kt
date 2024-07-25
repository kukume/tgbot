package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.toList
import me.kuku.telegram.mongoDatabase
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

val eCloudCollection = mongoDatabase.getCollection<ECloudEntity>("e_cloud")

class ECloudEntity: BaseEntity() {
    @BsonId
    var id: ObjectId? = null
    var cookie: String = ""
    var eCookie: String = ""
    var sign: Status = Status.OFF
}


object ECloudService {

    suspend fun findBySign(sign: Status) = eCloudCollection.find(eq("sign", sign)).toList()

    suspend fun save(entity: ECloudEntity) = eCloudCollection.save(entity)

    suspend fun findByTgId(tgId: Long): ECloudEntity? = TODO()

    suspend fun deleteByTgId(tgId: Long): Unit = TODO()

}