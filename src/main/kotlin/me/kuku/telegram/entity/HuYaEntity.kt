package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.toList
import me.kuku.telegram.mongoDatabase
import org.bson.types.ObjectId

val huYaCollection = mongoDatabase.getCollection<HuYaEntity>("hu_ya")

class HuYaEntity: BaseEntity() {
    var id: ObjectId? = null
    var cookie: String = ""
    var live: Status = Status.OFF
}

object HuYaService {

    suspend fun findByTgId(tgId: Long): HuYaEntity? = TODO()

    suspend fun findByLive(live: Status) = huYaCollection.find(eq("live", live)).toList()

    suspend fun save(huYaEntity: HuYaEntity) = huYaCollection.save(huYaEntity)

    suspend fun findAll() = huYaCollection.find().toList()

    suspend fun deleteByTgId(tgId: Long): Unit = TODO()

}