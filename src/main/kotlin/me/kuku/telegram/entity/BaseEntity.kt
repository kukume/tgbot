package me.kuku.telegram.entity

import com.fasterxml.jackson.annotation.JsonFormat
import me.kuku.telegram.context.TelegramContext
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.Indexed
import java.time.LocalDateTime

open class BaseEntity {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @CreatedDate
    var createTime: LocalDateTime = LocalDateTime.now()
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @LastModifiedDate
    var updateTime: LocalDateTime = LocalDateTime.now()
    @Indexed(name = "tgId")
    open var tgId: Long = 0

    context(TelegramContext)
    inline fun <reified T: BaseEntity> init(): T{
        this@BaseEntity.tgId = this@TelegramContext.tgId
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
