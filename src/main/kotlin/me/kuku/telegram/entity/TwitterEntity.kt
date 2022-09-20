package me.kuku.telegram.entity

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Document("twitter")
class TwitterEntity {
    var id: String? = null
    var tgId: Long = 0
    var tId: String = ""
    var tRestId: String = ""
    var cookie: String = ""
    var csrf: String = ""
    var push: Status = Status.OFF
}

interface TwitterRepository: ReactiveMongoRepository<TwitterEntity, String> {

    fun findByTgId(tgId: Long): Mono<TwitterEntity>

    fun findByPush(push: Status): Flux<TwitterEntity>

}

@Service
class TwitterService(
    private val twitterRepository: TwitterRepository
) {

    suspend fun findByTgId(tgId: Long) = twitterRepository.findByTgId(tgId).awaitSingleOrNull()

    suspend fun findAll(): List<TwitterEntity> = twitterRepository.findAll().collectList().awaitSingle()

    suspend fun save(entity: TwitterEntity): TwitterEntity = twitterRepository.save(entity).awaitSingle()

    suspend fun findByPush(push: Status) = twitterRepository.findByPush(push).collectList().awaitSingle()

}