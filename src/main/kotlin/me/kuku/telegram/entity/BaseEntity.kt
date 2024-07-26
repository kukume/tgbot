package me.kuku.telegram.entity

import com.fasterxml.jackson.annotation.JsonFormat
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import me.kuku.telegram.context.AbilityContext
import me.kuku.telegram.context.TelegramContext
import me.kuku.utils.DateTimeFormatterUtils
import java.time.LocalDateTime
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

@Serializable
open class BaseEntity {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Serializable(with = LocalDateTimeSerializer::class)
    var createTime: LocalDateTime = LocalDateTime.now()
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Serializable(with = LocalDateTimeSerializer::class)
    var updateTime: LocalDateTime = LocalDateTime.now()
    open var tgId: Long = 0
    var tgName: String? = null

    context(TelegramContext)
    suspend inline fun <reified T: BaseEntity> init(): T{
        this@BaseEntity.tgId = this@TelegramContext.tgId
        this@BaseEntity.tgName = tgId.tgName()
        return this as T
    }

    context(AbilityContext)
    suspend inline fun <reified T: BaseEntity> init(): T{
        this@BaseEntity.tgId = this@AbilityContext.tgId
        this@BaseEntity.tgName = tgId.tgName()
        return this as T
    }

}

enum class Status {
    OFF, ON;

    fun str() : String{
        return if (this.name == "ON") "√" else "×"
    }

    fun reverse(): Status {
        return if (this.name == "ON") OFF else ON
    }

    operator fun not(): Status {
        return this.reverse()
    }

    override fun toString(): String {
        return str()
    }
}

suspend fun Long.tgName(): String? {
    val findList = SwitchService.findByTgIdAndStatus(this, Status.ON)
    return  if (findList.isEmpty()) null
    else findList.first().name
}

suspend fun <T: Any> MongoCollection<T>.save(entity: T): T {
    val find = entity::class.memberProperties.find { it.name == "id" } as? KProperty1<T, *> ?: error("")
    val id = find.get(entity)
    if (id == null) this.insertOne(entity)
    else this.replaceOne(eq(find.get(entity)), entity, ReplaceOptions().upsert(true))
    return entity
}

suspend fun <T: BaseEntity> MongoCollection<T>.findEnableEntityByTgId(tgId: Long): T? {
    val name = tgId.tgName()
    return this.find(and(eq("tgId", tgId), eq("tgName", name))).firstOrNull()
}

suspend fun MongoCollection<out BaseEntity>.deleteEnableEntityByTgId(tgId: Long) {
    val name = tgId.tgName()
    this.deleteOne(and(eq("tgId", tgId), eq("tgName", name)))
}

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {

    private const val FORMATTER = "yyyy-MM-dd HH:mm:ss"

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return DateTimeFormatterUtils.parseToLocalDateTime(decoder.decodeString(), FORMATTER)
    }

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        val result = DateTimeFormatterUtils.format(value, FORMATTER)
        encoder.encodeString(result)
    }
}
