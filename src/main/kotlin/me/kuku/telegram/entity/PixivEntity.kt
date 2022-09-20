package me.kuku.telegram.entity

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Document("pixiv")
class PixivEntity {
    @Id
    var id: String? = null
    var tgId: Long = 0
    var cookie: String = ""
    var push: Status = Status.OFF
}


interface PixivRepository: ReactiveMongoRepository<PixivEntity, String> {

    fun findByTgId(tgId: Long): Mono<PixivEntity>

    fun findByPush(push: Status): Flux<PixivEntity>

}

@Service
class PixivService(
    private val pixivRepository: PixivRepository
) {

    suspend fun findByTgId(tgId: Long) = pixivRepository.findByTgId(tgId).awaitSingleOrNull()

    suspend fun findByPush(push: Status): List<PixivEntity> = pixivRepository.findByPush(push).collectList().awaitSingle()

    suspend fun save(piXivEntity: PixivEntity): PixivEntity = pixivRepository.save(piXivEntity).awaitSingle()

}