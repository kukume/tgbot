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

@Document("host_loc")
class HostLocEntity {
    @Id
    var id: String? = null
    var tgId: Long = 0
    var cookie: String = ""
    var push: Status = Status.OFF
    var sign: Status = Status.OFF
}

interface HostLocRepository: ReactiveMongoRepository<HostLocEntity, String> {

    fun findByTgId(tgId: Long): Mono<HostLocEntity>

    fun findByPush(push: Status): Flux<HostLocEntity>

    fun findBySign(sign: Status): Flux<HostLocEntity>

    fun deleteByTgId(tgId: Long): Mono<Void>

}

@Service
class HostLocService(
    private val hostLocRepository: HostLocRepository
) {
    suspend fun findByTgId(tgId: Long) = hostLocRepository.findByTgId(tgId).awaitSingleOrNull()

    suspend fun findByPush(push: Status): List<HostLocEntity> = hostLocRepository.findByPush(push).collectList().awaitSingle()

    suspend fun findBySign(sign: Status): List<HostLocEntity> = hostLocRepository.findBySign(sign).collectList().awaitSingle()

    suspend fun save(hostLocEntity: HostLocEntity): HostLocEntity = hostLocRepository.save(hostLocEntity).awaitSingle()

    suspend fun findAll(): List<HostLocEntity> = hostLocRepository.findAll().collectList().awaitSingle()

    @Transactional
    suspend fun deleteByTgId(tgId: Long) = hostLocRepository.deleteByTgId(tgId).awaitSingleOrNull()
}