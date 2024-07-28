package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.kuku.telegram.logic.MiHoYoFix
import me.kuku.telegram.mongoDatabase
import org.bson.types.ObjectId

val miHoYoCollection = mongoDatabase.getCollection<MiHoYoEntity>("mi_ho_yo")

@Serializable
class MiHoYoEntity: BaseEntity() {
    @Contextual
    @SerialName("_id")
    var id: ObjectId? = null
    var fix: MiHoYoFix = MiHoYoFix()
    var aid: String = ""
    var mid: String = ""
    var cookie: String = ""
    var token: String = ""
    var sToken: String = ""
    var ticket: String = ""
    var sign: Status = Status.OFF
    var mysSign: Status = Status.OFF

    fun hubCookie(): String {
        if (token.isEmpty()) error("未设置token，请使用app账号密码重新登录")
        return "stuid=$aid; stoken=$token; mid=$mid; "
    }
}

object MiHoYoService {

    suspend fun findByTgId(tgId: Long) = miHoYoCollection.findEnableEntityByTgId(tgId)

    suspend fun findBySign(sign: Status) = miHoYoCollection.find(eq("sign", sign)).toList()

    suspend fun findByMysSign(sign: Status) = miHoYoCollection.find(eq("mysSign", sign)).toList()

    suspend fun save(miHoYoEntity: MiHoYoEntity) = miHoYoCollection.save(miHoYoEntity)

    suspend fun findAll() = miHoYoCollection.find().toList()

    suspend fun deleteByTgId(tgId: Long) = miHoYoCollection.deleteEnableEntityByTgId(tgId)

}