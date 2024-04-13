package me.kuku.telegram.entity

import kotlinx.coroutines.flow.toList
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Document("config")
class ConfigEntity: BaseEntity() {
    @Id
    var id: String? = null
    // 正能量推送
    var positiveEnergy: Status = Status.OFF
    var rrOcrKey: String = ""
    var twoCaptchaKey: String = ""
    var v2exPush: Status = Status.OFF
    var xianBaoPush: Status = Status.OFF // http://new.xianbao.fun/
    var epicFreeGamePush: Status = Status.OFF

    fun twoCaptchaKey() = twoCaptchaKey.ifEmpty { null }
    fun rrOcrKey() = rrOcrKey.ifEmpty { null }
}

interface ConfigRepository: CoroutineCrudRepository<ConfigEntity, String> {
    suspend fun findByTgId(tgId: Long): List<ConfigEntity>
    suspend fun findByTgIdAndTgName(tgId: Long, tgName: String?): ConfigEntity?
    suspend fun findByPositiveEnergy(positiveEnergy: Status): List<ConfigEntity>
    suspend fun findByV2exPush(v2exPush: Status): List<ConfigEntity>
    suspend fun findByXianBaoPush(push: Status): List<ConfigEntity>
    suspend fun findByEpicFreeGamePush(push: Status): List<ConfigEntity>

}

@Service
class ConfigService(
    private val configRepository: ConfigRepository
) {

    suspend fun save(configEntity: ConfigEntity): ConfigEntity = configRepository.save(configEntity)

    @Transactional
    suspend fun findByTgId(tgId: Long): ConfigEntity? {
        val list = configRepository.findByTgId(tgId)
        if (list.size > 1) {
            val subList = list.subList(1, list.size)
            configRepository.deleteAll(subList)
        }
        return list.firstOrNull()
    }

    suspend fun findAll(): List<ConfigEntity> = configRepository.findAll().toList()

    suspend fun findByPositiveEnergy(positiveEnergy: Status): List<ConfigEntity> = configRepository.findByPositiveEnergy(positiveEnergy)
    suspend fun findByV2exPush(v2exPush: Status): List<ConfigEntity> = configRepository.findByV2exPush(v2exPush)
    suspend fun findByXianBaoPush(push: Status): List<ConfigEntity> = configRepository.findByXianBaoPush(push)

    suspend fun findByEpicFreeGamePush(push: Status) = configRepository.findByEpicFreeGamePush(push)

}
