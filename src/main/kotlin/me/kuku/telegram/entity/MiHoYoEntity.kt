package me.kuku.telegram.entity

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import me.kuku.telegram.logic.MiHoYoFix
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Document("mi_ho_yo")
class MiHoYoEntity: BaseEntity() {
    @Id
    var id: String? = null
    var fix: MiHoYoFix = MiHoYoFix()
    var aid: String = ""
    var mid: String = ""
    var cookie: String = ""
    var token: String = ""
    var ticket: String = ""
    var sign: Status = Status.OFF
    var mysSign: Status = Status.OFF
}

interface MiHoYoRepository: ReactiveMongoRepository<MiHoYoEntity, String> {

    fun findByTgId(tgId: Long): Mono<MiHoYoEntity>

    fun findBySign(sign: Status): Flux<MiHoYoEntity>

    fun deleteByTgId(tgId: Long): Mono<Void>

    fun findByMysSign(sign: Status): Flux<MiHoYoEntity>

}

@Service
class MiHoYoService(
    private val miHoYoRepository: MiHoYoRepository
) {

    suspend fun findByTgId(tgId: Long) = miHoYoRepository.findByTgId(tgId).awaitSingleOrNull()

    suspend fun findBySign(sign: Status): List<MiHoYoEntity> = miHoYoRepository.findBySign(sign).collectList().awaitSingle()

    suspend fun findByMysSign(sign: Status): List<MiHoYoEntity> = miHoYoRepository.findByMysSign(sign).collectList().awaitSingle()

    suspend fun save(miHoYoEntity: MiHoYoEntity): MiHoYoEntity = miHoYoRepository.save(miHoYoEntity).awaitSingle()

    suspend fun findAll(): List<MiHoYoEntity> = miHoYoRepository.findAll().collectList().awaitSingle()

    @Transactional
    suspend fun deleteByTgId(tgId: Long) = miHoYoRepository.deleteByTgId(tgId).awaitSingleOrNull()

}