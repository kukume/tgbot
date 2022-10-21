package me.kuku.telegram.entity

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Document("bili_bili")
class BiliBiliEntity {
    @Id
    var id: String? = null
    var tgId: Long = 0
    var cookie: String = ""
    var userid: String = ""
    var token: String = ""
    var push: Status = Status.OFF
    var sign: Status = Status.OFF
    var live: Status = Status.OFF
//    var coin: Status = Status.OFF
}

interface BiliBiliRepository: ReactiveMongoRepository<BiliBiliEntity, String> {

    fun findByTgId(tgId: Long): Mono<BiliBiliEntity>

    fun findByPush(push: Status): Flux<BiliBiliEntity>

    fun findBySign(sign: Status): Flux<BiliBiliEntity>

    fun findByLive(live: Status): Flux<BiliBiliEntity>

    fun deleteByTgId(tgId: Long): Mono<Void>

}

@Service
class BiliBiliService(
    private val biliBiliRepository: BiliBiliRepository
) {

    suspend fun findByTgId(tgId: Long) = biliBiliRepository.findByTgId(tgId).awaitSingleOrNull()

    suspend fun findByPush(push: Status): List<BiliBiliEntity> = biliBiliRepository.findByPush(push).collectList().awaitSingle()

    suspend fun findBySign(sign: Status): List<BiliBiliEntity> = biliBiliRepository.findBySign(sign).collectList().awaitSingle()

    suspend fun findByLive(live: Status): List<BiliBiliEntity> = biliBiliRepository.findByLive(live).collectList().awaitSingle()

    suspend fun save(biliEntity: BiliBiliEntity) = biliBiliRepository.save(biliEntity).awaitSingleOrNull()

    suspend fun findAll(): List<BiliBiliEntity> = biliBiliRepository.findAll().collectList().awaitSingle()

    @Transactional
    suspend fun deleteByTgId(tgId: Long) = biliBiliRepository.deleteByTgId(tgId).awaitSingleOrNull()

}