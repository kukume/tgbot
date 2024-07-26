package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.kuku.telegram.mongoDatabase
import org.bson.types.ObjectId

val otherPushCollection = mongoDatabase.getCollection<OtherPushEntity>("other_push")

@Serializable
class OtherPushEntity {
    @Contextual
    @SerialName("_id")
    var id: ObjectId? = null
    var tgId: Long = 0
    var pushTypes: MutableSet<OtherPushType> = mutableSetOf()
    var complex: Complex = Complex()

    enum class OtherPushType {
        Email
    }

    @Serializable
    class Complex {
        var receiveEmail: String = ""
    }
}

object OtherPushService {

    suspend fun save(entity: OtherPushEntity) = otherPushCollection.save(entity)

    suspend fun findByTgId(tgId: Long): OtherPushEntity {
        return otherPushCollection.find(eq("tgId", tgId)).firstOrNull() ?: kotlin.run {
            val addEntity = OtherPushEntity()
            addEntity.tgId = tgId
            otherPushCollection.save(addEntity)
        }
    }

}