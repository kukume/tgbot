package me.kuku.telegram.entity

import kotlinx.coroutines.flow.toList
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Document("net_ease")
class NetEaseEntity {
    @Id
    var id: String? = null
    var tgId: Long = 0
    var musicU: String = ""
    var csrf: String = ""
    var sign: Status = Status.OFF
    var musicianSign: Status = Status.OFF
    var vipSign: Status = Status.OFF

    fun cookie() = "channel=netease; __remember_me=true; MUSIC_U=$musicU; __csrf=$csrf; "

    fun pcCookie() = "os=pc; ${cookie()}"

    fun androidCookie() = "os=android; ${cookie()}"
}

@Suppress("SpringDataRepositoryMethodReturnTypeInspection")
interface NetEaseRepository: CoroutineCrudRepository<NetEaseEntity, String> {

    suspend fun findByTgId(tgId: Long): NetEaseEntity?

    suspend fun findBySign(sign: Status): List<NetEaseEntity>

    suspend fun findByMusicianSign(musicianSign: Status): List<NetEaseEntity>

    suspend fun deleteByTgId(tgId: Long)

    suspend fun findByVipSign(status: Status): List<NetEaseEntity>

}

@Service
class NetEaseService(
    private val netEaseRepository: NetEaseRepository
) {

    suspend fun findByTgId(tgId: Long) = netEaseRepository.findByTgId(tgId)

    suspend fun findBySign(sign: Status): List<NetEaseEntity> = netEaseRepository.findBySign(sign)

    suspend fun findByMusicianSign(musicianSign: Status): List<NetEaseEntity> = netEaseRepository.findByMusicianSign(musicianSign)

    suspend fun save(netEaseEntity: NetEaseEntity): NetEaseEntity = netEaseRepository.save(netEaseEntity)

    suspend fun findAll(netEaseEntity: NetEaseEntity): List<NetEaseEntity> = netEaseRepository.findAll().toList()

    @Transactional
    suspend fun deleteByTgId(tgId: Long) = netEaseRepository.deleteByTgId(tgId)

    suspend fun findByVipSign(status: Status) = netEaseRepository.findByVipSign(status)

}
