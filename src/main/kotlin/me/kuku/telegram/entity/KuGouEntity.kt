package me.kuku.telegram.entity

import kotlinx.coroutines.flow.toList
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Document("ku_gou")
class KuGouEntity: BaseEntity() {
    @Id
    var id: String? = null
    var token: String = ""
    var userid: Long = 0
    var kuGoo: String = ""
    var mid: String = ""
    var sign: Status = Status.OFF
}

interface KuGouRepository: CoroutineCrudRepository<KuGouEntity, String> {

    suspend fun findByTgIdAndTgName(tgId: Long, tgName: String?): KuGouEntity?

    suspend fun findBySign(sign: Status): List<KuGouEntity>

    suspend fun deleteByTgIdAndTgName(tgId: Long, tgName: String?)

}

@Service
class KuGouService(
    private val kuGouRepository: KuGouRepository
) {

    suspend fun findByTgId(tgId: Long) = kuGouRepository.findEnableEntityByTgId(tgId) as? KuGouEntity

    suspend fun findBySign(sign: Status): List<KuGouEntity> = kuGouRepository.findBySign(sign)

    suspend fun save(kuGouEntity: KuGouEntity): KuGouEntity = kuGouRepository.save(kuGouEntity)

    suspend fun findAll(): List<KuGouEntity> = kuGouRepository.findAll().toList()

    @Transactional
    suspend fun deleteByTgId(tgId: Long) = kuGouRepository.deleteEnableEntityByTgId(tgId)
}