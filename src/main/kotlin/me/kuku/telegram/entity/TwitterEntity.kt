package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.kuku.telegram.mongoDatabase

val twitterCollection = mongoDatabase.getCollection<TwitterEntity>("twitter")

@Serializable
class TwitterEntity: BaseEntity() {
    @Contextual
    @SerialName("_id")
    var id: String? = null
    var tId: String = ""
    var tRestId: String = ""
    var cookie: String = ""
    var csrf: String = ""
    var push: Status = Status.OFF
}

object TwitterService {

    suspend fun findByTgId(tgId: Long) = twitterCollection.findEnableEntityByTgId(tgId)

    suspend fun findAll() = twitterCollection.find().toList()

    suspend fun save(entity: TwitterEntity) = twitterCollection.save(entity)

    suspend fun findByPush(push: Status) = twitterCollection.find(eq("push", push)).toList()

    suspend fun deleteByTgId(tgId: Long) = twitterCollection.deleteEnableEntityByTgId(tgId)

}