package me.kuku.telegram.entity

import kotlinx.coroutines.flow.toList
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Document("hu_ya")
class HuYaEntity: BaseEntity() {
    var id: String? = null
    var cookie: String = ""
    var live: Status = Status.OFF
}

interface HuYaRepository: CoroutineCrudRepository<HuYaEntity, String> {

    suspend fun findByTgIdAndTgName(tgId: Long, tgName: String?): HuYaEntity?

    suspend fun findByLive(live: Status): List<HuYaEntity>

    suspend fun deleteByTgIdAndTgName(tgId: Long, tgName: String?)

}

@Service
class HuYaService(
    private val huYaRepository: HuYaRepository
) {

    suspend fun findByTgId(tgId: Long) = huYaRepository.findEnableEntityByTgId(tgId) as? HuYaEntity

    suspend fun findByLive(live: Status): List<HuYaEntity> = huYaRepository.findByLive(live)

    suspend fun save(huYaEntity: HuYaEntity): HuYaEntity = huYaRepository.save(huYaEntity)

    suspend fun findAll(): List<HuYaEntity> = huYaRepository.findAll().toList()

    @Transactional
    suspend fun deleteByTgId(tgId: Long) = huYaRepository.deleteEnableEntityByTgId(tgId)

}