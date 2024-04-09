package me.kuku.telegram.entity

import kotlinx.coroutines.flow.toList
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Document("host_loc")
class HostLocEntity: BaseEntity() {
    @Id
    var id: String? = null
    var cookie: String = ""
    var push: Status = Status.OFF
    var sign: Status = Status.OFF
}

interface HostLocRepository: CoroutineCrudRepository<HostLocEntity, String> {

    suspend fun findByTgIdAndTgName(tgId: Long, tgName: String?): HostLocEntity?

    suspend fun findByPush(push: Status): List<HostLocEntity>

    suspend fun findBySign(sign: Status): List<HostLocEntity>

    suspend fun deleteByTgIdAndTgName(tgId: Long, tgName: String?)

}

@Service
class HostLocService(
    private val hostLocRepository: HostLocRepository
) {
    suspend fun findByTgId(tgId: Long) = hostLocRepository.findEnableEntityByTgId(tgId) as? HostLocEntity

    suspend fun findByPush(push: Status): List<HostLocEntity> = hostLocRepository.findByPush(push)

    suspend fun findBySign(sign: Status): List<HostLocEntity> = hostLocRepository.findBySign(sign)

    suspend fun save(hostLocEntity: HostLocEntity): HostLocEntity = hostLocRepository.save(hostLocEntity)

    suspend fun findAll(): List<HostLocEntity> = hostLocRepository.findAll().toList()

    @Transactional
    suspend fun deleteByTgId(tgId: Long) = hostLocRepository.deleteEnableEntityByTgId(tgId)
}