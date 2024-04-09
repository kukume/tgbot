package me.kuku.telegram.entity

import kotlinx.coroutines.flow.toList
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Document("twitter")
class TwitterEntity: BaseEntity() {
    var id: String? = null
    var tId: String = ""
    var tRestId: String = ""
    var cookie: String = ""
    var csrf: String = ""
    var push: Status = Status.OFF
}

interface TwitterRepository: CoroutineCrudRepository<TwitterEntity, String> {

    suspend fun findByTgIdAndTgName(tgId: Long, tgName: String?): TwitterEntity?

    suspend fun findByPush(push: Status): List<TwitterEntity>

    suspend fun deleteByTgIdAndTgName(tgId: Long, tgName: String?)

}

@Service
class TwitterService(
    private val twitterRepository: TwitterRepository
) {

    suspend fun findByTgId(tgId: Long) = twitterRepository.findEnableEntityByTgId(tgId) as? TwitterEntity

    suspend fun findAll(): List<TwitterEntity> = twitterRepository.findAll().toList()

    suspend fun save(entity: TwitterEntity): TwitterEntity = twitterRepository.save(entity)

    suspend fun findByPush(push: Status): List<TwitterEntity> = twitterRepository.findByPush(push)

    @Transactional
    suspend fun deleteByTgId(tgId: Long) = twitterRepository.deleteEnableEntityByTgId(tgId)

}