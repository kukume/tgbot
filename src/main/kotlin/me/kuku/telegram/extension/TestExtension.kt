package me.kuku.telegram.extension

import io.ktor.client.call.*
import io.ktor.client.request.*
import me.kuku.telegram.entity.AliDriveService
import me.kuku.telegram.logic.AliDriveLogic
import me.kuku.telegram.utils.AbilitySubscriber
import me.kuku.utils.client
import org.springframework.stereotype.Component

@Component
class TestExtension(
    private val aliDriveLogic: AliDriveLogic,
    private val aliDriveService: AliDriveService
) {

    fun AbilitySubscriber.xx() {

        sub("test") {
            val aliDriveEntity = aliDriveService.findByTgId(tgId)!!
//            val userGet = aliDriveLogic.userGet(aliDriveEntity)
//            val backupDriveId = userGet.backupDriveId
//            val searchFile = aliDriveLogic.searchFile(aliDriveEntity, "kuku的视频", listOf(backupDriveId.toString()))
//            val fileId = if (searchFile.isEmpty())
//                aliDriveLogic.createFolder(aliDriveEntity, backupDriveId, "kuku的视频").fileId
//            else searchFile[0].fileId
//            val bytes = client.get("https://minio.kuku.me/kuku/BV14s4y1Z7ZAoutput.mp4").body<ByteArray>()
//            val uploadComplete = aliDriveLogic.uploadFileToBackupDrive(
//                aliDriveEntity, backupDriveId,
//                "BV14s4y1Z7ZAoutput.mp4", bytes, fileId
//            )
//            val uploadFileId = uploadComplete.fileId
//            val uploadDriveId = uploadComplete.driveId
            val uploadDriveId = 103118
            val uploadFileId = "64eb4f48fff7ce50a8b542d09507a6da928f2068"
            val videoInfo = aliDriveLogic.videoInfo(aliDriveEntity, uploadDriveId, uploadFileId)
            aliDriveLogic.videoUpdate(aliDriveEntity, uploadDriveId, uploadFileId, videoInfo.videoPreviewPlayInfo.meta.duration,
                50.123)
        }

    }

}
