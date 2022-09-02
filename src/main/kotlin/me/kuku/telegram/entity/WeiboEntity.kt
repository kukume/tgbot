package me.kuku.telegram.entity

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Document("weibo")
class WeiboEntity {
    @Id
    var id: String? = null
    var tgId: Long = 0
    var cookie: String = ""
    var push: Status = Status.OFF
    var sign: Status = Status.OFF
}

interface WeiboRepository: ReactiveMongoRepository<WeiboEntity, String> {

    fun findByTgId(tgId: Long): Mono<WeiboEntity>

    fun findByPush(push: Status): Flux<WeiboEntity>

    fun findBySign(sign: Status): Flux<WeiboEntity>

}

@Service
class WeiboService(
    private val weiboRepository: WeiboRepository
) {

    suspend fun findByTgId(tgId: Long) = weiboRepository.findByTgId(tgId).awaitSingleOrNull()

    suspend fun findByPush(push: Status): List<WeiboEntity> = weiboRepository.findByPush(push).collectList().awaitSingle()

    suspend fun findBySign(sign: Status): List<WeiboEntity> = weiboRepository.findBySign(sign).collectList().awaitSingle()

    suspend fun save(weiboEntity: WeiboEntity): WeiboEntity = weiboRepository.save(weiboEntity).awaitSingle()

}