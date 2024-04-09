package me.kuku.telegram.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Document("weibo")
class WeiboEntity: BaseEntity() {
    @Id
    var id: String? = null
    var cookie: String = ""
    var push: Status = Status.OFF
    var sign: Status = Status.OFF
}

interface WeiboRepository: CoroutineCrudRepository<WeiboEntity, String> {

    suspend fun findByTgIdAndTgName(tgId: Long, tgName: String?): WeiboEntity?

    suspend fun findByPush(push: Status): List<WeiboEntity>

    suspend fun findBySign(sign: Status): List<WeiboEntity>

    suspend fun deleteByTgIdAndTgName(tgId: Long, tgName: String?)

}

@Service
class WeiboService(
    private val weiboRepository: WeiboRepository
) {

    suspend fun findByTgId(tgId: Long) = weiboRepository.findEnableEntityByTgId(tgId) as? WeiboEntity

    suspend fun findByPush(push: Status): List<WeiboEntity> = weiboRepository.findByPush(push)

    suspend fun findBySign(sign: Status): List<WeiboEntity> = weiboRepository.findBySign(sign)

    suspend fun save(weiboEntity: WeiboEntity): WeiboEntity = weiboRepository.save(weiboEntity)

    @Transactional
    suspend fun deleteByTgId(tgId: Long) = weiboRepository.deleteEnableEntityByTgId(tgId)

}