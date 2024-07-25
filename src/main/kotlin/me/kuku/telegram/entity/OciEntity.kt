package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import me.kuku.telegram.mongoDatabase
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.io.Serializable

val ociCollection = mongoDatabase.getCollection<OciEntity>("oci")

@Suppress("ConstPropertyName")
class OciEntity: Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
    @BsonId
    var id: ObjectId? = null
    var tgId: Long = 0
    var tenantId: String = ""
    var remark: String = ""
    var userid: String = ""
    var fingerprint: String = ""
    var privateKey: String = ""
    var region: String = ""
}

class OciTask

object OciService {

    suspend fun findByTgId(tgId: Long) = ociCollection.find(eq("tgId", tgId)).toList()

    suspend fun save(entity: OciEntity) = ociCollection.save(entity)

    suspend fun deleteById(id: String) = ociCollection.deleteOne(eq(id))

    suspend fun findById(id: String) = ociCollection.find(eq(id)).firstOrNull()

    suspend fun findAll() = ociCollection.find().toList()

    suspend fun checkRemark(remark: String, tgId: Long): Boolean {
        val entity = ociCollection.find(and(eq("remark", remark), eq("tgId", tgId))).firstOrNull()
        return entity == null || entity.remark == remark
    }

}
