@file:Suppress("SpringDataRepositoryMethodReturnTypeInspection")

package me.kuku.telegram.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service

@Document("you_pin")
class YouPinEntity {
    @Id
    var id: String? = null
    var tgId: Long = 0
    var uk: String = ""
    var token: String = ""
}


interface YouPinRepository: CoroutineCrudRepository<YouPinEntity, String> {
    suspend fun findByTgId(tgId: Long): YouPinEntity?

}

@Service
class YouPinService(
    private val youPinRepository: YouPinRepository
) {

    suspend fun findByTgId(tgId: Long) = youPinRepository.findByTgId(tgId)

    suspend fun save(entity: YouPinEntity) = youPinRepository.save(entity)


}
