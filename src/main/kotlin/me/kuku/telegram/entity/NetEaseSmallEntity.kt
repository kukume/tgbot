package me.kuku.telegram.entity

import kotlinx.coroutines.flow.toList
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

@Document("net_ease_small")
class NetEaseSmallEntity: NetEaseBaseEntity() {
    @Id
    var id: String? = null
    var username: String = ""
    var password: String = ""
    var status: Status = Status.OFF
    var reason: String = ""

    fun oneDayAgo() = ChronoUnit.DAYS.between(LocalDateTime.now(), super.updateTime).absoluteValue >= 1
}

interface NetEaseSmallRepository: CoroutineCrudRepository<NetEaseSmallEntity, String> {
    suspend fun findByUsername(username: String): NetEaseSmallEntity?
    suspend fun findByStatus(status: Status): List<NetEaseSmallEntity>
}

@Service
class NetEaseSmallService(
    private val netEaseSmallRepository: NetEaseSmallRepository
) {
    suspend fun save(netEaseSmallEntity: NetEaseSmallEntity): NetEaseSmallEntity = netEaseSmallRepository.save(netEaseSmallEntity)

    suspend fun findByUsername(username: String): NetEaseSmallEntity? = netEaseSmallRepository.findByUsername(username)

    suspend fun findAll(): List<NetEaseSmallEntity> = netEaseSmallRepository.findAll().toList()

    suspend fun delete(entity: NetEaseSmallEntity) = netEaseSmallRepository.delete(entity)

    suspend fun findByStatus(status: Status) = netEaseSmallRepository.findByStatus(status)
}