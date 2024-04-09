package me.kuku.telegram.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service

@Document("iqy")
class IqyEntity: BaseEntity() {
    @Id
    var id: String? = null
    var platform: String = ""
    var deviceId: String = ""
    var cookie: String = ""
    var authCookie: String = ""
    var p00001: String = ""
    var qyId: String = ""
    var userid: Long = 0
    var sign: Status = Status.OFF
}

interface IqyRepository: CoroutineCrudRepository<IqyEntity, String> {
    suspend fun findByTgIdAndTgName(tgId: Long, tgName: String?): IqyEntity?
    suspend fun findBySign(status: Status): List<IqyEntity>
    suspend fun deleteByTgIdAndTgName(tgId: Long, tgName: String?)
}

@Service
class IqyService(
    private val iqyRepository: IqyRepository
) {

    suspend fun findByTgId(tgId: Long) = iqyRepository.findEnableEntityByTgId(tgId) as? IqyEntity

    suspend fun save(entity: IqyEntity) = iqyRepository.save(entity)

    suspend fun findBySign(status: Status) = iqyRepository.findBySign(status)

    suspend fun deleteByTgId(tgId: Long) = iqyRepository.deleteEnableEntityByTgId(tgId)

}