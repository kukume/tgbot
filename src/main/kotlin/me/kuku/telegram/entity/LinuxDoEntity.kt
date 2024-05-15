package me.kuku.telegram.entity

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Document("linux_do")
class LinuxDoEntity: BaseEntity() {
    var id: String? = null
    var cookie: String = ""
    var sign: Status = Status.OFF
}

interface LinuxDoRepository: CoroutineCrudMultipleRepository<LinuxDoEntity, String> {
    suspend fun findBySign(sign: Status): List<LinuxDoEntity>
}

@Service
class LinuxDoService(
    private val linuxDoRepository: LinuxDoRepository
) {

    suspend fun findByTgId(tgId: Long) = linuxDoRepository.findEnableEntityByTgId(tgId) as? LinuxDoEntity

    suspend fun save(linuxDoEntity: LinuxDoEntity) = linuxDoRepository.save(linuxDoEntity)

    @Transactional
    suspend fun deleteByTgId(tgId: Long) = linuxDoRepository.deleteEnableEntityByTgId(tgId)

    suspend fun findBySign(sign: Status) = linuxDoRepository.findBySign(sign)

}