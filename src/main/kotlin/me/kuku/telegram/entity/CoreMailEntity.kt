package me.kuku.telegram.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service

@Document("core_mail")
class CoreMailEntity: BaseEntity() {
    @Id
    var id: String? = null
    var url: String = ""
    var suffix: String = ""
    var username: String = ""
    var password: String = ""
    var cookie: String = ""
    var sid: String = ""
    var type: Type = Type.XT5
    var secret: String = ""
    var token: String = ""
    var tokenExpire: Long = 0

    enum class Type {
        XT5, XT3
    }

    fun mail() = "$username@$suffix"
}


interface CoreMailRepository: CoroutineCrudRepository<CoreMailEntity, String> {

    suspend fun findByTgId(tgId: Long): List<CoreMailEntity>

}

@Service
class CoreMailService(
    private val coreMailRepository: CoreMailRepository
) {

    suspend fun findByTgId(tgId: Long) = coreMailRepository.findByTgId(tgId)

    suspend fun save(entity: CoreMailEntity) = coreMailRepository.save(entity)

    suspend fun deleteById(id: String) = coreMailRepository.deleteById(id)

    suspend fun findById(id: String) = coreMailRepository.findById(id)


}
