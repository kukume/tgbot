package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.toList
import me.kuku.telegram.mongoDatabase
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

val glaDosCollection = mongoDatabase.getCollection<GlaDosEntity>("glo_dos")

class GlaDosEntity: BaseEntity() {
    @BsonId
    var id: ObjectId? = null
    var cookie: String = ""
    var sign: Status = Status.OFF
}

object GlaDosService {

    suspend fun save(glaDosEntity: GlaDosEntity) = glaDosCollection.save(glaDosEntity)

    suspend fun findBySign(sign: Status) = glaDosCollection.find(eq("sign", sign)).toList()

    suspend fun findByTgId(tgId: Long): GlaDosEntity? = TODO()

    suspend fun deleteByTgId(tgId: Long): Unit = TODO()

}
