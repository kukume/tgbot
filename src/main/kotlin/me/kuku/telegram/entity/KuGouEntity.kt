package me.kuku.telegram.entity

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Document("ku_gou")
class KuGouEntity {
    @Id
    var id: String? = null
    var tgId: Long = 0
    var token: String = ""
    var userid: Long = 0
    var kuGoo: String = ""
    var mid: String = ""
    var sign: Status = Status.OFF
}

interface KuGouRepository: ReactiveMongoRepository<KuGouEntity, String> {

    fun findByTgId(tgId: Long): Mono<KuGouEntity>

    fun findBySign(sign: Status): Flux<KuGouEntity>

}

@Service
class KuGouService(
    private val kuGouRepository: KuGouRepository
) {

    suspend fun findByTgId(tgId: Long) = kuGouRepository.findByTgId(tgId).awaitSingleOrNull()

    suspend fun findBySign(sign: Status): List<KuGouEntity> = kuGouRepository.findBySign(sign).collectList().awaitSingle()

    suspend fun save(kuGouEntity: KuGouEntity): KuGouEntity = kuGouRepository.save(kuGouEntity).awaitSingle()

    suspend fun findAll(): List<KuGouEntity> = kuGouRepository.findAll().collectList().awaitSingle()
}