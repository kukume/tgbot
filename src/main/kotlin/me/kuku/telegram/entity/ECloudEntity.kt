package me.kuku.telegram.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service

@Document("e_cloud")
class ECloudEntity: BaseEntity() {
    @Id
    var id: String? = null
    var cookie: String = ""
    var eCookie: String = ""
    var sign: Status = Status.OFF
}

interface ECloudRepository: CoroutineCrudRepository<ECloudEntity, String> {
    suspend fun findBySign(sign: Status): List<ECloudEntity>

    suspend fun findByTgIdAndTgName(tgId: Long, tgName: String?): ECloudEntity?

    suspend fun deleteByTgIdAndTgName(tgId: Long, tgName: String?)
}

@Service
class ECloudService(
    private val eCloudRepository: ECloudRepository
) {

    suspend fun findBySign(sign: Status) = eCloudRepository.findBySign(sign)

    suspend fun save(entity: ECloudEntity) = eCloudRepository.save(entity)

    suspend fun findByTgId(tgId: Long) = eCloudRepository.findEnableEntityByTgId(tgId) as? ECloudEntity

    suspend fun deleteByTgId(tgId: Long) = eCloudRepository.deleteEnableEntityByTgId(tgId)

}