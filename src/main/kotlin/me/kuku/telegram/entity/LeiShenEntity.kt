@file:Suppress("SpringDataRepositoryMethodReturnTypeInspection")

package me.kuku.telegram.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Document("lei_shen")
class LeiShenEntity: BaseEntity() {
    @Id
    var id: String? = null
    var tgId: Long = 0
    var username: String = ""
    var password: String = ""
    var accountToken: String = ""
    var nnToken: String = ""
    var status: Status = Status.OFF
    var expiryTime: Long = 0
}

interface LeiShenRepository: CoroutineCrudRepository<LeiShenEntity, String> {

    suspend fun findByTgId(tgId: Long): LeiShenEntity?

    suspend fun findByStatus(status: Status): List<LeiShenEntity>

    suspend fun deleteByTgId(tgId: Long)
}

@Service
class LeiShenService(
    private val leiShenRepository: LeiShenRepository
) {

    suspend fun findByTgId(tgId: Long) = leiShenRepository.findByTgId(tgId)

    suspend fun findByStatus(status: Status) = leiShenRepository.findByStatus(status)

    @Transactional
    suspend fun deleteByTgId(tgId: Long) = leiShenRepository.deleteByTgId(tgId)

    suspend fun save(entity: LeiShenEntity) = leiShenRepository.save(entity)

}


