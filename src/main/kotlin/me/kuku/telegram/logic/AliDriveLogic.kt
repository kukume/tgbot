package me.kuku.telegram.logic

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import me.kuku.pojo.CommonResult
import me.kuku.pojo.UA
import me.kuku.telegram.entity.AliDriveEntity
import me.kuku.telegram.entity.AliDriveService
import me.kuku.utils.*
import net.consensys.cava.bytes.Bytes32
import net.consensys.cava.crypto.Hash
import net.consensys.cava.crypto.SECP256K1
import okhttp3.RequestBody.Companion.toRequestBody
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.encoders.Hex
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.security.Security
import java.time.LocalDate
import java.util.*
import javax.imageio.ImageIO

@Service
class AliDriveLogic(
    private val aliDriveService: AliDriveService
) {

    init {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    private val cache = mutableMapOf<Long, AliDriveAccessToken>()
    private val signatureCache = mutableMapOf<String, AliDriveSignature>()

    suspend fun login1(): AliDriveQrcode {
        val url = "https://passport.aliyundrive.com/mini_login.htm?lang=zh_cn&appName=aliyun_drive&appEntrance=web_default&styleType=auto&bizParams=&notLoadSsoView=false&notKeepLogin=false&isMobile=false&ad__pass__q__rememberLogin=true&ad__pass__q__rememberLoginDefaultValue=true&ad__pass__q__forgotPassword=true&ad__pass__q__licenseMargin=true&ad__pass__q__loginType=normal&hidePhoneCode=true&rnd=0.${MyUtils.randomNum(17)}"
        val html = OkHttpKtUtils.getStr(url,
            mapOf("user-agent" to UA.PC.value, "referer" to "https://auth.aliyundrive.com/"))
        val jsonStr = MyUtils.regex("window.viewData = ", ";", html)!!
        val jsonNode = Jackson.parse(jsonStr)
        val loginJsonNode = jsonNode["loginFormData"]
        val csrfToken = loginJsonNode["_csrf_token"].asText()
        val idToken = loginJsonNode["umidToken"].asText()
        val hs = loginJsonNode["hsiz"].asText()
        val qrcodeJsonNode = OkHttpKtUtils.getJson("https://passport.aliyundrive.com/newlogin/qrcode/generate.do?appName=aliyun_drive&appName=aliyun_drive&fromSite=52&fromSite=52&appEntrance=web&_csrf_token=$csrfToken&umidToken=$idToken&isMobile=false&lang=zh_CN&returnUrl=&hsiz=$hs&bizParams=&_bx-v=2.0.31",
            OkUtils.headers("", url, UA.PC))
        val contentJsonNode = qrcodeJsonNode["content"]
        val contentDataJsonNode = contentJsonNode["data"]
        return if (!contentJsonNode["success"].asBoolean()) error(contentDataJsonNode["titleMsg"].asText())
        else {
            val qrcodeUrl = contentDataJsonNode["codeContent"].asText()
            val t = contentDataJsonNode["t"].asLong()
            val ck = contentDataJsonNode["ck"].asText()
            AliDriveQrcode(qrcodeUrl, ck, csrfToken, idToken, hs, t)
        }
    }

    suspend fun login2(aliDriveQrcode: AliDriveQrcode): CommonResult<AliDriveEntity> {
        val jsonNode = OkHttpKtUtils.postJson("https://passport.aliyundrive.com/newlogin/qrcode/query.do?appName=aliyun_drive&fromSite=52&_bx-v=2.0.50",
            mapOf("t" to aliDriveQrcode.t.toString(), "ck" to aliDriveQrcode.ck, "ua" to "null", "appName" to "aliyun_drive",
                "appEntrance" to "web", "_csrf_token" to aliDriveQrcode.csrfToken, "umidToken" to aliDriveQrcode.idToken,
                "isMobile" to "false", "lang" to "zh_CN", "returnUrl" to "", "hsiz" to aliDriveQrcode.hs, "fromSite" to "52",
                "bizParams" to "", "navlanguage" to "zh-CN", "navUserAgent" to "null",
                "navPlatform" to "win32", "deviceId" to ""),  OkUtils.headers("", "",  UA.PC)
        )
        val contentJsonNode = jsonNode["content"]
        val contentDataJsonNode = contentJsonNode["data"]
        return if (!contentJsonNode["success"].asBoolean()) CommonResult.failure(contentDataJsonNode["titleMsg"].asText(), null)
        else {
            when (contentDataJsonNode["qrCodeStatus"].asText()) {
                "NEW" -> CommonResult.failure("二维码未扫描", null, 0)
                "EXPIRED" -> CommonResult.failure("二维码已失效", null, 505)
                "SCANED" -> CommonResult.failure("二维码已扫描", null, 0)
                "CANCELED" -> CommonResult.failure("二维码已失效", null, 505)
                "CONFIRMED" -> {
                    val bizExt = contentDataJsonNode["bizExt"].asText()
                    val jsonStr = Base64.getDecoder().decode(bizExt).toString(StandardCharsets.UTF_8)
                    val loginJsonNode = Jackson.parse(jsonStr)
                    val refreshToken = loginJsonNode["pds_login_result"]["refreshToken"].asText()
                    CommonResult.success(AliDriveEntity().also {
                        it.refreshToken = refreshToken
                    })
                }
                else -> CommonResult.failure("未知的状态码", null)
            }
        }
    }

    private suspend fun accessToken(aliDriveEntity: AliDriveEntity): String {
        val accessToken = cache[aliDriveEntity.tgId]
        return if (accessToken == null || accessToken.isExpire()) {
            val jsonNode = client.post("https://auth.aliyundrive.com/v2/account/token") {
                setJsonBody("""{"refresh_token": "${aliDriveEntity.refreshToken}", "grant_type": "refresh_token"}"}""")
            }.body<JsonNode>()
            if (jsonNode.has("code")) error(jsonNode["message"].asText())
            val token = "${jsonNode["token_type"].asText()} ${jsonNode["access_token"].asText()}"
            cache[aliDriveEntity.tgId] = AliDriveAccessToken(token, System.currentTimeMillis() + jsonNode["expires_in"].asLong() * 1000)
            val newRefreshToken = jsonNode["refresh_token"].asText()
            val newEntity = aliDriveService.findById(aliDriveEntity.id!!)!!
            newEntity.refreshToken = newRefreshToken
            aliDriveService.save(newEntity)
            token
        } else accessToken.accessToken
    }

    suspend fun sign(aliDriveEntity: AliDriveEntity): AliDriveSign {
        val accessToken = accessToken(aliDriveEntity)
        val jsonNode = client.post("https://member.aliyundrive.com/v1/activity/sign_in_list") {
            setJsonBody("{}")
            headers {
                append("Authorization", accessToken)
            }
        }.body<JsonNode>()
        jsonNode.check()
        val result = jsonNode["result"]
        val sign = AliDriveSign()
        sign.subject = result["subject"].asText()
        sign.customMessage = "签到成功，本月已签到${jsonNode["result"]["signInCount"].asInt()}次"
        sign.title = result["title"].asText()
        sign.isReward = result["isReward"].asBoolean()
        sign.blessing = result["blessing"].asText()
        sign.signInCount = result["signInCount"].asInt()
        for (node in result["signInLogs"]) {
            val signInLog = AliDriveSign.SignInLog()
            signInLog.day = node["day"].asInt()
            signInLog.status = node["status"].asText()
            signInLog.type = node["type"].asText()
            signInLog.rewardAmount = node["rewardAmount"].asInt()
            signInLog.themes = node["themes"].asText()
            signInLog.calendarChinese = node["calendarChinese"].asText()
            signInLog.calendarDay = node["calendarDay"].asInt()
            signInLog.calendarMonth = node["calendarMonth"].asText()
            signInLog.isReward = node["isReward"].asBoolean()
            sign.signInLogs.add(signInLog)
        }
        return sign
    }

    context(HttpRequestBuilder)
    private suspend fun AliDriveEntity.appendAuth() {
        val accessToken = accessToken(this@AliDriveEntity)
        headers {
            append("Authorization", accessToken)
        }
    }

    context(HttpRequestBuilder)
    private suspend fun AliDriveEntity.appendEncrypt(device: AliDriveDevice = AliDriveDevice()) {
        val entity = this@AliDriveEntity
        val deviceId = entity.deviceId
        val key = "${entity.tgId}$deviceId"
        val aliDriveSignature = if (signatureCache.containsKey(key)) {
            val aliDriveSignature = signatureCache[key]!!
            if (aliDriveSignature.isExpire()) {
                val encrypt = encrypt(entity, aliDriveSignature.key,
                    aliDriveSignature.deviceId, aliDriveSignature.userid, aliDriveSignature.nonce,
                    device.deviceName, device.deviceModel)
                aliDriveSignature.signature = encrypt.signature
                aliDriveSignature.expireRefresh()
                aliDriveSignature
            } else aliDriveSignature
        } else {
            val userGet = userGet(entity)
            if (entity.deviceId.isEmpty()) {
                entity.deviceId = UUID.randomUUID().toString()
                aliDriveService.save(entity)
            }
            val encryptKey = encryptKey()
            val encrypt =
                encrypt(entity, encryptKey, deviceId, userGet.userid, 0, device.deviceName, device.deviceModel)
            val aliDriveSignature = AliDriveSignature(encryptKey)
            aliDriveSignature.deviceId = deviceId
            aliDriveSignature.signature = encrypt.signature
            aliDriveSignature.userid = userGet.userid
            signatureCache[key] = aliDriveSignature
            aliDriveSignature
        }
        headers {
            append("x-device-id", aliDriveSignature.deviceId)
            append("x-signature", aliDriveSignature.signature)
        }
    }

    private fun JsonNode.check() {
        if (this["success"]?.asBoolean() != true) error("${this["code"].asText()}-${this["message"]?.asText()}")
    }

    private fun JsonNode.check2() {
        if (this.has("code")) error(this["message"].asText())
    }

    private fun JsonNode.check3() {
        if (this["code"].asInt() != 200) error(this["message"].asText())
    }

    suspend fun receive(aliDriveEntity: AliDriveEntity, day: Int = LocalDate.now().dayOfMonth): String {
        val accessToken = accessToken(aliDriveEntity)
        val jsonNode = client.post("https://member.aliyundrive.com/v1/activity/sign_in_reward?_rx-s=mobile") {
            setJsonBody("""{"signInDay": $day}""")
            headers {
                append("Authorization", accessToken)
            }
        }.body<JsonNode>()
        return if (jsonNode["success"]?.asBoolean() == true) {
            "领取成功，${jsonNode["result"]["notice"].asText()}"
        } else error(jsonNode["code"].asText())
    }

    suspend fun receiveTask(aliDriveEntity: AliDriveEntity, day: Int = LocalDate.now().dayOfMonth): String {
        val accessToken = accessToken(aliDriveEntity)
        val jsonNode = client.post("https://member.aliyundrive.com/v2/activity/sign_in_task_reward?_rx-s=mobile") {
            setJsonBody("""{"signInDay": $day}""")
            headers {
                append("Authorization", accessToken)
            }
        }.body<JsonNode>()
        jsonNode.check()
        return if (jsonNode["success"]?.asBoolean() == true) {
            "领取成功，${jsonNode["result"]["notice"].asText()}"
        } else error(jsonNode["code"].asText())
    }

    suspend fun signInInfo(aliDriveEntity: AliDriveEntity): AliDriveSignInInfo {
        val jsonNode = client.post("https://member.aliyundrive.com/v2/activity/sign_in_info") {
            setJsonBody("{}")
            aliDriveEntity.appendAuth()
            aliDriveEntity.appendEncrypt()
        }.body<JsonNode>()
        jsonNode.check()
        return jsonNode["result"].convertValue()
    }

    suspend fun queryTeam(aliDriveEntity: AliDriveEntity): AliDriveTeam {
        val jsonNode = client.post("https://member.aliyundrive.com/v1/activity/sign_in_team?_rx-s=mobile") {
            setJsonBody("")
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        jsonNode.check()
        val result = jsonNode["result"]
        return AliDriveTeam(result["id"].asInt(), result["period"].asText(), result["title"].asText(),
            result["subTitle"].asText(), result["joinTeam"].asText(), result["joinCount"].asInt(), result["endTime"].asLong())
    }

    suspend fun joinTeam(aliDriveEntity: AliDriveEntity, id: Int, team: String = "blue" /* purple */) {
        val jsonNode = client.post("https://member.aliyundrive.com/v1/activity/sign_in_team_pk?_rx-s=mobile") {
            setJsonBody("""{"id": $id, "team": "$team"}""")
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        jsonNode.check()
    }

    suspend fun albumsDriveId(aliDriveEntity: AliDriveEntity): Int {
        // code = 200
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v1/user/albums_info") {
            aliDriveEntity.appendAuth()
            setJsonBody("{}")
        }.body<JsonNode>()
        jsonNode.check3()
        return jsonNode["data"]["driveId"].asInt()
    }

    @Suppress("DuplicatedCode")
    suspend fun uploadFileToAlbums(aliDriveEntity: AliDriveEntity, driveId: Int, fileName: String, byteArray: ByteArray,
                                   scene: AliDriveScene = AliDriveScene.ManualBackup, deviceName: String = ""): AliDriveUploadComplete {
        val suffix = fileName.substring(fileName.lastIndexOf('.') + 1)
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v1/biz/albums/file/create") {
            setJsonBody("""
                {"drive_id":"$driveId","part_info_list":[{"part_number":1}],"parent_file_id":"root","name":"$fileName","type":"file","check_name_mode":"auto_rename","size":${byteArray.size},"create_scene":"${scene.value}","device_name":"$deviceName","hidden":false,"content_type":"image/$suffix"}
            """.trimIndent())
            aliDriveEntity.appendAuth()
            aliDriveEntity.appendEncrypt(findDevice(aliDriveEntity))
        }.body<JsonNode>()
        jsonNode.check2()
        val fileId = jsonNode["file_id"].asText()
        val uploadId = jsonNode["upload_id"].asText()
        val uploadUrl = jsonNode["part_info_list"][0]["upload_url"].asText()
        OkHttpKtUtils.putStr(
            uploadUrl, byteArray.toRequestBody(), mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36",
                "Origin" to "https://www.aliyundrive.com",
                "Referer" to "https://www.aliyundrive.com"
            )
        )
        val complete = client.post("https://api.aliyundrive.com/v2/file/complete") {
            setJsonBody("""
                {"drive_id":"$driveId","upload_id":"$uploadId","file_id":"$fileId"}
            """.trimIndent())
            aliDriveEntity.appendAuth()
            aliDriveEntity.appendEncrypt(findDevice(aliDriveEntity))
        }.body<JsonNode>()
        return complete.convertValue()
    }

    suspend fun albumList(aliDriveEntity: AliDriveEntity): List<AliDriveAlbum> {
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v1/album/list") {
            setJsonBody("""{"limit":20,"order_by":"created_at","order_direction":"ASC"}""")
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        jsonNode.check2()
        val list = mutableListOf<AliDriveAlbum>()
        for (node in jsonNode["items"]) {
            list.add(AliDriveAlbum().also {
                it.id = node["album_id"].asText()
                it.name = node["name"].asText()
            })
        }
        return list
    }

    suspend fun albumFileList(aliDriveEntity: AliDriveEntity, albumId: String): AliDriveFileList {
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v1/album/list_files") {
            setJsonBody("""
                {"album_id":"$albumId","image_thumbnail_process":"image/resize,w_480/format,avif","image_url_process":"image/resize,w_1920/format,avif","video_thumbnail_process":"video/snapshot,t_0,f_jpg,ar_auto,w_480","filter":"","fields":"*","limit":20,"order_by":"joined_at","order_direction":"DESC"}
            """.trimIndent())
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        jsonNode.check2()
        return jsonNode.convertValue()
    }

    suspend fun createAlbum(aliDriveEntity: AliDriveEntity, name: String): AliDriveAlbum {
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v1/album/create") {
            setJsonBody("""{"name":"$name","description":""}""")
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        jsonNode.check2()
        return AliDriveAlbum().also {
            it.id = jsonNode["album_id"].asText()
            it.name = jsonNode["name"].asText()
        }
    }

    suspend fun deleteAlbum(aliDriveEntity: AliDriveEntity, id: String) {
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v1/album/delete") {
            setJsonBody("""{"album_id":"$id"}""")
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        jsonNode.check2()
    }

    suspend fun addFileToAlbum(aliDriveEntity: AliDriveEntity, driveId: Int, fileId: String, albumId: String) {
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v1/album/add_files") {
            setJsonBody("""{"drive_file_list":[{"drive_id":"$driveId","file_id":"$fileId"}],"album_id":"$albumId"}""")
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        jsonNode.check2()
    }

    @Suppress("DuplicatedCode")
    suspend fun uploadFileToBackupDrive(aliDriveEntity: AliDriveEntity, driveId: Int, fileName: String, byteArray: ByteArray, parentId: String = "root", scene: AliDriveScene = AliDriveScene.Upload): AliDriveUploadComplete {
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v2/file/createWithFolders") {
            setJsonBody("""
                {"drive_id":"$driveId","part_info_list":[{"part_number":1}],"parent_file_id":"$parentId","name":"$fileName","type":"file","check_name_mode":"auto_rename","size":${byteArray.size},"create_scene":"${scene.value}","device_name":""}
            """.trimIndent())
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        jsonNode.check2()
        val fileId = jsonNode["file_id"].asText()
        val uploadId = jsonNode["upload_id"].asText()
        val uploadUrl = jsonNode["part_info_list"][0]["upload_url"].asText()
        OkHttpKtUtils.putStr(
            uploadUrl, byteArray.toRequestBody(), mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36",
                "Origin" to "https://www.aliyundrive.com",
                "Referer" to "https://www.aliyundrive.com"
            )
        )
        val complete = client.post("https://api.aliyundrive.com/v2/file/complete") {
            setJsonBody("""
                {"drive_id":"$driveId","upload_id":"$uploadId","file_id":"$fileId"}
            """.trimIndent())
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        return complete.convertValue()
    }

    suspend fun createFolder(aliDriveEntity: AliDriveEntity, driveId: Int, name: String, parentId: String = "root"): AliDriveFolder {
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v2/file/createWithFolders") {
            setJsonBody("""{"drive_id":"$driveId","parent_file_id":"root","name":"$name","check_name_mode":"refuse","type":"folder"}""")
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        jsonNode.check2()
        return jsonNode.convertValue()
    }

    suspend fun fileList(aliDriveEntity: AliDriveEntity, driveId: Int, parentId: String = "root"): AliDriveFileList {
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v3/file/list?jsonmask=next_marker%2Citems(name%2Cfile_id%2Cdrive_id%2Ctype%2Csize%2Ccreated_at%2Cupdated_at%2Ccategory%2Cfile_extension%2Cparent_file_id%2Cmime_type%2Cstarred%2Cthumbnail%2Curl%2Cstreams_info%2Ccontent_hash%2Cuser_tags%2Cuser_meta%2Ctrashed%2Cvideo_media_metadata%2Cvideo_preview_metadata%2Csync_meta%2Csync_device_flag%2Csync_flag%2Cpunish_flag)") {
            setJsonBody("""
                {"drive_id":"$driveId","parent_file_id":"$parentId","limit":20,"all":false,"url_expire_sec":14400,"image_thumbnail_process":"image/resize,w_256/format,avif","image_url_process":"image/resize,w_1920/format,avif","video_thumbnail_process":"video/snapshot,t_1000,f_jpg,ar_auto,w_256","fields":"*","order_by":"updated_at","order_direction":"DESC"}
            """.trimIndent())
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        jsonNode.check2()
        return jsonNode.convertValue()
    }

    private suspend fun batch(aliDriveEntity: AliDriveEntity, aliDriveBatch: AliDriveBatch, header: Map<String, String> = mapOf()) {
        val jsonNode = client.post("https://api.aliyundrive.com/v2/batch") {
            setJsonBody(aliDriveBatch)
            aliDriveEntity.appendAuth()
            headers {
                header.forEach { (k, u) -> append(k, u)  }
            }
        }.body<JsonNode>()
        jsonNode.check2()
//        val responses = jsonNode["responses"]
//        for (node in responses) {
//            val status = node["status"]
//
//        }
    }

    suspend fun batchDeleteFile(aliDriveEntity: AliDriveEntity, list: List<AliDriveBatch.DeleteFileBody>) {
        if (list.isEmpty()) return
        val batch = AliDriveBatch()
        for (deleteFileBody in list) {
            val request = AliDriveBatch.Request()
            request.id = deleteFileBody.fileId
            request.url = "/recyclebin/trash"
            request.body = deleteFileBody
            batch.requests.add(request)
        }
        batch(aliDriveEntity, batch)
    }

    suspend fun searchFile(aliDriveEntity: AliDriveEntity, name: String, driveId: List<String>): List<AliDriveSearch> {
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v3/file/search") {
            setJsonBody("""
                {"limit":20,"query":"name match \"$name\"","image_thumbnail_process":"image/resize,w_256/format,avif","image_url_process":"image/resize,w_1920/format,avif","video_thumbnail_process":"video/snapshot,t_1000,f_jpg,ar_auto,w_256","order_by":"updated_at DESC","drive_id_list":[${driveId.joinToString(",", prefix = "\"", postfix = "\"")}]}
            """.trimIndent())
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        jsonNode.check2()
        return jsonNode["items"].convertValue()
    }

    suspend fun userGet(aliDriveEntity: AliDriveEntity): AliDriveUser {
        val jsonNode = client.post("https://user.aliyundrive.com/v2/user/get") {
            aliDriveEntity.appendAuth()
            setJsonBody("{}")
        }.body<JsonNode>()
        jsonNode.check2()
        return jsonNode.convertValue()
    }

    suspend fun videoInfo(aliDriveEntity: AliDriveEntity, driveId: Int, fileId: String): AliDriveVideo {
        val jsonNode = client.post("https://api.aliyundrive.com/v2/file/get_video_preview_play_info") {
            setJsonBody("""
                {"drive_id":"$driveId","file_id":"$fileId","category":"live_transcoding","template_id":"","get_subtitle_info":true}
            """.trimIndent())
            aliDriveEntity.appendAuth()
            aliDriveEntity.appendEncrypt()
        }.body<JsonNode>()
        jsonNode.check2()
        return jsonNode.convertValue()
    }

    suspend fun videoUpdate(aliDriveEntity: AliDriveEntity, driveId: Int, fileId: String, duration: Double, play: Double) {
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v2/video/update") {
            setJsonBody("""
                {"drive_id":"$driveId","file_id":"$fileId","play_cursor":"$play","duration":"$duration"}
            """.trimIndent())
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        jsonNode.check2()
    }

    suspend fun shareAlbum(aliDriveEntity: AliDriveEntity): List<AliDriveShareAlbum> {
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v1/sharedAlbum/list") {
            setJsonBody("{}")
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        jsonNode.check2()
        return jsonNode["items"].convertValue()
    }

    suspend fun deleteShareAlbum(aliDriveEntity: AliDriveEntity, id: String) {
        val jsonNode = client.post("https://api.alipan.com/adrive/v1/sharedAlbum/delete") {
            setJsonBody("""{"sharedAlbumId":"$id"}""")
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        jsonNode.check2()
    }

    suspend fun createShareAlbum(aliDriveEntity: AliDriveEntity, name: String): String {
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v1/sharedAlbum/create") {
            setJsonBody("""{"name":"$name","description":""}""")
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        jsonNode.check2()
        return jsonNode["sharedAlbumId"].asText()
    }

    suspend fun uploadFileToShareAlbum(aliDriveEntity: AliDriveEntity, id: String, name: String, byteArray: ByteArray) {
        val activityJsonNode = client.post("https://api.aliyundrive.com/adrive/v1/sharedAlbum/createActivity") {
            setJsonBody("""{"sharedAlbumId":"$id"}""")
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        activityJsonNode.check2()
        val activityId = activityJsonNode["activityId"].asText()
        val time = System.currentTimeMillis()
        val width: Int
        val height: Int
        ByteArrayInputStream(byteArray).use {
            val image = ImageIO.read(it)
            width = image.width
            height = image.height
        }
        val phoneId = MyUtils.randomInt(1000, 9999)
        val createFileJsonNode = client.post("https://api.aliyundrive.com/adrive/v1/sharedAlbum/createFile") {
            setJsonBody("""
                {"boundId":"$id","check_name_mode":"auto_rename","device_name":"Redmi M2007J3SC","drive_id":"","hidden":false,"image_media_metadata":{"width":$width,"time":$time,"height":$height},"name":"$name","parent_file_id":"root","part_info_list":[{"part_number":1,"part_size":${byteArray.size}}],"size":${byteArray.size},"type":"file","user_meta":"{\"size\":${byteArray.size},\"android_local_file_path\":\"/storage/emulated/0/DCIM/Screenshots/$name\",\"device_meta\":[{\"identifier\":\"${phoneId}_/storage/emulated/0/DCIM/Screenshots/${name}\",\"utd_id\":\"${MyUtils.random(24)}\",\"platform\":\"android\"}],\"android_identify_id\":\"$phoneId\",\"time\":$time}"}
            """.trimIndent())
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        val fileId = createFileJsonNode["file_id"].asText()
        val uploadId = createFileJsonNode["upload_id"].asText()
        val driveId = createFileJsonNode["drive_id"].asText()
        val uploadUrl = createFileJsonNode["part_info_list"][0]["upload_url"].asText()
        OkHttpKtUtils.putStr(
            uploadUrl, byteArray.toRequestBody(), mapOf(
                "Referer" to "https://www.aliyundrive.com"
            )
        )
        client.post("https://api.aliyundrive.com/adrive/v1/sharedAlbum/completeFile") {
            setJsonBody("""
                {"boundId":"$id","drive_id":"$driveId","file_id":"$fileId","upload_id":"$uploadId"}
            """.trimIndent())
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        client.post("https://api.aliyundrive.com/adrive/v1/sharedAlbum/addFilesToActivity") {
            setJsonBody("""{"activityId":"$activityId","files":[{"drive_id":"$driveId","file_id":"$fileId"}],"sharedAlbumId":"$id"}""")
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
    }

    suspend fun shareAlbumInvite(aliDriveEntity: AliDriveEntity, id: String): AliDriveShareAlbumInvite {
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v1/sharedAlbumMember/invite") {
            setJsonBody("""
                {"sharedAlbumId":"$id"}
            """.trimIndent())
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        jsonNode.check2()
        return jsonNode.convertValue()
    }

    private suspend fun albumIdByCode(aliDriveEntity: AliDriveEntity, code: String): String {
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v1/sharedAlbumMember/invitePage") {
            setJsonBody("""{"code":"$code"}""")
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        jsonNode.check2()
        return jsonNode["sharedAlbumId"].asText()
    }

    suspend fun joinShareAlbum(aliDriveEntity: AliDriveEntity, code: String) {
        val shareAlbumId = albumIdByCode(aliDriveEntity, code)
        val joinNode = client.post("https://api.aliyundrive.com/adrive/v1/sharedAlbumMember/join") {
            setJsonBody("""
                {"code":"$code","sharedAlbumId":"$shareAlbumId"}
            """.trimIndent())
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        joinNode.check2()
        // https://api.alipan.com/adrive/v1/sharedAlbumMember/quit {"sharedAlbumId":""}
    }

    suspend fun quitShareAlbum(aliDriveEntity: AliDriveEntity, id: String) {
        val jsonNode = client.post("https://api.alipan.com/adrive/v1/sharedAlbumMember/quit") {
            setJsonBody("""
                {"sharedAlbumId":"$id"}
            """.trimIndent())
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        jsonNode.check2()
    }

    suspend fun quitShareAlbumByCode(aliDriveEntity: AliDriveEntity, code: String) {
        val shareAlbumId = albumIdByCode(aliDriveEntity, code)
        quitShareAlbum(aliDriveEntity, shareAlbumId)
    }

    suspend fun bottleFish(aliDriveEntity: AliDriveEntity): AliDriveBottle {
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v1/bottle/fish") {
            setJsonBody("""
                {}
            """.trimIndent())
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        jsonNode.check2()
        return jsonNode.convertValue()
    }

    suspend fun saveTo(aliDriveEntity: AliDriveEntity, shareId: String) {
        val tokenNode = client.post("https://api.aliyundrive.com/v2/share_link/get_share_token") {
            setJsonBody("""
                {"share_id":"$shareId","share_pwd":""}
            """.trimIndent())
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        tokenNode.check2()
        val shareToken = tokenNode["share_token"].asText()
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v2/file/list_by_share") {
            setJsonBody("""
                {"share_id":"$shareId","parent_file_id":"root","limit":20,"image_thumbnail_process":"image/resize,w_256/format,jpeg","image_url_process":"image/resize,w_1920/format,jpeg/interlace,1","video_thumbnail_process":"video/snapshot,t_1000,f_jpg,ar_auto,w_256","order_by":"name","order_direction":"DESC"}
            """.trimIndent())
            aliDriveEntity.appendAuth()
            headers { append("X-Share-Token", shareToken) }
        }.body<JsonNode>()
        val page = jsonNode.convertValue<AliDrivePage<AliDriveShareFile>>()
        val file = page.items.random()
        val fileId = file.fileId
//        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v3/share_link/get_share_by_anonymous?share_id=$shareId") {
//            setJsonBody("""{"share_id":"$shareId"}""")
//            aliDriveEntity.appendAuth()
//        }.body<JsonNode>()
//        jsonNode.check2()
//        val fileId = jsonNode["file_infos"][0]["file_id"].asText()
        val batch = AliDriveBatch()
        val request = AliDriveBatch.Request()
        request.id = "0"
        request.url = "/file/copy"
        val body = AliDriveBatch.SaveShareFileBody()
        body.fileId = fileId
        body.shareId = shareId
        val userGet = userGet(aliDriveEntity)
        body.toDriveId = userGet.resourceDriveId.toString()
        request.body = body
        batch.requests.add(request)
        batch(aliDriveEntity, batch, mapOf("X-Share-Token" to shareToken))
    }

    private fun encryptKey(): AliDriveKey {
        val byteArray = ByteArray(32)
        Random().nextBytes(byteArray)
        val privateKey = HexUtils.byteArrayToHex(byteArray)
        val publicKey = generatePublicKey(privateKey)
        return AliDriveKey(privateKey, publicKey)
    }

    private fun generatePublicKey(privateKey: String): String {
        val fromBytes = SECP256K1.SecretKey.fromBytes(Bytes32.wrap(Hex.decode(privateKey)))
        val keyPair = SECP256K1.KeyPair.fromSecretKey(fromBytes)
        return Hex.toHexString(keyPair.publicKey().bytesArray())
    }

    private suspend fun encrypt(aliDriveEntity: AliDriveEntity, encryptKey: AliDriveKey, deviceId: String, userid: String,
                                nonce: Int = 0, deviceName: String = "Chrome浏览器", modelName: String = "Windows网页版"): AliDriveEncrypt {
        val phone = deviceName != "Chrome浏览器"
        val privateKey = encryptKey.privateKey
        val publicKey = encryptKey.publicKey
        val appId = if (!phone) "5dde4e1bdf9e4966b387ba58f4b3fdc3"
        else "81c38d8c0d224d5d981f5f4e6db2d587"
        val data = "$appId:$deviceId:$userid:$nonce".toByteArray()
        val fromBytes = SECP256K1.SecretKey.fromBytes(Bytes32.wrap(Hex.decode(privateKey)))
        val keyPair = SECP256K1.KeyPair.fromSecretKey(fromBytes)
        val signature = Hex.toHexString(SECP256K1.signHashed(Hash.sha2_256(data), keyPair).bytes().toArray())
        val accessToken = accessToken(aliDriveEntity)
        val jsonNode = /*if (nonce == 0) {*/
            client.post("https://api.aliyundrive.com/users/v1/users/device/create_session") {
                setJsonBody("""
                    {"deviceName":"$deviceName","modelName":"$modelName","nonce":"0","pubKey":"$publicKey","refreshToken":"${aliDriveEntity.refreshToken}"}
                """.trimIndent())
                headers {
                    append("x-device-id", deviceId)
                    append("x-signature", signature)
                    append("authorization", accessToken)
                    if (phone) {
                        append("X-Canary", "client=Android,app=adrive,version=v4.1.0")
                    }
                }
            }.body<JsonNode>()
        /*} else {
            client.post("https://api.aliyundrive.com/users/v1/users/device/renew_session") {
                setJsonBody("{}")
                headers {
                    append("x-device-id", deviceId)
                    append("x-signature", signature)
                    append("authorization", accessToken)
                    if (phone) {
                        append("X-Canary", "client=Android,app=adrive,version=v4.1.0")
                    }
                }
            }.body<JsonNode>()
        }*/
        jsonNode.check()
        return AliDriveEncrypt(deviceId, signature)
    }

    @Suppress("DuplicatedCode")
    suspend fun finishTask(aliDriveEntity: AliDriveEntity) {
        val signInInfo = signInInfo(aliDriveEntity)
        val reward = signInInfo.rewards[1]
        when (reward.remind) {
            "创建一个手工相册即可领取奖励" -> {
                val aliDriveAlbums = albumList(aliDriveEntity).filter { it.name.contains("kuku的") }
                aliDriveAlbums.forEach {
                    val fileList = albumFileList(aliDriveEntity, it.id)
                    val bodies = fileList.items.map { item -> AliDriveBatch.DeleteFileBody(item.driveId.toString(), item.fileId) }
                    batchDeleteFile(aliDriveEntity, bodies)
                    deleteAlbum(aliDriveEntity, it.id)
                }
                createAlbum(aliDriveEntity, "kuku的创建相册任务")
            }
            "上传10个文件到备份盘即可领取奖励" -> {
                val userGet = userGet(aliDriveEntity)
                val backupDriveId = userGet.backupDriveId
                val searchFile = searchFile(aliDriveEntity, "kuku的上传文件任务", listOf(backupDriveId.toString()))
                val fileId = if (searchFile.isEmpty())
                    createFolder(aliDriveEntity, backupDriveId, "kuku的上传文件任务").fileId
                else searchFile[0].fileId
                val fileList = fileList(aliDriveEntity, backupDriveId, fileId)
                val bodies = fileList.items.map { AliDriveBatch.DeleteFileBody(it.driveId.toString(), it.fileId) }
                batchDeleteFile(aliDriveEntity, bodies)
                repeat(12) {
                    delay(3000)
                    val bytes = picture()
                    uploadFileToBackupDrive(aliDriveEntity, backupDriveId,
                        "${MyUtils.random(10)}.jpg", bytes, fileId)
                }
            }
            "备份10张照片到相册即可领取奖励" -> {
                val albumsDriveId = albumsDriveId(aliDriveEntity)
                val albumList = albumList(aliDriveEntity)
                val findAlbum = albumList.find { it.name == "kuku的上传图片任务" }
                val id = findAlbum?.id ?: createAlbum(aliDriveEntity, "kuku的上传图片任务").id
                val fileList = albumFileList(aliDriveEntity, id)
                val bodies = fileList.items.map { AliDriveBatch.DeleteFileBody(it.driveId.toString(), it.fileId) }
                batchDeleteFile(aliDriveEntity, bodies)
                repeat(12) {
                    delay(3000)
                    val bytes = picture()
                    val complete = uploadFileToAlbums(
                        aliDriveEntity,
                        albumsDriveId,
                        "${MyUtils.random(10)}.jpg",
                        bytes
                    )
                    addFileToAlbum(aliDriveEntity, albumsDriveId, complete.fileId, id)
                }
            }
            "接3次好运瓶即可领取奖励" -> {
                repeat(3) {
                    delay(3000)
                    bottleFish(aliDriveEntity)
                }
            }
            "播放1个视频30秒即可领取奖励" -> {
                val userGet = userGet(aliDriveEntity)
                val backupDriveId = userGet.backupDriveId
                val searchFile = searchFile(aliDriveEntity, "kuku的视频", listOf(backupDriveId.toString()))
                val fileId = if (searchFile.isEmpty())
                    createFolder(aliDriveEntity, backupDriveId, "kuku的视频").fileId
                else searchFile[0].fileId
                val fileList = fileList(aliDriveEntity, backupDriveId, fileId)
                val bodies = fileList.items.map { AliDriveBatch.DeleteFileBody(it.driveId.toString(), it.fileId) }
                batchDeleteFile(aliDriveEntity, bodies)
                val bytes = client.get("https://minio.kuku.me/kuku/BV14s4y1Z7ZAoutput.mp4").body<ByteArray>()
                val uploadComplete = uploadFileToBackupDrive(
                    aliDriveEntity, backupDriveId,
                    "BV14s4y1Z7ZAoutput.mp4", bytes, fileId
                )
                val uploadFileId = uploadComplete.fileId
                val uploadDriveId = uploadComplete.driveId
                val videoInfo = videoInfo(aliDriveEntity, uploadDriveId, uploadFileId)
                videoUpdate(aliDriveEntity, uploadDriveId, uploadFileId, videoInfo.videoPreviewPlayInfo.meta.duration,
                    50.123)
            }
            "创建共享相簿邀请成员加入并上传10张照片" -> {
                val albumList = shareAlbum(aliDriveEntity)
                albumList.filter { it.name == "kuku的共享相册任务" }.forEach {
                    deleteShareAlbum(aliDriveEntity, it.shareAlbumId)
                }
                val id = createShareAlbum(aliDriveEntity, "kuku的共享相册任务")
                val shareAlbumInvite = shareAlbumInvite(aliDriveEntity, id)
                val filterEntity = aliDriveService.findAll().filter { it.id != aliDriveEntity.id }.randomOrNull()
                    ?: error("数据库中未拥有其他阿里云盘账号，无法邀请成员加入共享相簿")
                joinShareAlbum(filterEntity, shareAlbumInvite.code())
                repeat(12) {
                    delay(3000)
                    val bytes = picture()
                    uploadFileToShareAlbum(aliDriveEntity, id, "${MyUtils.random(6)}.jpg", bytes)
                }
            }
            "使用快传功能传输任意1个文件即可领取奖励" -> {
                val userGet = userGet(aliDriveEntity)
                val backupDriveId = userGet.backupDriveId
                val searchFile = searchFile(aliDriveEntity, "kuku的上传文件任务", listOf(backupDriveId.toString()))
                val fileId = if (searchFile.isEmpty())
                    createFolder(aliDriveEntity, backupDriveId, "kuku的上传文件任务").fileId
                else searchFile[0].fileId
                val bytes = picture()
                val complete = uploadFileToBackupDrive(
                    aliDriveEntity, backupDriveId,
                    "${MyUtils.random(10)}.jpg", bytes, fileId
                )
                quickShare(aliDriveEntity, backupDriveId, complete.fileId)
            }
            "分享好运口令（点击分享-> 今日好运卡）" -> {
                shareGoodLuckCard(aliDriveEntity)
            }
            "接好运瓶并转存任意1个文件" -> {
                val bottleFish = bottleFish(aliDriveEntity)
                val shareId = bottleFish.shareId
                saveTo(aliDriveEntity, shareId)
            }
            "创建1个共享相簿即可领取奖励" -> {
                val albumList = shareAlbum(aliDriveEntity)
                albumList.filter { it.name == "kuku的共享相册任务" }.forEach {
                    deleteShareAlbum(aliDriveEntity, it.shareAlbumId)
                }
                createShareAlbum(aliDriveEntity, "kuku的共享相册任务")
            }
            "开启自动备份并备份满10个文件" -> {
                val deviceList = deviceList(aliDriveEntity)
                val random = deviceList.randomOrNull() ?: error("请在app上登陆阿里云盘")
                aliDriveEntity.deviceId = random.deviceId
                backup(aliDriveEntity, random.deviceName, random.deviceSystemVersion)
                val driveId = albumsDriveId(aliDriveEntity)
                repeat(12) {
                    delay(3000)
                    val bytes = picture()
                    uploadFileToAlbums(aliDriveEntity, driveId,
                        "${MyUtils.random(10)}.jpg", bytes, scene = AliDriveScene.AutoBackup, deviceName = "${random.deviceName} ${random.deviceModel}")
                }
            }
            "开启手机自动备份并持续至少一小时" -> {
                val deviceList = deviceList(aliDriveEntity)
                val random = deviceList.randomOrNull() ?: error("请在app上登陆阿里云盘")
                aliDriveEntity.deviceId = random.deviceId
                backup(aliDriveEntity, random.deviceName, random.deviceSystemVersion)
                signInInfo(aliDriveEntity)
            }
            else -> error("不支持的任务，${reward.remind}")
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

    suspend fun signInList(aliDriveEntity: AliDriveEntity): AliDriveSignIn {
        val jsonNode = client.post("https://member.aliyundrive.com/v2/activity/sign_in_list?_rx-s=mobile") {
            setJsonBody("{}")
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        jsonNode.check()
        return jsonNode["result"].convertValue()
    }

    suspend fun quickShare(aliDriveEntity: AliDriveEntity, driveId: Int, fileId: String) {
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v1/share/create") {
            setJsonBody("""{"drive_file_list":[{"drive_id":"$driveId","file_id":"$fileId"}]}""")
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        jsonNode.check2()
    }

    suspend fun shareGoodLuckCard(aliDriveEntity: AliDriveEntity) {
        val jsonNode = client.post("https://member.aliyundrive.com/v1/activity/behave?_rx-s=mobile") {
            setJsonBody("""{"behave":"share-signIn-code"}""")
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        jsonNode.check()
    }

    suspend fun backup(aliDriveEntity: AliDriveEntity, brand: String, systemVersion: String, status: Boolean = true) {
        val jsonNode = client.post("https://api.alipan.com/users/v1/users/update_device_extras") {
            setJsonBody("""
                {"albumAccessAuthority":true,"albumBackupLeftFileTotal":0,"albumBackupLeftFileTotalSize":0,"albumFile":0,"autoBackupStatus":$status,"brand":"${brand.lowercase()}","systemVersion":"$systemVersion","totalSize":242965508096,"umid":"ZDYBtYRLPNE6gwKLFDrYBaGJ8Q/r8p58","useSize":122042286080,"utdid":"Y90sZAck9L8DAO5WYKs2lFge"}
            """.trimIndent())
            aliDriveEntity.appendAuth()
            aliDriveEntity.appendEncrypt(findDevice(aliDriveEntity))
        }.body<JsonNode>()
        jsonNode.check()
    }

    suspend fun deviceList(aliDriveEntity: AliDriveEntity): List<AliDriveDevice> {
        val newEntity = aliDriveService.findById(aliDriveEntity.id!!)!!
        val jsonNode = client.post("https://api.alipan.com/adrive/v2/backup/device_applet_list_summary") {
            setJsonBody("{}")
            newEntity.appendAuth()
            newEntity.appendEncrypt()
        }.body<JsonNode>()
        jsonNode.check2()
        return jsonNode["deviceItems"].convertValue()
    }

    suspend fun findDevice(aliDriveEntity: AliDriveEntity): AliDriveDevice {
        val list = deviceList(aliDriveEntity)
        return list.find { it.deviceId == aliDriveEntity.deviceId } ?: AliDriveDevice()
    }

}


data class AliDriveQrcode(
    var qrcodeUrl: String = "",
    var ck: String = "",
    var csrfToken: String = "",
    var idToken: String = "",
    var hs: String = "",
    var t: Long = 0
)

data class AliDriveAccessToken(val accessToken: String, val expire: Long) {
    fun isExpire() = System.currentTimeMillis() > expire
}

data class AliDriveTeam(val id: Int, val period: String, val title: String, val subTitle: String,
    val joinTeam: String, val joinCount: Int, val endTime: Long)

class AliDriveSign {
    var subject: String = ""
    var customMessage: String = ""
    var title: String = ""
    var isReward: Boolean = false
    var blessing: String = ""
    var signInCount: Int = 0
    var signInLogs: MutableList<SignInLog> = mutableListOf()

    class SignInLog {
        var day: Int = 0
        var status: String = ""
        var type: String = ""
        var rewardAmount: Int = 0
        var themes: String = ""
        var calendarChinese: String = ""
        var calendarDay: Int = 0
        var calendarMonth: String = ""
        var isReward: Boolean = false
    }
}

class AliDriveAlbum {
    var id: String = ""
    var name: String = ""
}

class AliDriveUploadComplete {
    @JsonProperty("drive_id")
    var driveId: Int = 0
    @JsonProperty("file_id")
    var fileId: String = ""
    var name: String = ""
}

class AliDriveFolder {
    @JsonProperty("parent_file_id")
    var parentFileId: String = ""
    var type: String = ""
    @JsonProperty("file_id")
    var fileId: String = ""
    @JsonProperty("domain_id")
    var domainId: String = ""
    @JsonProperty("drive_id")
    var driveId: Int = 0
    @JsonProperty("file_name")
    var fileName: String = ""
}

class AliDriveUser {
    @JsonProperty("backup_drive_id")
    var backupDriveId: Int = 0
    @JsonProperty("resource_drive_id")
    var resourceDriveId: Int = 0
    @JsonProperty("default_drive_id")
    var defaultDriveId: Int = 0
    @JsonProperty("user_id")
    var userid: String = ""
}

class AliDriveSignInInfo {
    var isSignIn: Boolean = false
    var month: String = ""
    var day: Int = 0
    var signInDay: Int = 0
    var rewards: MutableList<Reward> = mutableListOf()

    class Reward {
        var name: String = ""
        var type: String = ""
        var remind: String = ""
        var status: String = ""
        var position: Int = 0
    }
}

class AliDriveSearch {
    @JsonProperty("drive_id")
    var driveId: Int = 0
    @JsonProperty("domain_id")
    var domainId: String = ""
    @JsonProperty("file_id")
    var fileId: String = ""
    var name: String = ""
    var type: String = ""
}

class AliDriveVideo {
    @JsonProperty("domain_id")
    var domainId: String = ""
    @JsonProperty("drive_id")
    var driveId: Int = 0
    @JsonProperty("file_id")
    var fileId: String = ""
    var category: String = ""
    @JsonProperty("video_preview_play_info")
    var videoPreviewPlayInfo: VideoPreviewPlayInfo = VideoPreviewPlayInfo()

    class VideoPreviewPlayInfo {
        var category: String = ""
        var meta: Meta = Meta()

        class Meta {
            var duration: Double = 0.0
            var width: Int = 0
            var height: Int = 0
        }
    }
}

class AliDriveShareAlbum {
    @JsonProperty("sharedAlbumId")
    var shareAlbumId: String = ""
    var name: String = ""
    var description: String = ""
}

class AliDriveBottle {
    var bottleId: Long = 0
    var bottleName: String = ""
    var shareId: String = ""
}

data class AliDriveEncrypt(val deviceId: String, val signature: String)

data class AliDriveKey(val privateKey: String, val publicKey: String)

class AliDriveSignature(val key: AliDriveKey) {
    var nonce: Int = 0
    var deviceId: String = ""
    var signature: String = ""
    var userid: String = ""
    private var expire: Long = System.currentTimeMillis() + 1000 * 60 * 60

    fun isExpire() = System.currentTimeMillis() > expire

    fun expireRefresh() {
        expire = System.currentTimeMillis() + 1000 * 60 * 30
    }
}

class AliDriveSignIn {
    var month: String = ""
    var signInCount: Int = 0
    var signInInfos: MutableList<SignInInfo> = mutableListOf()

    class SignInInfo {
        var day: Int = 0
        var date: String? = null
        var blessing: String = ""
        var status: String = ""
        var subtitle: String? = null
        var theme: String? = null
        var rewards: MutableList<Reward> = mutableListOf()

        class Reward {
            var name: String = ""
            var type: String = ""
            var status: String = ""
            var remind: String = ""
        }
    }


}

class AliDriveFileList {
    var items: MutableList<AliDriveFile> = mutableListOf()
    @JsonProperty("next_marker")
    var nextMarker: String = ""
}

class AliDriveFile {
    var category: String = ""
    @JsonProperty("content_hash")
    var contentHash: String = ""
    @JsonProperty("created_at")
    var createdAt: String = ""
    @JsonProperty("drive_id")
    var driveId: Int = 0
    @JsonProperty("file_extension")
    var fileExtension: String = ""
    @JsonProperty("file_id")
    var fileId: String = ""
    @JsonProperty("mime_type")
    var mimeType: String = ""
    var name: String = ""
    @JsonProperty("parent_file_id")
    var parentFileId: String = ""
    @JsonProperty("punish_flag")
    var punishFlag: Int = 0
    var size: Long = 0
    var starred: Boolean = false
    var thumbnail: String = ""
    var type: String = ""
    @JsonProperty("updated_at")
    var updatedAt: String = ""
    var url: String = ""
    @JsonProperty("user_meta")
    var userMeta: String = ""
}

class AliDriveBatch {
    var requests: MutableList<Request> = mutableListOf()
    var resource: String = "file"

    class Request {
        var body: Any = Any()
        var headers: MutableMap<String, String> = mutableMapOf("Content-Type" to "application/json")
        var id: String = ""
        var method: String = "POST"
        var url: String = ""
    }

    data class DeleteFileBody(
        @JsonProperty("drive_id")
        var driveId: String = "",
        @JsonProperty("file_id")
        var fileId: String = ""
    )

    data class SaveShareFileBody(
        @JsonProperty("file_id")
        var fileId: String = "",
        @JsonProperty("share_id")
        var shareId: String = "",
        @JsonProperty("auto_rename")
        var autoRename: Boolean = true,
        @JsonProperty("to_parent_file_id")
        var toParentFileId: String = "root",
        @JsonProperty("to_drive_id")
        var toDriveId: String = ""
    )
}

class AliDriveShareAlbumInvite {

    var message: String = ""
    var url: String = ""
    var shareMode: String = ""
    var shareTitle: String = ""
    var shareSubTitle: String = ""
    var shareImageUrl: String = ""

    fun code(): String {
        return MyUtils.regex("(?<=album/).*", url) ?: error("找不到共享相册code")
    }

}

class AliDrivePage<T> {
    var items: MutableList<T> = mutableListOf()
    @JsonProperty("next_marker")
    var nextMarker: String = ""
}

class AliDriveShareFile {
    @JsonProperty("drive_id")
    var driveId: String = ""
    @JsonProperty("domain_id")
    var domainId: String = ""
    @JsonProperty("file_id")
    var fileId: String = ""
    @JsonProperty("share_id")
    var shareId: String = ""
    var name: String = ""
    var type: String = ""
    @JsonProperty("parent_file_id")
    var parentFileId: String = ""
}

enum class AliDriveScene(val value: String) {
    Upload("file_upload"),
    AutoBackup("album_autobackup"),
    ManualBackup("album_manualbackup")
}

class AliDriveDevice {
    var fileCount: Int = 0
    var totalSize: String = ""
    var latestBackupTime: Long = 0
    var earlyBackupTime: Long = 0
    var enable: Boolean = false
    var defaultDriveFileCount: Int = 0
    var albumDriveFileCount: Int = 0
    var defaultDriveFileSize: String = ""
    var albumDriveFileSize: String = ""
    var deviceId: String = ""
    var deviceName: String = "Chrome浏览器"
    var deviceModel: String = "Windows网页版"
    var deviceType: String = ""
    var deviceSystemVersion: String = ""
    var deviceRegisterDay: Int = 0
    var updateTime: Long = 0
}