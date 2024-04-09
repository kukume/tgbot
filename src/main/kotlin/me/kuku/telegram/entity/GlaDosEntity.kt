package me.kuku.telegram.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service

@Document("glo_dos")
class GlaDosEntity: BaseEntity() {
    @Id
    var id: String? = null
    var cookie: String = ""
    var sign: Status = Status.OFF
}

interface GlaDosRepository: CoroutineCrudRepository<GlaDosEntity, String> {

    suspend fun findBySign(sign: Status): List<GlaDosEntity>

    suspend fun findByTgIdAndTgName(tgId: Long, tgName: String?): GlaDosEntity?

    suspend fun deleteByTgIdAndTgName(tgId: Long, tgName: String?)

}

@Service
class GlaDosService(
    private val glaDosRepository: GlaDosRepository
) {

    suspend fun save(glaDosEntity: GlaDosEntity) = glaDosRepository.save(glaDosEntity)

    suspend fun findBySign(sign: Status) = glaDosRepository.findBySign(sign)

    suspend fun findByTgId(tgId: Long) = glaDosRepository.findEnableEntityByTgId(tgId) as? GlaDosEntity

    suspend fun deleteByTgId(tgId: Long) = glaDosRepository.deleteEnableEntityByTgId(tgId)

}
