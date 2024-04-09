package me.kuku.telegram.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service

@Document("push")
class PushEntity {
    @Id
    var id: String? = null
    @Indexed(unique = true)
    var tgId: Long = 0
    @Indexed(unique = true)
    var key: String = ""
}

interface PushRepository: CoroutineCrudRepository<PushEntity, String> {
    suspend fun findByTgId(tgId: Long): PushEntity?

    suspend fun findByKey(key: String): PushEntity?
}

@Service
class PushService(
    private val pushRepository: PushRepository
) {

    suspend fun save(entity: PushEntity) = pushRepository.save(entity)

    suspend fun findByTgId(tgId: Long) = pushRepository.findByTgId(tgId)

    suspend fun findByKey(key: String) = pushRepository.findByKey(key)


}
