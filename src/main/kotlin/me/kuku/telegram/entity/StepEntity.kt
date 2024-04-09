package me.kuku.telegram.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Document("step")
class StepEntity: BaseEntity() {
    @Id
    var id: String? = null
    var leXinCookie: String = ""
    var leXinUserid: String = ""
    var leXinAccessToken: String = ""
    var miLoginToken: String = ""
    var step: Int = -1
    var offset: Status = Status.OFF
}

interface StepRepository: CoroutineCrudRepository<StepEntity, String> {

    suspend fun findByTgIdAndTgName(tgId: Long, tgName: String?): StepEntity?

    suspend fun findByStepIsGreaterThan(step: Int): List<StepEntity>

    suspend fun deleteByTgIdAndTgName(tgId: Long, tgName: String?)

}

@Service
class StepService(
    private val stepRepository: StepRepository
) {

    suspend fun findByTgId(tgId: Long) = stepRepository.findEnableEntityByTgId(tgId) as? StepEntity

    suspend fun findByAuto(): List<StepEntity> = stepRepository.findByStepIsGreaterThan(0)

    suspend fun save(stepEntity: StepEntity): StepEntity = stepRepository.save(stepEntity)

    @Transactional
    suspend fun deleteByTgId(tgId: Long) = stepRepository.deleteEnableEntityByTgId(tgId)

}