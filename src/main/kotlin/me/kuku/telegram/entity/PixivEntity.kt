package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.toList
import me.kuku.telegram.mongoDatabase
import org.bson.codecs.pojo.annotations.BsonId

val pixivCollection = mongoDatabase.getCollection<PixivEntity>("pixiv")

class PixivEntity: BaseEntity() {
    @BsonId
    var id: String? = null
    var cookie: String = ""
    var push: Status = Status.OFF
}


object PixivService {

    suspend fun findByTgId(tgId: Long): PixivEntity? = TODO()

    suspend fun findByPush(push: Status) = pixivCollection.find(eq("push", push)).toList()

    suspend fun save(piXivEntity: PixivEntity) = pixivCollection.save(piXivEntity)

    suspend fun deleteByTgId(tgId: Long) = Unit

}