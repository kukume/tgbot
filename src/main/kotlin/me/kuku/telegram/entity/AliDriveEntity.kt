package me.kuku.telegram.entity

import com.mongodb.client.model.Filters
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import me.kuku.telegram.mongoDatabase
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

val aliDriveCollection = mongoDatabase.getCollection<AliDriveEntity>("ali_drive")

class AliDriveEntity: BaseEntity() {
    @BsonId
    var id: ObjectId? = null
    var refreshToken: String = ""
    var deviceId: String = ""
    var backupDeviceId: String = ""
    var backupDesktopDeviceId: String = ""
    var sign: Status = Status.OFF
    var receive: Status = Status.OFF
    var task: Status = Status.OFF
    var receiveTask: Status = Status.OFF
    var deviceRoom: Status = Status.OFF
    var card: Status = Status.OFF

    var uploads: MutableList<Upload> = mutableListOf()
    data class Upload(val driveId: Int, val fileId: String)
}

object AliDriveService {

    suspend fun findByTgId(tgId: Long): AliDriveEntity = TODO()

    suspend fun findBySign(sign: Status) = aliDriveCollection.find(Filters.eq(AliDriveEntity::sign.name, sign)).toList()

    suspend fun save(aliDriveEntity: AliDriveEntity) = aliDriveCollection.save(aliDriveEntity)

    suspend fun delete(aliDriveEntity: AliDriveEntity) = aliDriveCollection.deleteOne(Filters.eq(aliDriveEntity::id.name, aliDriveEntity.id))

    suspend fun deleteByTgId(tgId: Long): Unit = TODO()

    suspend fun findByTask(task: Status) = aliDriveCollection.find(Filters.eq(AliDriveEntity::task.name, task)).toList()

    suspend fun findByReceiveTask(task: Status) = aliDriveCollection.find(Filters.eq(AliDriveEntity::receiveTask.name, task)).toList()

    suspend fun findAll() = aliDriveCollection.find().toList()

    suspend fun findById(id: ObjectId) = aliDriveCollection.find(Filters.eq("_id", id)).firstOrNull()

    suspend fun findByDeviceRoom(deviceRoom: Status) = aliDriveCollection.find(Filters.eq(AliDriveEntity::deviceRoom.name, deviceRoom)).toList()

    suspend fun findByCard(card: Status) = aliDriveCollection.find(Filters.eq(AliDriveEntity::card.name, card)).toList()
}
