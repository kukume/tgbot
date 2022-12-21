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

@Document("dou_yu")
class DouYuEntity {
    @Id
    var id: String? = null
    var tgId: Long = 0
    var cookie: String = ""
    var live: Status = Status.OFF
    var fishGroup: Status = Status.OFF
}

interface DouYuRepository: ReactiveMongoRepository<DouYuEntity, String> {

    fun findByTgId(tgId: Long): Mono<DouYuEntity>

    fun findByLive(live: Status): Flux<DouYuEntity>

    fun deleteByTgId(tgId: Long): Mono<Void>

    fun findByFishGroup(fishGroup: Status): Flux<DouYuEntity>

}

@Service
class DouYuService(
    private val douYuRepository: DouYuRepository
) {
    suspend fun findByLive(live: Status): List<DouYuEntity> = douYuRepository.findByLive(live).collectList().awaitSingle()

    suspend fun save(douYuEntity: DouYuEntity) = douYuRepository.save(douYuEntity).awaitSingleOrNull()

    suspend fun findByTgId(tgId: Long) = douYuRepository.findByTgId(tgId).awaitSingleOrNull()

    suspend fun findAll(): List<DouYuEntity> = douYuRepository.findAll().collectList().awaitSingle()

    @Transactional
    suspend fun deleteByTgId(tgId: Long) = douYuRepository.deleteByTgId(tgId).awaitSingleOrNull()

    suspend fun findByFishGroup(fishGroup: Status): List<DouYuEntity> = douYuRepository.findByFishGroup(fishGroup).collectList().awaitSingle()

}
