package me.kuku.telegram.scheduled

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.AliDriveLogic
import me.kuku.utils.MyUtils
import me.kuku.utils.client
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@Component
class AliDriveScheduled(
    private val aliDriveLogic: AliDriveLogic,
    private val aliDriveService: AliDriveService,
    private val logService: LogService
) {

    @Scheduled(cron = "13 9 4 * * ?")
    suspend fun sign() {
        val list = aliDriveService.findBySign(Status.ON)
        for (aliDriveEntity in list) {
            logService.log(aliDriveEntity.tgId, LogType.AliDrive) {
                delay(3000)
                aliDriveLogic.sign(aliDriveEntity)
                if (aliDriveEntity.receive == Status.ON) {
                    text = aliDriveLogic.receive(aliDriveEntity)
                }
            }
        }
    }

    @Scheduled(cron = "41 4 2 * * ?")
    suspend fun pk() {
        val list = aliDriveService.findByJoinTeam(Status.ON)
        for (aliDriveEntity in list) {
            kotlin.runCatching {
                val team = aliDriveLogic.queryTeam(aliDriveEntity)
                if (team.joinTeam.isEmpty()) {
                    aliDriveLogic.joinTeam(aliDriveEntity, team.id)
                }
            }
        }
    }

    @Scheduled(cron = "10 4 5 * * ?")
    suspend fun lastDayReceive() {
        val now = LocalDate.now()
        val nextMonth = now.plusMonths(1)
        val lastDay = LocalDate.of(nextMonth.year, nextMonth.month, 1).minusDays(1)
        if (now.dayOfMonth == lastDay.dayOfMonth) {
            val list = aliDriveService.findBySign(Status.ON)
            for (aliDriveEntity in list) {
                val signList = aliDriveLogic.sign(aliDriveEntity)
                for (signInLog in signList.signInLogs) {
                    if (!signInLog.isReward) {
                        aliDriveLogic.receive(aliDriveEntity, signInLog.day)
                    }
                }
            }
        }
    }

    @Scheduled(cron = "43 4 3 * * ?")
    suspend fun task() {
        val list = aliDriveService.findByTask(Status.ON)
        for (aliDriveEntity in list) {
            logService.log(aliDriveEntity.tgId, LogType.AliDrive) {
                val signInInfo = aliDriveLogic.signInInfo(aliDriveEntity)
                val reward = signInInfo.rewards[1]
                when (reward.remind) {
                    "创建一个手工相册即可领取奖励" -> {
                        val album = aliDriveLogic.createAlbum(aliDriveEntity, "kuku的创建相册任务")
                        aliDriveLogic.deleteAlbum(aliDriveEntity, album.id)
                    }
                    "上传10个文件到备份盘即可领取奖励" -> {
                        val userGet = aliDriveLogic.userGet(aliDriveEntity)
                        val backupDriveId = userGet.backupDriveId
                        val searchFile = aliDriveLogic.searchFile(aliDriveEntity, "kuku的上传文件任务", listOf(backupDriveId.toString()))
                        val fileId = if (searchFile.isEmpty())
                            aliDriveLogic.createFolder(aliDriveEntity, backupDriveId, "kuku的上传文件任务").fileId
                        else searchFile[0].fileId
                        repeat(10) {
                            delay(3000)
                            val bytes = picture()
                            aliDriveLogic.uploadFileToBackupDrive(aliDriveEntity, backupDriveId,
                                "${MyUtils.random(10)}.jpg", bytes, fileId)
                        }
                    }
                    "备份10张照片到相册即可领取奖励" -> {
                        val albumsDriveId = aliDriveLogic.albumsDriveId(aliDriveEntity)
                        val albumList = aliDriveLogic.albumList(aliDriveEntity)
                        val findAlbum = albumList.find { it.name == "kuku的上传图片任务" }
                        val id = findAlbum?.id ?: aliDriveLogic.createAlbum(aliDriveEntity, "kuku的上传图片任务").id
                        repeat(10) {
                            delay(3000)
                            val bytes = picture()
                            val complete = aliDriveLogic.uploadFileToAlbums(
                                aliDriveEntity,
                                albumsDriveId,
                                "${MyUtils.random(10)}.jpg",
                                bytes
                            )
                            aliDriveLogic.addFileToAlbum(aliDriveEntity, albumsDriveId, complete.fileId, id)
                        }
                    }
                    "接3次好运瓶" -> {
                        repeat(3) {
                            delay(3000)
                            aliDriveLogic.bottleFish(aliDriveEntity)
                        }
                    }
                    "播放1个视频30秒即可领取奖励" -> {
                        val userGet = aliDriveLogic.userGet(aliDriveEntity)
                        val backupDriveId = userGet.backupDriveId
                        val searchFile = aliDriveLogic.searchFile(aliDriveEntity, "kuku的视频", listOf(backupDriveId.toString()))
                        val fileId = if (searchFile.isEmpty())
                            aliDriveLogic.createFolder(aliDriveEntity, backupDriveId, "kuku的视频").fileId
                        else searchFile[0].fileId
                        val bytes = client.get("https://minio.kuku.me/kuku/BV14s4y1Z7ZAoutput.mp4").body<ByteArray>()
                        val uploadComplete = aliDriveLogic.uploadFileToBackupDrive(
                            aliDriveEntity, backupDriveId,
                            "BV14s4y1Z7ZAoutput.mp4", bytes, fileId
                        )
                        val uploadFileId = uploadComplete.fileId
                        val uploadDriveId = uploadComplete.driveId
                        val videoInfo = aliDriveLogic.videoInfo(aliDriveEntity, uploadDriveId, uploadFileId)
                        aliDriveLogic.videoUpdate(aliDriveEntity, uploadDriveId, uploadFileId, videoInfo.videoPreviewPlayInfo.meta.duration,
                            50.123)
                    }
                    "创建共享相簿邀请成员加入并上传10张照片" -> {
                        val albumList = aliDriveLogic.albumList(aliDriveEntity)
                        val find = albumList.find { it.name == "kuku的共享相册任务" }
                        val id = find?.id ?: aliDriveLogic.createShareAlbum(aliDriveEntity, "kuku的共享相册任务")
                        aliDriveLogic.shareAlbumInvite(aliDriveEntity, id)
                        repeat(10) {
                            delay(3000)
                            val bytes = picture()
                            aliDriveLogic.uploadFileToShareAlbum(aliDriveEntity, id, "${MyUtils.random(6)}.jpg", bytes)
                        }
                    }
                    else -> error("不支持的任务，${reward.remind}")
                }
            }

        }
    }

    private suspend fun picture(): ByteArray {
        var hour = MyUtils.randomInt(0, 23).toString()
        if (hour.length == 1) hour = "0$hour"
        var minute = MyUtils.randomInt(0, 59).toString()
        if (minute.length == 1) minute = "0$minute"
        val pictureUrl = "https://minio.kuku.me/kuku/time/$hour/$hour-$minute.jpg"
        return client.get(pictureUrl).body<ByteArray>()
    }

}
