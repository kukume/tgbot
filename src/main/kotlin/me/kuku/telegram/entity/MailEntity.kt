package me.kuku.telegram.entity

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Document("mail")
class MailEntity {
    @Id
    var id: String? = null
    var tgId: Long = 0
    var host: String = ""
    var port: Int = 0
    var username: String = ""
    var password: String = ""
    var type: String = "imap"
}

interface MailRepository: ReactiveMongoRepository<MailEntity, String> {

    fun findByTgId(tgId: Long): Flux<MailEntity>

    fun findByTgIdAndUsername(tgId: Long, username: String): Mono<MailEntity>

    fun deleteByTgIdAndUsername(tgId: Long, username: String): Mono<Void>

}

@Service
class MailService(
    private val mailRepository: MailRepository
) {

    suspend fun findByTgId(tgId: Long): List<MailEntity> = mailRepository.findByTgId(tgId).collectList().awaitSingle()

    suspend fun save(mailEntity: MailEntity): MailEntity = mailRepository.save(mailEntity).awaitSingle()

    suspend fun findAll(): List<MailEntity> = mailRepository.findAll().collectList().awaitSingle()

    suspend fun findByTgIdAndUsername(tgId: Long, username: String) = mailRepository.findByTgIdAndUsername(tgId, username).awaitSingleOrNull()

    suspend fun delete(mailEntity: MailEntity): Void = mailRepository.delete(mailEntity).awaitSingle()

    suspend fun deleteByTgIdAndUsername(tgId: Long, username: String) = mailRepository.deleteByTgIdAndUsername(tgId, username).awaitSingle()
}