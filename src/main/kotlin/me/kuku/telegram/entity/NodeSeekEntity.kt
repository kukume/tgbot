package me.kuku.telegram.entity

import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.kuku.telegram.mongoDatabase
import org.bson.types.ObjectId

val nodeSeekCollection = mongoDatabase.getCollection<NodeSeekEntity>("node_seek")

@Serializable
class NodeSeekEntity: BaseEntity() {
    @Contextual
    @SerialName("_id")
    var id: ObjectId? = null
    var cookie: String = ""
    var sign: Sign = Sign.None

    enum class Sign(val value: String) {
        Random("随机"), Fix("固定"), None("关闭")
    }

}

object NodeSeekService {

    suspend fun save(entity: NodeSeekEntity) = nodeSeekCollection.save(entity)

    suspend fun findByTgId(tgId: Long) = nodeSeekCollection.findEnableEntityByTgId(tgId)

    suspend fun deleteByTgId(tgId: Long) = nodeSeekCollection.deleteEnableEntityByTgId(tgId)

    suspend fun findAll() = nodeSeekCollection.find().toList()

}
