package me.kuku.telegram.entity

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.toList
import me.kuku.telegram.mongoDatabase

val linuxDoCollection = mongoDatabase.getCollection<LinuxDoEntity>("linux_do")

class LinuxDoEntity: BaseEntity() {
    var id: String? = null
    var cookie: String = ""
    var sign: Status = Status.OFF
}

object LinuxDoService {

    suspend fun findByTgId(tgId: Long): LinuxDoEntity? = TODO()

    suspend fun save(linuxDoEntity: LinuxDoEntity) = linuxDoCollection.save(linuxDoEntity)

    suspend fun deleteByTgId(tgId: Long) = Unit

    suspend fun findBySign(sign: Status) = linuxDoCollection.find(eq("sign", sign)).toList()

}