package me.kuku.telegram.entity

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendMessage
import me.kuku.telegram.context.asyncExecute
import me.kuku.telegram.utils.SpringUtils
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Document("log")
class LogEntity: BaseEntity() {
    var id: String? = null
    var type: LogType = LogType.None
    var text: String = "成功"
    var show: String = ""
    var errReason: String = ""
    var exceptionStack: String = ""

    suspend fun sendFailMessage(message: String? = null) {
        val telegramBot = SpringUtils.getBean<TelegramBot>()
        val sendMessage = SendMessage(tgId, "#自动签到失败提醒\n${type.value}执行失败，${message ?: "未知异常原因，请重新执行指令以查看原因"}")
        telegramBot.asyncExecute(sendMessage)
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
    Mys("米游社"),
    NetEase("网易云"),
    NetEaseMusician("网易云音乐人"),
    NetEaseVip("网易云音乐vip"),
    Step("修改步数"),
    Weibo("微博"),
    DouYu("斗鱼"),
    SmZdm("什么值得买"),
    AliDrive("阿里云盘"),
    AliDriver("阿里云盘"),
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

interface LogRepository: CoroutineCrudRepository<LogEntity, String> {
    suspend fun findByTgId(tgId: Long): List<LogEntity>
    suspend fun findByTgIdAndTgName(tgId: Long, tgName: String?): List<LogEntity>
    suspend fun findByType(logType: LogType): List<LogEntity>
    suspend fun findByCreateTimeBetween(before: LocalDateTime, after: LocalDateTime): List<LogEntity>

    suspend fun findByCreateTimeBetweenAndTgId(before: LocalDateTime, after: LocalDateTime, tgId: Long): List<LogEntity>

    suspend fun findByCreateTimeBetweenAndTgIdAndTgName(before: LocalDateTime, after: LocalDateTime, tgId: Long, tgName: String?): List<LogEntity>
}

@Service
class LogService(
    private val logRepository: LogRepository
) {

    suspend fun findByTgId(tgId: Long): List<LogEntity> = logRepository.findByTgId(tgId)

    suspend fun findByType(logType: LogType): List<LogEntity> = logRepository.findByType(logType)

    suspend fun save(logEntity: LogEntity): LogEntity = logRepository.save(logEntity)

    suspend fun findByCreateTimeBetween(before: LocalDateTime, after: LocalDateTime): List<LogEntity> = logRepository.findByCreateTimeBetween(before, after)

    suspend fun findByCreateTimeBetweenAndTgId(before: LocalDateTime, after: LocalDateTime, tgId: Long): List<LogEntity> =
        logRepository.findByCreateTimeBetweenAndTgIdAndTgName(before, after, tgId, tgId.tgName())

    suspend fun findById(id: String) = logRepository.findById(id)

    suspend fun log(entity: BaseEntity, type: LogType, block: suspend LogEntity.() -> Unit) {
        val logEntity = LogEntity().also {
            it.tgId = entity.tgId
            it.tgName = entity.tgName
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
