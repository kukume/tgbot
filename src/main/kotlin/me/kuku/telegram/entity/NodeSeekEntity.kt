package me.kuku.telegram.entity

import kotlinx.coroutines.flow.toList
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service

@Document("node_seek")
class NodeSeekEntity: BaseEntity() {
    @Id
    var id: String? = null
    var cookie: String = ""
    var sign: Sign = Sign.None

    enum class Sign(val value: String) {
        Random("随机"), Fix("固定"), None("关闭")
    }

}

@Suppress("SpringDataRepositoryMethodReturnTypeInspection")
interface NodeSeekRepository: CoroutineCrudRepository<NodeSeekEntity, String> {

    suspend fun findByTgId(tgId: Long): NodeSeekEntity?

    suspend fun deleteByTgId(tgId: Long)

}

@Service
class NodeSeekService(
    private val nodeSeekRepository: NodeSeekRepository
) {

    suspend fun save(entity: NodeSeekEntity) = nodeSeekRepository.save(entity)

    suspend fun findByTgId(tgId: Long) = nodeSeekRepository.findByTgId(tgId)

    suspend fun deleteByTgId(tgId: Long) = nodeSeekRepository.deleteByTgId(tgId)

    suspend fun findAll() = nodeSeekRepository.findAll().toList()

}
