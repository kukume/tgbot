package me.kuku.telegram.entity

import kotlinx.coroutines.flow.toList
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service
import java.io.Serializable

@Suppress("ConstPropertyName")
@Document("oci")
class OciEntity: Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
    @Id
    var id: String? = null
    var tgId: Long = 0
    var tenantId: String = ""
    @Indexed(unique = true)
    var remark: String = ""
    var userid: String = ""
    var fingerprint: String = ""
    var privateKey: String = ""
    var region: String = ""
}

class OciTask

interface OciRepository: CoroutineCrudRepository<OciEntity, String> {

    suspend fun findByTgId(tgId: Long): List<OciEntity>

    suspend fun findByRemarkAndTgId(remark: String, tgId: Long): OciEntity?

}

@Service
class OciService(
    private val ociRepository: OciRepository
) {

    suspend fun findByTgId(tgId: Long) = ociRepository.findByTgId(tgId)

    suspend fun save(entity: OciEntity) = ociRepository.save(entity)

    suspend fun deleteById(id: String) = ociRepository.deleteById(id)

    suspend fun findById(id: String) = ociRepository.findById(id)

    suspend fun findAll() = ociRepository.findAll().toList()

    suspend fun checkRemark(remark: String, tgId: Long): Boolean {
        val entity = ociRepository.findByRemarkAndTgId(remark, tgId)
        return entity == null || entity.remark == remark
    }

}
