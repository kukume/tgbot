package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.gt
import kotlinx.coroutines.flow.toList
import me.kuku.telegram.mongoDatabase
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

val stepCollection = mongoDatabase.getCollection<StepEntity>("step")

class StepEntity: BaseEntity() {
    @BsonId
    var id: ObjectId? = null
    var leXinCookie: String = ""
    var leXinUserid: String = ""
    var leXinAccessToken: String = ""
    var miLoginToken: String = ""
    var step: Int = -1
    var offset: Status = Status.OFF
}


object StepService {

    suspend fun findByTgId(tgId: Long): StepEntity? = TODO()

    suspend fun findByAuto() = stepCollection.find(gt("step", 0)).toList()

    suspend fun save(stepEntity: StepEntity) = stepCollection.save(stepEntity)

    suspend fun deleteByTgId(tgId: Long) = Unit

}