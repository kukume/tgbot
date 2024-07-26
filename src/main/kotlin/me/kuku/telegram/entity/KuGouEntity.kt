package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.kuku.telegram.mongoDatabase
import org.bson.types.ObjectId

val kuGouCollection = mongoDatabase.getCollection<KuGouEntity>("ku_gou")

@Serializable
class KuGouEntity: BaseEntity() {
    @Contextual
    @SerialName("_id")
    var id: ObjectId? = null
    var token: String = ""
    var userid: Long = 0
    var kuGoo: String = ""
    var mid: String = ""
    var sign: Status = Status.OFF
}

object KuGouService {

    suspend fun findByTgId(tgId: Long) = kuGouCollection.findEnableEntityByTgId(tgId)

    suspend fun findBySign(sign: Status) = kuGouCollection.find(eq("sign", sign)).toList()

    suspend fun save(kuGouEntity: KuGouEntity) = kuGouCollection.save(kuGouEntity)

    suspend fun findAll() = kuGouCollection.find().toList()

    suspend fun deleteByTgId(tgId: Long) = kuGouCollection.deleteEnableEntityByTgId(tgId)
}