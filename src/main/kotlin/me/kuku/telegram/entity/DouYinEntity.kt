package me.kuku.telegram.entity

import kotlinx.coroutines.flow.toList
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Document("dou_yin")
class DouYinEntity: BaseEntity() {
    var id: String? = null
    var cookie: String = ""
    var userid: Long = 0
    var secUserid: String = ""
    var push: Status = Status.OFF
}

interface DouYinRepository: CoroutineCrudRepository<DouYinEntity, String> {
    suspend fun findByTgIdAndTgName(tgId: Long, tgName: String?): DouYinEntity?

    suspend fun deleteByTgIdAndTgName(tgId: Long, tgName: String?)

    suspend fun findByPush(push: Status): List<DouYinEntity>

}


@Service
class DouYinService(
    private val douYinRepository: DouYinRepository
) {

    suspend fun save(douYinEntity: DouYinEntity): DouYinEntity = douYinRepository.save(douYinEntity)

    suspend fun findByTgId(tgId: Long) = douYinRepository.findEnableEntityByTgId(tgId) as? DouYinEntity

    @Transactional
    suspend fun deleteByTgId(tgId: Long) = douYinRepository.deleteEnableEntityByTgId(tgId)

    suspend fun findAll(): List<DouYinEntity> = douYinRepository.findAll().toList()

    suspend fun findByPush(push: Status): List<DouYinEntity> = douYinRepository.findByPush(push)

}
