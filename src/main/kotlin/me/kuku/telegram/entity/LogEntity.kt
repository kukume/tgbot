package me.kuku.telegram.entity

import kotlinx.coroutines.reactor.awaitSingle
import me.kuku.telegram.config.TelegramBot
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import reactor.core.publisher.Flux
import java.time.LocalDateTime

@Document("log")
class LogEntity: BaseEntity() {
    var id: String? = null
    @Indexed(name = "tgId")
    var tgId: Long = 0
    var type: LogType = LogType.None
    var text: String = ""

    fun sendFailMessage(telegramBot: TelegramBot) {
        val sendMessage = SendMessage()
        sendMessage.chatId = tgId.toString()
        sendMessage.text = "#自动签到失败提醒\n${type.value}自动签到失败，请手动执行以查看原因"
        telegramBot.execute(sendMessage)
    }
}


enum class LogType(val value: String) {
    None("无"),
    Baidu("百度"),
    BiliBili("哔哩哔哩"),
    HostLoc("HostLoc"),
    KuGou("酷狗"),
    GenShin("原神"),
    NetEase("网易云"),
    NetEaseMusician("网易云音乐人"),
    Step("修改步数"),
    Weibo("微博"),
    DouYu("斗鱼")
}

interface LogRepository: ReactiveMongoRepository<LogEntity, Int> {
    fun findByTgId(tgId: Long): Flux<LogEntity>
    fun findByType(logType: LogType): Flux<LogEntity>
    fun findByCreateTimeBetween(before: LocalDateTime, after: LocalDateTime): Flux<LogEntity>

    fun findByCreateTimeBetweenAndTgId(before: LocalDateTime, after: LocalDateTime, tgId: Long): Flux<LogEntity>
}

@Service
class LogService(
    private val logRepository: LogRepository
) {

    suspend fun findByTgId(tgId: Long): List<LogEntity> = logRepository.findByTgId(tgId).collectList().awaitSingle()

    suspend fun findByType(logType: LogType): List<LogEntity> = logRepository.findByType(logType).collectList().awaitSingle()

    suspend fun save(logEntity: LogEntity): LogEntity = logRepository.save(logEntity).awaitSingle()

    suspend fun findByCreateTimeBetween(before: LocalDateTime, after: LocalDateTime): List<LogEntity> = logRepository.findByCreateTimeBetween(before, after).collectList().awaitSingle()

    suspend fun findByCreateTimeBetweenAndTgId(before: LocalDateTime, after: LocalDateTime, tgId: Long): List<LogEntity> =
        logRepository.findByCreateTimeBetweenAndTgId(before, after, tgId).collectList().awaitSingle()
}
