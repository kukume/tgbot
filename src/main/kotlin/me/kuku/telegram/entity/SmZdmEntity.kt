@file:Suppress("SpringDataRepositoryMethodReturnTypeInspection")

package me.kuku.telegram.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service

@Document("sm_zdm")
class SmZdmEntity {
    @Id
    var id: String? = null
    var tgId: Long = 0
    var cookie: String = ""
    var sign: Status = Status.OFF
}

interface SmZdmRepository: CoroutineCrudRepository<SmZdmEntity, String> {

    suspend fun findByTgId(tgId: Long): SmZdmEntity?

    suspend fun findBySign(sign: Status): List<SmZdmEntity>

    suspend fun deleteByTgId(tgId: Long)

}

@Service
class SmZdmService(
    private val smZdmRepository: SmZdmRepository
) {

    suspend fun findByTgId(tgId: Long) = smZdmRepository.findByTgId(tgId)

    suspend fun save(smZdmEntity: SmZdmEntity) = smZdmRepository.save(smZdmEntity)

    suspend fun delete(smZdmEntity: SmZdmEntity) = smZdmRepository.delete(smZdmEntity)

    suspend fun findBySign(sign: Status) = smZdmRepository.findBySign(sign)

    suspend fun deleteByTgId(tgId: Long) = smZdmRepository.deleteByTgId(tgId)

}
