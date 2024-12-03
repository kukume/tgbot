package me.kuku.telegram.entity

import kotlinx.coroutines.flow.toList
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Document("dou_yu")
class DouYuEntity: BaseEntity() {
    @Id
    var id: String? = null
    var cookie: String = ""
    var live: Status = Status.OFF
    var fishGroup: Status = Status.OFF
    var push: Status = Status.OFF
    var titleChange: Status = Status.OFF
}

interface DouYuRepository: CoroutineCrudRepository<DouYuEntity, String> {

    suspend fun findByTgIdAndTgName(tgId: Long, tgName: String?): DouYuEntity?

    suspend fun findByLive(live: Status): List<DouYuEntity>

    suspend fun deleteByTgIdAndTgName(tgId: Long, tgName: String?)

    suspend fun findByFishGroup(fishGroup: Status): List<DouYuEntity>

    suspend fun findByPush(push: Status): List<DouYuEntity>

    suspend fun findByTitleChange(titleChange: Status): List<DouYuEntity>

}

@Service
class DouYuService(
    private val douYuRepository: DouYuRepository
) {
    suspend fun findByLive(live: Status): List<DouYuEntity> = douYuRepository.findByLive(live)

    suspend fun save(douYuEntity: DouYuEntity) = douYuRepository.save(douYuEntity)

    suspend fun findByTgId(tgId: Long) = douYuRepository.findEnableEntityByTgId(tgId) as? DouYuEntity

    suspend fun findAll(): List<DouYuEntity> = douYuRepository.findAll().toList()

    @Transactional
    suspend fun deleteByTgId(tgId: Long) = douYuRepository.deleteEnableEntityByTgId(tgId)

    suspend fun findByFishGroup(fishGroup: Status): List<DouYuEntity> = douYuRepository.findByFishGroup(fishGroup)

    suspend fun findByPush(push: Status): List<DouYuEntity> = douYuRepository.findByPush(push)

    suspend fun findByTitleChange(titleChange: Status): List<DouYuEntity> = douYuRepository.findByTitleChange(titleChange)

}
