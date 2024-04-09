package me.kuku.telegram.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Document("pixiv")
class PixivEntity: BaseEntity() {
    @Id
    var id: String? = null
    var cookie: String = ""
    var push: Status = Status.OFF
}


interface PixivRepository: CoroutineCrudRepository<PixivEntity, String> {

    suspend fun findByTgIdAndTgName(tgId: Long, tgName: String?): PixivEntity?

    suspend fun findByPush(push: Status): List<PixivEntity>

    suspend fun deleteByTgIdAndTgName(tgId: Long, tgName: String?)

}

@Service
class PixivService(
    private val pixivRepository: PixivRepository
) {

    suspend fun findByTgId(tgId: Long) = pixivRepository.findEnableEntityByTgId(tgId) as? PixivEntity

    suspend fun findByPush(push: Status): List<PixivEntity> = pixivRepository.findByPush(push)

    suspend fun save(piXivEntity: PixivEntity): PixivEntity = pixivRepository.save(piXivEntity)

    @Transactional
    suspend fun deleteByTgId(tgId: Long) = pixivRepository.deleteEnableEntityByTgId(tgId)

}