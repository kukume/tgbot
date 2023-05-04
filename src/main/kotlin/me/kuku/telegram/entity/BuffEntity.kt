package me.kuku.telegram.entity

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import me.kuku.telegram.logic.PaintWearInterval
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.util.UUID

@Document("buff")
class BuffEntity {
    var id: String? = null
    var tgId: Long = 0
    var cookie: String = ""
    var csrf: String = ""
    var monitors: MutableList<BuffMonitor> = mutableListOf()
}

data class BuffMonitor(
    var goodsId: Int = 0,
    var goodsName: String = "",
    var paintWearInterval: PaintWearInterval = PaintWearInterval(0.0, 0.0),
    var maxPrice: Double = 0.0,
    var payMethod: Int = 6,
    var type: BuffType = BuffType.Push,
    var id: String = UUID.randomUUID().toString().replace("-", "")
)


interface BuffRepository: ReactiveMongoRepository<BuffEntity, String> {
    fun findByTgId(tgId: Long): Mono<BuffEntity>

    fun deleteByTgId(tgId: Long): Mono<Void>
}

@Service
class BuffService(
    private val buffRepository: BuffRepository
) {

    suspend fun save(buffEntity: BuffEntity): BuffEntity = buffRepository.save(buffEntity).awaitSingle()

    suspend fun findByTgId(tgId: Long): BuffEntity? = buffRepository.findByTgId(tgId).awaitSingleOrNull()

    suspend fun findAll(): List<BuffEntity> = buffRepository.findAll().collectList().awaitSingle()

    @Transactional
    suspend fun deleteByTgId(tgId: Long) = buffRepository.deleteByTgId(tgId).awaitSingleOrNull()

}


enum class BuffType {
    Push, Buy
}
