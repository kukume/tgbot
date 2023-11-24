package me.kuku.telegram.entity

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendMessage
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import me.kuku.telegram.utils.SpringUtils
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.time.LocalDateTime

@Document("log")
class LogEntity: BaseEntity() {
    var id: String? = null
    var type: LogType = LogType.None
    var text: String = "成功"
    var show: String = ""
    var errReason: String = ""
    var exceptionStack: String = ""

    fun sendFailMessage(message: String? = null) {
        val telegramBot = SpringUtils.getBean<TelegramBot>()
        val sendMessage = SendMessage(tgId, "#自动签到失败提醒\n${type.value}执行失败，${message ?: "未知异常原因，请重新执行指令以查看原因"}")
        telegramBot.execute(sendMessage)
    }

    fun success() = text.contains("成功")
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
    DouYu("斗鱼"),
    SmZdm("什么值得买"),
    AliDrive("阿里云盘"),
    AliDriveTask("阿里云盘任务"),
    AliDriveReceiveTaskToday("阿里云盘领取当日任务奖励"),
    ALiDriveReceive("阿里云盘月末领取签到奖励"),
    ALiDriveReceiveTask("阿里云盘月末领取任务奖励"),
    AliDriveDeviceRoom("阿里云盘时光设备间"),
    AliDriveCard("阿里云盘领取补签卡"),
    NodeSeek("NodeSeek"),
    GlaDos("GlaDos"),
    Iqy("爱奇艺"),
    ECloud("天翼云盘")
}

interface LogRepository: ReactiveMongoRepository<LogEntity, String> {
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

    suspend fun findById(id: String) = logRepository.findById(id).awaitSingleOrNull()

    suspend fun log(tgId: Long, type: LogType, block: suspend LogEntity.() -> Unit) {
        val logEntity = LogEntity().also {
            it.tgId = tgId
            it.type = type
        }
        kotlin.runCatching {
            block(logEntity)
        }.onFailure {
            logEntity.text = "失败"
            logEntity.errReason = it.message ?: "未知异常原因"
            logEntity.exceptionStack = it.stackTraceToString()
            logEntity.sendFailMessage(it.message)
        }
        save(logEntity)
    }
}
