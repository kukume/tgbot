@file:Suppress("SpringDataRepositoryMethodReturnTypeInspection")

package me.kuku.telegram.entity

import kotlinx.coroutines.flow.toList
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service
import java.util.*

@Document("you_pin")
class YouPinEntity {
    @Id
    var id: String? = null
    var tgId: Long = 0
    var userid: Long = 0
    var uk: String = ""
    var token: String = ""
    var monitors: MutableList<Monitor> = mutableListOf()

    class Monitor {
        var templateId: Int = 0
        var templateName: String = ""
        var minAbrade: Double? = null
        var maxAbrade: Double? = null
        var type: Type = Type.Push
        var id: String = UUID.randomUUID().toString().replace("-", "")

        enum class Type {
            Push, LowDepositPush;

            fun str(): String {
                return if (this == Push) {
                    "推送"
                } else if (this == LowDepositPush) {
                    "押金低于售价推送"
                } else "没有匹配的类型"
            }
        }
    }
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

    suspend fun findAll() = youPinRepository.findAll().toList()


}
