package me.kuku.telegram.entity

import com.mongodb.client.model.Filters
import kotlinx.coroutines.flow.toList
import me.kuku.telegram.mongoDatabase
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

val configCollection = mongoDatabase.getCollection<ConfigEntity>("config")

class ConfigEntity: BaseEntity() {
    @BsonId
    var id: ObjectId? = null
    // 正能量推送
    var positiveEnergy: Status = Status.OFF
    var twoCaptchaKey: String = ""
    var v2exPush: Status = Status.OFF
    var xianBaoPush: Status = Status.OFF // http://new.xianbao.fun/
    var epicFreeGamePush: Status = Status.OFF

    fun twoCaptchaKey() = twoCaptchaKey.ifEmpty { null }
}

object ConfigService {

    suspend fun save(configEntity: ConfigEntity) = configCollection.save(configEntity)

    suspend fun findByTgId(tgId: Long): ConfigEntity? {
        val list = configCollection.find(Filters.eq(ConfigEntity::tgId.name, tgId)).toList()
        if (list.size > 1) {
            val subList = list.subList(1, list.size)
            configCollection.deleteMany(Filters.`in`("_id", subList.map { it.id }))
        }
        return list.firstOrNull()
    }

    suspend fun findAll() = configCollection.find().toList()

    suspend fun findByPositiveEnergy(positiveEnergy: Status) =
        configCollection.find(Filters.eq(ConfigEntity::positiveEnergy.name, positiveEnergy)).toList()
    suspend fun findByV2exPush(v2exPush: Status) = configCollection.find(Filters.eq(ConfigEntity::v2exPush.name, v2exPush)).toList()
    suspend fun findByXianBaoPush(push: Status) = configCollection.find(Filters.eq(ConfigEntity::xianBaoPush.name, push)).toList()

    suspend fun findByEpicFreeGamePush(push: Status) = configCollection.find(Filters.eq(ConfigEntity::epicFreeGamePush.name, push)).toList()

}
