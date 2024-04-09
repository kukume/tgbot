package me.kuku.telegram.entity

import kotlinx.coroutines.flow.toList
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service

@Document("ali_driver")
class AliDriveEntity: BaseEntity() {
    @Id
    var id: String? = null
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

interface AliDriveRepository: CoroutineCrudRepository<AliDriveEntity, String> {

    suspend fun findByTgIdAndTgName(tgId: Long, tgName: String?): AliDriveEntity?

    suspend fun deleteByTgIdAndTgName(tgId: Long, tgName: String?)

    suspend fun findBySign(sign: Status): List<AliDriveEntity>

    suspend fun findByTask(task: Status): List<AliDriveEntity>

    suspend fun findByReceiveTask(task: Status): List<AliDriveEntity>

    suspend fun findByDeviceRoom(deviceRoom: Status): List<AliDriveEntity>

    suspend fun findByCard(card: Status): List<AliDriveEntity>
}

@Service
class AliDriveService(
    private val aliDriveRepository: AliDriveRepository
) {

    suspend fun findByTgId(tgId: Long) = aliDriveRepository.findEnableEntityByTgId(tgId) as? AliDriveEntity

    suspend fun findBySign(sign: Status) = aliDriveRepository.findBySign(sign)

    suspend fun save(aliDriveEntity: AliDriveEntity): AliDriveEntity = aliDriveRepository.save(aliDriveEntity)

    suspend fun delete(aliDriveEntity: AliDriveEntity) = aliDriveRepository.delete(aliDriveEntity)

    suspend fun deleteByTgId(tgId: Long) = aliDriveRepository.deleteEnableEntityByTgId(tgId)

    suspend fun findByTask(task: Status) = aliDriveRepository.findByTask(task)

    suspend fun findByReceiveTask(task: Status) = aliDriveRepository.findByReceiveTask(task)

    suspend fun findAll() = aliDriveRepository.findAll().toList()

    suspend fun findById(id: String) = aliDriveRepository.findById(id)

    suspend fun findByDeviceRoom(deviceRoom: Status) = aliDriveRepository.findByDeviceRoom(deviceRoom)

    suspend fun findByCard(card: Status) = aliDriveRepository.findByCard(card)
}
