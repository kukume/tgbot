package me.kuku.telegram.entity

import kotlinx.coroutines.flow.toList
import me.kuku.telegram.logic.MiHoYoFix
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Document("mi_ho_yo")
class MiHoYoEntity: BaseEntity() {
    @Id
    var id: String? = null
    var fix: MiHoYoFix = MiHoYoFix()
    var aid: String = ""
    var mid: String = ""
    var cookie: String = ""
    var token: String = ""
    var sToken: String = ""
    var ticket: String = ""
    var sign: Status = Status.OFF
    var mysSign: Status = Status.OFF

    fun hubCookie(): String {
        if (sToken.isEmpty()) error("未设置sToken，请使用账号密码重新登录")
        return "stuid=$aid; stoken=$sToken; "
    }
}

@Suppress("SpringDataRepositoryMethodReturnTypeInspection")
interface MiHoYoRepository: CoroutineCrudRepository<MiHoYoEntity, String> {

    suspend fun findByTgId(tgId: Long): MiHoYoEntity?

    suspend fun findBySign(sign: Status): List<MiHoYoEntity>

    suspend fun deleteByTgId(tgId: Long)

    suspend fun findByMysSign(sign: Status): List<MiHoYoEntity>

}

@Service
class MiHoYoService(
    private val miHoYoRepository: MiHoYoRepository
) {

    suspend fun findByTgId(tgId: Long) = miHoYoRepository.findByTgId(tgId)

    suspend fun findBySign(sign: Status): List<MiHoYoEntity> = miHoYoRepository.findBySign(sign)

    suspend fun findByMysSign(sign: Status): List<MiHoYoEntity> = miHoYoRepository.findByMysSign(sign)

    suspend fun save(miHoYoEntity: MiHoYoEntity): MiHoYoEntity = miHoYoRepository.save(miHoYoEntity)

    suspend fun findAll(): List<MiHoYoEntity> = miHoYoRepository.findAll().toList()

    @Transactional
    suspend fun deleteByTgId(tgId: Long) = miHoYoRepository.deleteByTgId(tgId)

}