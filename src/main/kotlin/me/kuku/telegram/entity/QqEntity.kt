@file:Suppress("SpringDataRepositoryMethodReturnTypeInspection", "FunctionName")

package me.kuku.telegram.entity

import kotlinx.coroutines.flow.toList
import me.kuku.telegram.logic.QqGroupEssenceMessage
import me.kuku.utils.MyUtils
import me.kuku.utils.md5
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service
import java.util.function.Consumer

@Suppress("MemberVisibilityCanBePrivate")
@Document("qq")
@CompoundIndexes(
    CompoundIndex(name = "tgId_domain_idx", def = "{'tgId': 1, 'domain': 1}", unique = true)
)
class QqEntity {
    @Id
    var id: String? = null
    var tgId: Long = 0
    var cookie: String = ""
        set(value) {
            val sKey = MyUtils.regex("skey=", ";", value) ?: error("cookie无效")
            this.sKey = sKey
            val psKey = MyUtils.regex("p_skey=", ";", value) ?: error("cookie无效")
            this.psKey = psKey
            field = value
        }
    var domain: String = ""
    var sKey: String = ""
    var psKey: String = ""

    private fun getGtk(key: String): Long {
        val len = key.length
        var hash = 5381L
        for (i in 0 until len) {
            hash += (hash shl 5 and 2147483647) + key[i].code and 2147483647
            hash = hash and 2147483647
        }
        return hash and 2147483647
    }

    private fun getGtk2(key: String): String {
        var salt: Long = 5381
        val md5key = "tencentQQVIP123443safde&!%^%1282"
        val hash: MutableList<Long> = ArrayList()
        hash.add(salt shl 5)
        val len = key.length
        for (i in 0 until len) {
            val ascCode = Integer.toHexString(key[i].code)
            val code = Integer.valueOf(ascCode, 16).toLong()
            hash.add((salt shl 5) + code)
            salt = code
        }
        val sb = StringBuilder()
        hash.forEach(Consumer { obj: Long? -> sb.append(obj) })
        return (sb.toString() + md5key).md5()
    }

    fun getSKeyGtk() = getGtk(sKey)
    fun getSKeyGtk2() = getGtk2(sKey)
    fun getPsKeyGtk() = getGtk(psKey)
    fun getPsKeyGtk2() = getGtk2(psKey)

}

interface QqRepository: CoroutineCrudRepository<QqEntity, String> {

    suspend fun findByTgIdAndDomain(tgId: Long, domain: String): QqEntity?

    suspend fun deleteByTgId(tgId: Long)


}

@Service
class QqService(
    private val qqRepository: QqRepository
) {
    suspend fun findByTgIdAndDomain(tgId: Long, domain: String) = qqRepository.findByTgIdAndDomain(tgId, domain)

    suspend fun save(entity: QqEntity) = qqRepository.save(entity)

    suspend fun deleteByTgId(tgId: Long) = qqRepository.deleteByTgId(tgId)

}


@Document("essence")
@CompoundIndexes(
    CompoundIndex(name = "chatId_messageThreadId_group_idx", def = "{'chatId': 1, 'messageThreadId': 1, 'group': 1}", unique = true)
)
class EssenceEntity {
    @Id
    var id: String? = null
    var chatId: Long = 0
    var messageThreadId: Int? = null
    var group: Long = 0
    var messages: MutableList<QqGroupEssenceMessage> = mutableListOf()
}


interface EssenceRepository: CoroutineCrudRepository<EssenceEntity, String> {

    suspend fun findByChatIdAndMessageThreadIdAndGroup(chatId: Long, messageThreadId: Int?, group: Long): EssenceEntity?

    suspend fun findByChatIdAndMessageThreadId(chatId: Long, messageThreadId: Int?): List<EssenceEntity>

    suspend fun deleteByChatIdAndMessageThreadIdAndGroup(chatId: Long, messageThreadId: Int?, group: Long)

}

@Service
class EssenceService(
    private val essenceRepository: EssenceRepository
) {

    suspend fun findByChatIdAndMessageThreadIdAndGroup(chatId: Long, messageThreadId: Int?, group: Long) =
        essenceRepository.findByChatIdAndMessageThreadIdAndGroup(chatId, messageThreadId, group)

    suspend fun save(entity: EssenceEntity) = essenceRepository.save(entity)

    suspend fun findByChatIdAndMessageThreadId(chatId: Long, messageThreadId: Int?) =
        essenceRepository.findByChatIdAndMessageThreadId(chatId, messageThreadId)

    suspend fun findById(id: String) = essenceRepository.findById(id)

    suspend fun deleteById(id: String) = essenceRepository.deleteById(id)

    suspend fun findAll() = essenceRepository.findAll().toList()

    suspend fun deleteByChatIdAndMessageThreadIdAndGroup(chatId: Long, messageThreadId: Int?, group: Long) =
        essenceRepository.deleteByChatIdAndMessageThreadIdAndGroup(chatId, messageThreadId, group)

}
