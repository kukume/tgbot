package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.kuku.telegram.mongoDatabase
import org.bson.types.ObjectId

val iqyCollection = mongoDatabase.getCollection<IqyEntity>("iqy")

@Serializable
class IqyEntity: BaseEntity() {
    @Contextual
    @SerialName("_id")
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

    suspend fun findByTgId(tgId: Long) = iqyCollection.findEnableEntityByTgId(tgId)

    suspend fun save(entity: IqyEntity) = iqyCollection.save(entity)

    suspend fun findBySign(status: Status) = iqyCollection.find(eq("sign", status)).toList()

    suspend fun deleteByTgId(tgId: Long) = iqyCollection.deleteEnableEntityByTgId(tgId)

}