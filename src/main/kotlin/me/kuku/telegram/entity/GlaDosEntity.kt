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

@Suppress("SpringDataRepositoryMethodReturnTypeInspection")
interface GlaDosRepository: CoroutineCrudRepository<GlaDosEntity, String> {

    suspend fun findBySign(sign: Status): List<GlaDosEntity>

    suspend fun findByTgId(tgId: Long): GlaDosEntity?

}

@Service
class GlaDosService(
    private val glaDosRepository: GlaDosRepository
) {

    suspend fun save(glaDosEntity: GlaDosEntity) = glaDosRepository.save(glaDosEntity)

    suspend fun findBySign(sign: Status) = glaDosRepository.findBySign(sign)

    suspend fun findByTgId(tgId: Long) = glaDosRepository.findByTgId(tgId)

}
