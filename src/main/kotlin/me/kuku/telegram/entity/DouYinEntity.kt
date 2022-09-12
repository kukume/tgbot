package me.kuku.telegram.entity

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Document("dou_yin")
class DouYinEntity {
    var id: String? = null
    var tgId: Long = 0
    var cookie: String = ""
    var userid: Long = 0
    var secUserid: String = ""
}

interface DouYinRepository: ReactiveMongoRepository<DouYinEntity, String> {

    fun findByTgId(tgId: Long): Mono<DouYinEntity>

}


@Service
class DouYinService(
    private val douYinRepository: DouYinRepository
) {

    suspend fun save(douYinEntity: DouYinEntity): DouYinEntity = douYinRepository.save(douYinEntity).awaitSingle()

    suspend fun findByTgId(tgId: Long) = douYinRepository.findByTgId(tgId).awaitSingleOrNull()
}