package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.toList
import me.kuku.telegram.mongoDatabase
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

val netEaseCollection = mongoDatabase.getCollection<NetEaseEntity>("net_ease")

class NetEaseEntity: BaseEntity() {
    @BsonId
    var id: ObjectId? = null
    var musicU: String = ""
    var csrf: String = ""
    var sign: Status = Status.OFF
    var musicianSign: Status = Status.OFF
    var vipSign: Status = Status.OFF

    fun cookie() = "channel=netease; __remember_me=true; MUSIC_U=$musicU; __csrf=$csrf; "

    fun pcCookie() = "os=pc; ${cookie()}"

    fun androidCookie() = "os=android; ${cookie()}"
}

object NetEaseService {

    suspend fun findByTgId(tgId: Long): NetEaseEntity? = TODO()

    suspend fun findBySign(sign: Status) = netEaseCollection.find(eq("sign", sign)).toList()

    suspend fun findByMusicianSign(musicianSign: Status) = netEaseCollection.find(eq("musicianSign", musicianSign)).toList()

    suspend fun save(netEaseEntity: NetEaseEntity) = netEaseCollection.save(netEaseEntity)

    suspend fun findAll() = netEaseCollection.find().toList()

    suspend fun deleteByTgId(tgId: Long) = Unit

    suspend fun findByVipSign(status: Status) = netEaseCollection.find(eq("vipSign", status)).toList()

}
