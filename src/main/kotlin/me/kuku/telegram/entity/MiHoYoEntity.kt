package me.kuku.telegram.entity

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Document("mi_ho_yo")
class MiHoYoEntity {
    @Id
    var id: String? = null
    var tgId: Long = 0
    var cookie: String = ""
    var sign: Status = Status.OFF
}

interface MiHoYoRepository: ReactiveMongoRepository<MiHoYoEntity, String> {

    fun findByTgId(tgId: Long): Mono<MiHoYoEntity>

    fun findBySign(sign: Status): Flux<MiHoYoEntity>

}

@Service
class MiHoYoService(
    private val miHoYoRepository: MiHoYoRepository
) {

    suspend fun findByTgId(tgId: Long) = miHoYoRepository.findByTgId(tgId).awaitSingleOrNull()

    suspend fun findBySign(sign: Status): List<MiHoYoEntity> = miHoYoRepository.findBySign(sign).collectList().awaitSingle()

    suspend fun save(miHoYoEntity: MiHoYoEntity): MiHoYoEntity = miHoYoRepository.save(miHoYoEntity).awaitSingle()

    suspend fun findAll(): List<MiHoYoEntity> = miHoYoRepository.findAll().collectList().awaitSingle()

}