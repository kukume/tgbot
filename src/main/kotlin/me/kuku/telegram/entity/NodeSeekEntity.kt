package me.kuku.telegram.entity

import kotlinx.coroutines.flow.toList
import me.kuku.telegram.mongoDatabase
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

val nodeSeekCollection = mongoDatabase.getCollection<NodeSeekEntity>("node_seek")

class NodeSeekEntity: BaseEntity() {
    @BsonId
    var id: ObjectId? = null
    var cookie: String = ""
    var sign: Sign = Sign.None

    enum class Sign(val value: String) {
        Random("随机"), Fix("固定"), None("关闭")
    }

}

object NodeSeekService {

    suspend fun save(entity: NodeSeekEntity) = nodeSeekCollection.save(entity)

    suspend fun findByTgId(tgId: Long): NodeSeekEntity? = TODO()

    suspend fun deleteByTgId(tgId: Long) = Unit

    suspend fun findAll() = nodeSeekCollection.find().toList()

}
