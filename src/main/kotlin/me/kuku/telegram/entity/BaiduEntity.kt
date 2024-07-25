package me.kuku.telegram.entity

import com.mongodb.client.model.Filters
import kotlinx.coroutines.flow.toList
import me.kuku.telegram.mongoDatabase
import me.kuku.utils.OkUtils
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

val baiduCollection = mongoDatabase.getCollection<BaiduEntity>("baidu")

class BaiduEntity: BaseEntity() {
    @BsonId
    var id: ObjectId? = null
    var cookie: String = ""
    var tieBaSToken: String = ""
    var sign: Status = Status.OFF

    fun otherCookie(sToken: String): String {
        return OkUtils.cookieStr(cookie, "BDUSS") + "STOKEN=$sToken; "
    }

    fun teiBaCookie(): String {
        return otherCookie(tieBaSToken)
    }
}

object BaiduService {

    suspend fun findByTgId(tgId: Long): BaiduEntity? = TODO()

    suspend fun save(baiduEntity: BaiduEntity) = baiduCollection.save(baiduEntity)

    suspend fun findBySign(sign: Status): List<BaiduEntity> = baiduCollection.find(Filters.eq(BaiduEntity::sign.name, sign)).toList()

    suspend fun findAll(): List<BaiduEntity> = baiduCollection.find().toList()

    suspend fun deleteByTgId(tgId: Long): Unit = TODO()
}