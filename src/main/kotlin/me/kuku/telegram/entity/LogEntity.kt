package me.kuku.telegram.entity

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import com.pengrad.telegrambot.request.SendMessage
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.kuku.telegram.config.telegramBot
import me.kuku.telegram.context.asyncExecute
import me.kuku.telegram.mongoDatabase
import org.bson.types.ObjectId
import java.time.LocalDateTime
import java.time.ZoneId

val logCollection = mongoDatabase.getCollection<LogEntity>("log")

@Serializable
class LogEntity: BaseEntity() {
    @Contextual
    @SerialName("_id")
    var id: ObjectId? = null
    var type: LogType = LogType.None
    var text: String = "成功"
    var show: String = ""
    var errReason: String = ""
    var exceptionStack: String = ""

    suspend fun sendFailMessage(message: String? = null) {
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

object LogService {

    suspend fun findByTgId(tgId: Long) = logCollection.find(eq("tgId", tgId)).toList()

    suspend fun findByType(logType: LogType) = logCollection.find(eq("type", logType)).toList()

    suspend fun save(logEntity: LogEntity) = logCollection.save(logEntity)

    suspend fun findByCreateTimeBetween(before: LocalDateTime, after: LocalDateTime) =
        logCollection.find(Filters.and(Filters.gte("createTime", before), Filters.lte("createTime", after)))
            .toList()

    suspend fun findByCreateTimeBetweenAndTgId(before: LocalDateTime, after: LocalDateTime, tgId: Long): List<LogEntity> =
        logCollection.find(Filters.and(
            Filters.gte("createTime", before.atZone(ZoneId.systemDefault()).toInstant()),
            Filters.lte("createTime", after.atZone(ZoneId.systemDefault()).toInstant()),
            eq("tgId", tgId),
            eq("tgName", tgId.tgName()),
        )).toList()

    suspend fun findById(id: ObjectId) = logCollection.find(eq(id)).firstOrNull()

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
