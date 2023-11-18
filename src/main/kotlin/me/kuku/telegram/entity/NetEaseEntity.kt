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

@Document("net_ease")
class NetEaseEntity {
    @Id
    var id: String? = null
    var tgId: Long = 0
    var musicU: String = ""
    var csrf: String = ""
    var sign: Status = Status.OFF
    var musicianSign: Status = Status.OFF

    fun cookie() = "channel=netease; __remember_me=true; MUSIC_U=$musicU; __csrf=$csrf; "

    fun pcCookie() = "os=pc; ${cookie()}"
}

interface NetEaseRepository: ReactiveMongoRepository<NetEaseEntity, String> {

    fun findByTgId(tgId: Long): Mono<NetEaseEntity>

    fun findBySign(sign: Status): Flux<NetEaseEntity>

    fun findByMusicianSign(musicianSign: Status): Flux<NetEaseEntity>

    fun deleteByTgId(tgId: Long): Mono<Void>

}

@Service
class NetEaseService(
    private val netEaseRepository: NetEaseRepository
) {

    suspend fun findByTgId(tgId: Long) = netEaseRepository.findByTgId(tgId).awaitSingleOrNull()

    suspend fun findBySign(sign: Status): List<NetEaseEntity> = netEaseRepository.findBySign(sign).collectList().awaitSingle()

    suspend fun findByMusicianSign(musicianSign: Status): List<NetEaseEntity> = netEaseRepository.findByMusicianSign(musicianSign).collectList().awaitSingle()

    suspend fun save(netEaseEntity: NetEaseEntity): NetEaseEntity = netEaseRepository.save(netEaseEntity).awaitSingle()

    suspend fun findAll(netEaseEntity: NetEaseEntity): List<NetEaseEntity> = netEaseRepository.findAll().collectList().awaitSingle()

    @Transactional
    suspend fun deleteByTgId(tgId: Long) = netEaseRepository.deleteByTgId(tgId).awaitSingleOrNull()

}
