package me.kuku.telegram.entity

import kotlinx.coroutines.flow.toList
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Document("bili_bili")
class BiliBiliEntity: BaseEntity() {
    @Id
    var id: String? = null
    var cookie: String = ""
    var userid: String = ""
    var token: String = ""
    var push: Status = Status.OFF
    var sign: Status = Status.OFF
    var live: Status = Status.OFF
//    var coin: Status = Status.OFF
}

interface BiliBiliRepository: CoroutineCrudRepository<BiliBiliEntity, String> {

    suspend fun findByTgIdAndTgName(tgId: Long, tgName: String?): BiliBiliEntity?

    suspend fun findByPush(push: Status): List<BiliBiliEntity>

    suspend fun findBySign(sign: Status): List<BiliBiliEntity>

    suspend fun findByLive(live: Status): List<BiliBiliEntity>

    suspend fun deleteByTgIdAndTgName(tgId: Long, tgName: String?)

}

@Service
class BiliBiliService(
    private val biliBiliRepository: BiliBiliRepository
) {

    suspend fun findByTgId(tgId: Long) = biliBiliRepository.findEnableEntityByTgId(tgId) as? BiliBiliEntity

    suspend fun findByPush(push: Status): List<BiliBiliEntity> = biliBiliRepository.findByPush(push)

    suspend fun findBySign(sign: Status): List<BiliBiliEntity> = biliBiliRepository.findBySign(sign)

    suspend fun findByLive(live: Status): List<BiliBiliEntity> = biliBiliRepository.findByLive(live)

    suspend fun save(biliEntity: BiliBiliEntity) = biliBiliRepository.save(biliEntity)

    suspend fun findAll(): List<BiliBiliEntity> = biliBiliRepository.findAll().toList()

    @Transactional
    suspend fun deleteByTgId(tgId: Long) = biliBiliRepository.deleteEnableEntityByTgId(tgId)

}