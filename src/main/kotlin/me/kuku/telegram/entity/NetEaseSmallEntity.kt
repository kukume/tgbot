package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.kuku.telegram.mongoDatabase
import org.bson.types.ObjectId

private val netEaseSmallCollection = mongoDatabase.getCollection<NetEaseSmallEntity>("net_ease_small")

@Serializable
class NetEaseSmallEntity {
    @Contextual
    @SerialName("_id")
    var id: ObjectId? = null
    var username: String = ""
    var password: String = ""
    var musicU: String = ""
    var csrf: String = ""

    fun cookie() = "channel=netease; __remember_me=true; MUSIC_U=$musicU; __csrf=$csrf; "
}

object NetEaseSmallService {

    suspend fun findByUsername(username: String) = netEaseSmallCollection.find(eq("username", username)).firstOrNull()

    suspend fun save(entity: NetEaseSmallEntity) = netEaseSmallCollection.save(entity)

    suspend fun findAll() = netEaseSmallCollection.find().toList()

}