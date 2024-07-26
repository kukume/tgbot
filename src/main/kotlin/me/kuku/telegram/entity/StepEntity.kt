package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.gt
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.kuku.telegram.mongoDatabase
import org.bson.types.ObjectId

val stepCollection = mongoDatabase.getCollection<StepEntity>("step")

@Serializable
class StepEntity: BaseEntity() {
    @Contextual
    @SerialName("_id")
    var id: ObjectId? = null
    var leXinCookie: String = ""
    var leXinUserid: String = ""
    var leXinAccessToken: String = ""
    var miLoginToken: String = ""
    var step: Int = -1
    var offset: Status = Status.OFF
}


object StepService {

    suspend fun findByTgId(tgId: Long) = stepCollection.findEnableEntityByTgId(tgId)

    suspend fun findByAuto() = stepCollection.find(gt("step", 0)).toList()

    suspend fun save(stepEntity: StepEntity) = stepCollection.save(stepEntity)

    suspend fun deleteByTgId(tgId: Long) = stepCollection.deleteEnableEntityByTgId(tgId)

}