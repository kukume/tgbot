package me.kuku.telegram.entity

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Document("step")
class StepEntity {
    @Id
    var id: String? = null
    var tgId: Long = 0
    var leXinCookie: String = ""
    var leXinUserid: String = ""
    var leXinAccessToken: String = ""
    var miLoginToken: String = ""
    var step: Int = -1
    var offset: Status = Status.OFF
}

interface StepRepository: ReactiveMongoRepository<StepEntity, String> {

    fun findByTgId(tgId: Long): Mono<StepEntity>

    fun findByStepIsGreaterThan(step: Int): Flux<StepEntity>

}

@Service
class StepService(
    private val stepRepository: StepRepository
) {

    suspend fun findByTgId(tgId: Long) = stepRepository.findByTgId(tgId).awaitSingleOrNull()

    suspend fun findByAuto(): List<StepEntity> = stepRepository.findByStepIsGreaterThan(0).collectList().awaitSingle()

    suspend fun save(stepEntity: StepEntity): StepEntity = stepRepository.save(stepEntity).awaitSingle()

}