package me.kuku.telegram.entity

import com.mongodb.client.model.Filters
import kotlinx.coroutines.flow.firstOrNull
import me.kuku.telegram.config.TelegramConfig
import me.kuku.telegram.mongoDatabase
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

val botConfigCollection = mongoDatabase.getCollection<BotConfigEntity>("bot_config")

class BotConfigEntity {
    @BsonId
    var id: ObjectId? = null
    var token: String = ""
    var blacklist: MutableList<Long> = mutableListOf()
    var admins: MutableList<Long> = mutableListOf()
    var pushUrl: String = ""
    // 公用
    var twoCaptchaKey: String = ""

    var updatePush: Status = Status.OFF

    fun twoCaptchaKey() = twoCaptchaKey.ifEmpty { null }
}

object BotConfigService {

    suspend fun findByToken(token: String) = botConfigCollection.find(Filters.eq(BotConfigEntity::token.name, token)).firstOrNull()

    suspend fun save(entity: BotConfigEntity) = botConfigCollection.save(entity)

    suspend fun init(): BotConfigEntity {
        val token = TelegramConfig.token
        return findByToken(token) ?: kotlin.run {
            val botConfigEntity = BotConfigEntity()
            botConfigEntity.token = token
            botConfigEntity.also { save(botConfigEntity) }
        }
    }

}
