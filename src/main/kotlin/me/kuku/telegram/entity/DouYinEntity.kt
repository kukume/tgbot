package me.kuku.telegram.entity

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Document("dou_yin")
class DouYinEntity {
    var id: String? = null
    var tgId: Long = 0
    var cookie: String = ""
    var userid: Long = 0
    var secUserid: String = ""
    var push: Status = Status.OFF
}

interface DouYinRepository: ReactiveMongoRepository<DouYinEntity, String> {

    fun findByTgId(tgId: Long): Mono<DouYinEntity>

    fun deleteByTgId(tgId: Long): Mono<Void>

    fun findByPush(push: Status): Flux<DouYinEntity>

}


@Service
class DouYinService(
    private val douYinRepository: DouYinRepository
) {

    suspend fun save(douYinEntity: DouYinEntity): DouYinEntity = douYinRepository.save(douYinEntity).awaitSingle()

    suspend fun findByTgId(tgId: Long) = douYinRepository.findByTgId(tgId).awaitSingleOrNull()

    @Transactional
    suspend fun deleteByTgId(tgId: Long) = douYinRepository.deleteByTgId(tgId).awaitSingleOrNull()

    suspend fun findAll(): List<DouYinEntity> = douYinRepository.findAll().collectList().awaitSingle()

    suspend fun findByPush(push: Status): List<DouYinEntity> = douYinRepository.findByPush(push).collectList().awaitSingle()

}
