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
    var sign: Status = Status.OFF
}

@Suppress("SpringDataRepositoryMethodReturnTypeInspection")
interface ECloudRepository: CoroutineCrudRepository<ECloudEntity, String> {
    suspend fun findBySign(sign: Status): List<ECloudEntity>

    suspend fun findByTgId(tgId: Long): ECloudEntity?
}

@Service
class ECloudService(
    private val eCloudRepository: ECloudRepository
) {

    suspend fun findBySign(sign: Status) = eCloudRepository.findBySign(sign)

    suspend fun save(entity: ECloudEntity) = eCloudRepository.save(entity)

    suspend fun findByTgId(tgId: Long) = eCloudRepository.findByTgId(tgId)

}