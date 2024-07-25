package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.toList
import me.kuku.telegram.mongoDatabase
import org.bson.codecs.pojo.annotations.BsonId

val twitterCollection = mongoDatabase.getCollection<TwitterEntity>("twitter")

class TwitterEntity: BaseEntity() {
    @BsonId
    var id: String? = null
    var tId: String = ""
    var tRestId: String = ""
    var cookie: String = ""
    var csrf: String = ""
    var push: Status = Status.OFF
}

object TwitterService {

    suspend fun findByTgId(tgId: Long): TwitterEntity? = TODO()

    suspend fun findAll() = twitterCollection.find().toList()

    suspend fun save(entity: TwitterEntity) = twitterCollection.save(entity)

    suspend fun findByPush(push: Status) = twitterCollection.find(eq("push", push)).toList()

    suspend fun deleteByTgId(tgId: Long) = Unit

}