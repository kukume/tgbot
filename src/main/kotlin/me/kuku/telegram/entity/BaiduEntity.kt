package me.kuku.telegram.entity

import kotlinx.coroutines.flow.toList
import me.kuku.utils.OkUtils
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Document("baidu")
class BaiduEntity: BaseEntity() {
    @Id
    var id: String? = null
    var cookie: String = ""
    var tieBaSToken: String = ""
    var sign: Status = Status.OFF

    fun otherCookie(sToken: String): String {
        return OkUtils.cookieStr(cookie, "BDUSS") + "STOKEN=$sToken; "
    }

    fun teiBaCookie(): String {
        return otherCookie(tieBaSToken)
    }
}

interface BaiduRepository: CoroutineCrudRepository<BaiduEntity, String> {
    suspend fun findByTgIdAndTgName(tgId: Long, tgName: String?): BaiduEntity?
    suspend fun findBySign(sign: Status): List<BaiduEntity>
    suspend fun deleteByTgIdAndTgName(tgId: Long, tgName: String?)
}

@Service
class BaiduService(
    private val baiduRepository: BaiduRepository
) {

    suspend fun findByTgId(tgId: Long) = baiduRepository.findEnableEntityByTgId(tgId) as? BaiduEntity

    suspend fun save(baiduEntity: BaiduEntity) = baiduRepository.save(baiduEntity)

    suspend fun findBySign(sign: Status): List<BaiduEntity> = baiduRepository.findBySign(sign)

    suspend fun findAll(): List<BaiduEntity> = baiduRepository.findAll().toList()

    @Transactional
    suspend fun deleteByTgId(tgId: Long) = baiduRepository.deleteEnableEntityByTgId(tgId)
}