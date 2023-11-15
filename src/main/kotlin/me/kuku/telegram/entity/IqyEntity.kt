package me.kuku.telegram.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service

@Document("iqy")
class IqyEntity: BaseEntity() {
    @Id
    var id: String? = null
    var platform: String = ""
    var deviceId: String = ""
    var authCookie: String = ""
    var p00001: String = ""
    var qyId: String = ""
    var userid: Long = 0
}

interface IqyRepository: CoroutineCrudRepository<IqyEntity, String>

@Service
class IqyService(
    private val iqyRepository: IqyRepository
) {



}