package me.kuku.telegram.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service

@Document("sm_zdm")
class SmZdmEntity: BaseEntity() {
    @Id
    var id: String? = null
    var cookie: String = ""
    var sign: Status = Status.OFF
}

interface SmZdmRepository: CoroutineCrudRepository<SmZdmEntity, String> {

    suspend fun findByTgIdAndTgName(tgId: Long, tgName: String?): SmZdmEntity?

    suspend fun findBySign(sign: Status): List<SmZdmEntity>

    suspend fun deleteByTgIdAndTgName(tgId: Long, tgName: String?)

}

@Service
class SmZdmService(
    private val smZdmRepository: SmZdmRepository
) {

    suspend fun findByTgId(tgId: Long) = smZdmRepository.findEnableEntityByTgId(tgId) as? SmZdmEntity

    suspend fun save(smZdmEntity: SmZdmEntity) = smZdmRepository.save(smZdmEntity)

    suspend fun delete(smZdmEntity: SmZdmEntity) = smZdmRepository.delete(smZdmEntity)

    suspend fun findBySign(sign: Status) = smZdmRepository.findBySign(sign)

    suspend fun deleteByTgId(tgId: Long) = smZdmRepository.deleteEnableEntityByTgId(tgId)

}
