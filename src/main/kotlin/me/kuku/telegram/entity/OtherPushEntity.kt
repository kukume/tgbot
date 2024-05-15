package me.kuku.telegram.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service

@Document("other_push")
class OtherPushEntity {
    @Id
    var id: String? = null
    var tgId: Long = 0
    var pushTypes: MutableSet<OtherPushType> = mutableSetOf()
    var complex: Complex = Complex()

    enum class OtherPushType {
        Email
    }

    class Complex {
        var receiveEmail: String = ""
    }
}

interface OtherPushRepository: CoroutineCrudRepository<OtherPushEntity, String> {

    suspend fun findByTgId(tgId: Long): OtherPushEntity?

}

@Service
class OtherPushService(
    private val otherPushRepository: OtherPushRepository
) {

    suspend fun save(entity: OtherPushEntity) = otherPushRepository.save(entity)

    suspend fun findByTgId(tgId: Long): OtherPushEntity {
        return otherPushRepository.findByTgId(tgId) ?: kotlin.run {
            val addEntity = OtherPushEntity()
            addEntity.tgId = tgId
            otherPushRepository.save(addEntity)
        }
    }

}