package me.kuku.telegram.entity

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Document("hu_ya")
class HuYaEntity {
    var id: String? = null
    var tgId: Long = 0
    var cookie: String = ""
    var live: Status = Status.OFF
}

interface HuYaRepository: ReactiveMongoRepository<HuYaEntity, String> {

    fun findByTgId(tgId: Long): Mono<HuYaEntity>

    fun findByLive(live: Status): Flux<HuYaEntity>

}

@Service
class HuYaService(
    private val huYaRepository: HuYaRepository
) {

    suspend fun findByTgId(tgId: Long) = huYaRepository.findByTgId(tgId).awaitSingleOrNull()

    suspend fun findByLive(live: Status): List<HuYaEntity> = huYaRepository.findByLive(live).collectList().awaitSingle()

    suspend fun save(huYaEntity: HuYaEntity): HuYaEntity = huYaRepository.save(huYaEntity).awaitSingle()

    suspend fun findAll(): List<HuYaEntity> = huYaRepository.findAll().collectList().awaitSingle()

}