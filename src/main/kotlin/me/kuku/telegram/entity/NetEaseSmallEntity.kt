package me.kuku.telegram.entity

import kotlinx.coroutines.flow.toList
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service

@Document("net_ease_small")
class NetEaseSmallEntity {
    @Id
    var id: String? = null
    var username: String = ""
    var password: String = ""
    var musicU: String = ""
    var csrf: String = ""

    fun cookie() = "channel=netease; __remember_me=true; MUSIC_U=$musicU; __csrf=$csrf; "
}

interface NetEaseSmallRepository: CoroutineCrudRepository<NetEaseSmallEntity, String> {
    suspend fun findByUsername(username: String): NetEaseSmallEntity?
}

@Service
class NetEaseSmallService(
    private val netEaseSmallRepository: NetEaseSmallRepository
) {
    suspend fun save(netEaseSmallEntity: NetEaseSmallEntity): NetEaseSmallEntity = netEaseSmallRepository.save(netEaseSmallEntity)

    suspend fun findByUsername(username: String): NetEaseSmallEntity? = netEaseSmallRepository.findByUsername(username)

    suspend fun findAll(): List<NetEaseSmallEntity> = netEaseSmallRepository.findAll().toList()

    suspend fun delete(entity: NetEaseSmallEntity) = netEaseSmallRepository.delete(entity)
}