package me.kuku.telegram.entity

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Document("config")
class ConfigEntity {
    @Id
    var id: String? = null
    var tgId: Long = 0
    // 正能量推送
    var positiveEnergy: Status = Status.OFF
    var rrOcrKey: String = ""
    var v2exPush: Status = Status.OFF
    var xianBaoPush: Status = Status.OFF // http://new.xianbao.fun/
}

interface ConfigRepository: ReactiveMongoRepository<ConfigEntity, String> {
    fun findByTgId(tgId: Long): Mono<ConfigEntity>
    fun findByPositiveEnergy(positiveEnergy: Status): Flux<ConfigEntity>
    fun findByV2exPush(v2exPush: Status): Flux<ConfigEntity>
    fun findByXianBaoPush(push: Status): Flux<ConfigEntity>
}

@Service
class ConfigService(
    private val configRepository: ConfigRepository
) {

    suspend fun save(configEntity: ConfigEntity): ConfigEntity = configRepository.save(configEntity).awaitSingle()

    suspend fun findByTgId(tgId: Long) = configRepository.findByTgId(tgId).awaitSingleOrNull()

    suspend fun findAll(): List<ConfigEntity> = configRepository.findAll().collectList().awaitSingle()

    suspend fun findByPositiveEnergy(positiveEnergy: Status): List<ConfigEntity> = configRepository.findByPositiveEnergy(positiveEnergy).collectList().awaitSingle()
    suspend fun findByV2exPush(v2exPush: Status): List<ConfigEntity> = configRepository.findByV2exPush(v2exPush).collectList().awaitSingle()
    suspend fun findByXianBaoPush(push: Status): List<ConfigEntity> = configRepository.findByXianBaoPush(push).collectList().awaitSingle()

}
