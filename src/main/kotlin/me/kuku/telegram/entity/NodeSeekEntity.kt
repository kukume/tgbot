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

interface NodeSeekRepository: CoroutineCrudRepository<NodeSeekEntity, String> {

    suspend fun findByTgIdAndTgName(tgId: Long, tgName: String?): NodeSeekEntity?

    suspend fun deleteByTgIdAndTgName(tgId: Long, tgName: String?)

}

@Service
class NodeSeekService(
    private val nodeSeekRepository: NodeSeekRepository
) {

    suspend fun save(entity: NodeSeekEntity) = nodeSeekRepository.save(entity)

    suspend fun findByTgId(tgId: Long) = nodeSeekRepository.findEnableEntityByTgId(tgId) as? NodeSeekEntity

    suspend fun deleteByTgId(tgId: Long) = nodeSeekRepository.deleteEnableEntityByTgId(tgId)

    suspend fun findAll() = nodeSeekRepository.findAll().toList()

}
