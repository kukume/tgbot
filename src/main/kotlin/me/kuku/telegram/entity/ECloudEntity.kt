package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.kuku.telegram.mongoDatabase
import org.bson.types.ObjectId

val eCloudCollection = mongoDatabase.getCollection<ECloudEntity>("e_cloud")

@Serializable
class ECloudEntity: BaseEntity() {
    @Contextual
    @SerialName("_id")
    var id: ObjectId? = null
    var cookie: String = ""
    var eCookie: String = ""
    var sign: Status = Status.OFF
}


object ECloudService {

    suspend fun findBySign(sign: Status) = eCloudCollection.find(eq("sign", sign)).toList()

    suspend fun save(entity: ECloudEntity) = eCloudCollection.save(entity)

    suspend fun findByTgId(tgId: Long) = eCloudCollection.findEnableEntityByTgId(tgId)

    suspend fun deleteByTgId(tgId: Long) = eCloudCollection.deleteEnableEntityByTgId(tgId)

}