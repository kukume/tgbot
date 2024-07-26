package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.kuku.telegram.mongoDatabase
import org.bson.types.ObjectId

val switchCollection = mongoDatabase.getCollection<SwitchEntity>("switch")

@Serializable
class SwitchEntity {
    @Contextual
    @SerialName("_id")
    var id: ObjectId? = null
    var name: String = ""
    var tgId: Long = 0
    var status: Status = Status.OFF
}

object SwitchService {

    suspend fun findByTgId(tgId: Long) = switchCollection.find(eq("tgId", tgId)).toList()

    suspend fun findByTgIdAndName(tgId: Long, name: String) =
        switchCollection.find(and(eq("tgId", tgId), eq("name", name))).toList()

    suspend fun save(switchEntity: SwitchEntity) = switchCollection.save(switchEntity)

    suspend fun delete(switchEntity: SwitchEntity) = switchCollection.deleteOne(eq(switchEntity.id))

    suspend fun deleteById(id: ObjectId) = switchCollection.deleteOne(eq(id))

    suspend fun findById(id: ObjectId) = switchCollection.find(eq(id)).firstOrNull()

    suspend fun findByTgIdAndStatus(tgId: Long, status: Status) =
        switchCollection.find(and(eq("tgId", tgId), eq("status", status))).toList()

    suspend fun editName(tgId: Long, oldName: String, name: String) {
        for (collection in collectionList) {
            collection.updateOne(and(eq("tgId", tgId), eq("tgName", oldName)), Updates.set("tgName", name))
        }
    }

    suspend fun deleteName(tgId: Long, name: String) {
        for (collection in collectionList) {
            collection.deleteOne(and(eq("tgId", tgId), eq("tgName", name)))
        }
    }

}


private val collectionList = mutableListOf(
    aliDriveCollection, baiduCollection, biliBiliCollection,
    douYuCollection, eCloudCollection,
    glaDosCollection, hostLocCollection, huYaCollection,
    iqyCollection, kuGouCollection, leiShenCollection,
    logCollection, miHoYoCollection, netEaseCollection,
    nodeSeekCollection, pixivCollection, smZdmCollection, stepCollection,
    twitterCollection, weiboCollection)