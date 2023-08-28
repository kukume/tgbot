package me.kuku.telegram.logic

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import me.kuku.pojo.CommonResult
import me.kuku.telegram.entity.AliDriveEntity
import me.kuku.telegram.entity.AliDriveService
import me.kuku.utils.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.crypto.signers.HMacDSAKCalculator
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.time.LocalDate
import java.util.*
import javax.imageio.ImageIO

@Service
class AliDriveLogic(
    private val aliDriveService: AliDriveService
) {

    private val cache = mutableMapOf<Long, AliDriveAccessToken>()
    private val signatureCache = mutableMapOf<Long, AliDriveSignature>()

    suspend fun login1() = client.get("https://api.kukuqaq.com/alidrive/qrcode").body<AliDriveQrcode>()

    suspend fun login2(qrcode: AliDriveQrcode): CommonResult<AliDriveEntity> {
        val jsonNode = client.post("https://api.kukuqaq.com/alidrive/qrcode") {
            setJsonBody(qrcode)
        }.body<JsonNode>()
        return if (jsonNode.has("code")) {
            CommonResult.fail(message = jsonNode["message"].asText(), code = jsonNode["code"].asInt())
        } else CommonResult.success(AliDriveEntity().also {
            it.refreshToken = jsonNode["refreshToken"].asText()
        })
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
            aliDriveEntity.refreshToken = newRefreshToken
            aliDriveService.save(aliDriveEntity)
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
        return if (jsonNode["success"]?.asBoolean() == true) {
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
            sign
        } else error(jsonNode["code"].asText())
    }

    context(HttpRequestBuilder)
    private suspend fun AliDriveEntity.appendAuth() {
        val accessToken = accessToken(this@AliDriveEntity)
        headers {
            append("Authorization", accessToken)
        }
    }

    context(HttpRequestBuilder)
    private suspend fun AliDriveEntity.appendEncrypt() {
        val tgId = this@AliDriveEntity.tgId
        val aliDriveSignature = if (signatureCache.containsKey(tgId)) {
            val aliDriveSignature = signatureCache[tgId]!!
            if (aliDriveSignature.isExpire()) {
                aliDriveSignature.nonce += 1
                encrypt(this@AliDriveEntity, AliDriveKey(aliDriveSignature.privateKey, aliDriveSignature.publicKey),
                    aliDriveSignature.deviceId, aliDriveSignature.userid)
                aliDriveSignature.expireRefresh()
                aliDriveSignature
            } else aliDriveSignature
        } else {
            val userGet = userGet(this@AliDriveEntity)
            val deviceId = UUID.randomUUID().toString()
            val encryptKey = encryptKey()
            val encrypt = encrypt(this@AliDriveEntity, encryptKey, deviceId, userGet.userid)
            val aliDriveSignature = AliDriveSignature()
            aliDriveSignature.deviceId = deviceId
            aliDriveSignature.privateKey = encryptKey.privateKey
            aliDriveSignature.publicKey = encryptKey.publicKey
            aliDriveSignature.signature = encrypt.signature
            signatureCache[tgId] = aliDriveSignature
            aliDriveSignature
        }
        headers {
            append("x-device-id", aliDriveSignature.deviceId)
            append("x-signature", aliDriveSignature.signature)
        }
    }

    private fun JsonNode.check() {
        if (this["success"]?.asBoolean() != true) error(this["code"].asText())
    }

    private fun JsonNode.check2() {
        if (this.has("code")) error(this["message"].asText())
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
        val jsonNode = client.post("https://member.aliyundrive.com/v1/activity/sign_in_task_reward?_rx-s=mobile") {
            setJsonBody("""{"signInDay": $day}""")
            headers {
                append("Authorization", accessToken)
            }
        }.body<JsonNode>()
        return if (jsonNode["success"]?.asBoolean() == true) {
            "领取成功，${jsonNode["result"]["notice"].asText()}"
        } else error(jsonNode["code"].asText())
    }

    suspend fun signInInfo(aliDriveEntity: AliDriveEntity): AliDriveSignInInfo {
        val jsonNode = client.post("https://member.aliyundrive.com/v2/activity/sign_in_info") {
            setJsonBody("{}")
            aliDriveEntity.appendAuth()
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
        return jsonNode["data"]["driveId"].asInt()
    }

    @Suppress("DuplicatedCode")
    suspend fun uploadFileToAlbums(aliDriveEntity: AliDriveEntity, driveId: Int, fileName: String, byteArray: ByteArray): AliDriveUploadComplete {
        // 有code 异常
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v1/biz/albums/file/create") {
            setJsonBody("""
                {"drive_id":"$driveId","part_info_list":[{"part_number":1}],"parent_file_id":"root","name":"$fileName","type":"file","check_name_mode":"auto_rename","size":${byteArray.size},"create_scene":"album_manualbackup","device_name":""}
            """.trimIndent())
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
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

    suspend fun albumList(aliDriveEntity: AliDriveEntity): List<AliDriveAlbum> {
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v1/album/list") {
            setJsonBody("""{"limit":20,"order_by":"created_at","order_direction":"ASC"}""")
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        val list = mutableListOf<AliDriveAlbum>()
        for (node in jsonNode["items"]) {
            list.add(AliDriveAlbum().also {
                it.id = node["album_id"].asText()
                it.name = node["name"].asText()
            })
        }
        return list
    }

    suspend fun createAlbum(aliDriveEntity: AliDriveEntity, name: String): AliDriveAlbum {
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v1/album/create") {
            setJsonBody("""{"name":"$name","description":""}""")
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        return AliDriveAlbum().also {
            it.id = jsonNode["album_id"].asText()
            it.name = jsonNode["name"].asText()
        }
    }

    suspend fun deleteAlbum(aliDriveEntity: AliDriveEntity, id: String) {
        client.post("https://api.aliyundrive.com/adrive/v1/album/delete") {
            setJsonBody("""{"album_id":"$id"}""")
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
    }

    suspend fun addFileToAlbum(aliDriveEntity: AliDriveEntity, driveId: Int, fileId: String, albumId: String) {
        client.post("https://api.aliyundrive.com/adrive/v1/album/add_files") {
            setJsonBody("""{"drive_file_list":[{"drive_id":"$driveId","file_id":"$fileId"}],"album_id":"$albumId"}""")
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
    }

    @Suppress("DuplicatedCode")
    suspend fun uploadFileToBackupDrive(aliDriveEntity: AliDriveEntity, driveId: Int, fileName: String, byteArray: ByteArray, parentId: String = "root"): AliDriveUploadComplete {
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v2/file/createWithFolders") {
            setJsonBody("""
                {"drive_id":"$driveId","part_info_list":[{"part_number":1}],"parent_file_id":"$parentId","name":"$fileName","type":"file","check_name_mode":"auto_rename","size":${byteArray.size},"create_scene":"file_upload","device_name":""}
            """.trimIndent())
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
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
            setJsonBody("""{"drive_id":"103118","parent_file_id":"root","name":"kuku的任务","check_name_mode":"refuse","type":"folder"}""")
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        return jsonNode.convertValue()
    }

    suspend fun fileList(aliDriveEntity: AliDriveEntity, driveId: Int, parentId: String = "root") {
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v3/file/list?jsonmask=next_marker%2Citems(name%2Cfile_id%2Cdrive_id%2Ctype%2Csize%2Ccreated_at%2Cupdated_at%2Ccategory%2Cfile_extension%2Cparent_file_id%2Cmime_type%2Cstarred%2Cthumbnail%2Curl%2Cstreams_info%2Ccontent_hash%2Cuser_tags%2Cuser_meta%2Ctrashed%2Cvideo_media_metadata%2Cvideo_preview_metadata%2Csync_meta%2Csync_device_flag%2Csync_flag%2Cpunish_flag)") {
            setJsonBody("""
                {"drive_id":"$driveId","parent_file_id":"$parentId","limit":20,"all":false,"url_expire_sec":14400,"image_thumbnail_process":"image/resize,w_256/format,avif","image_url_process":"image/resize,w_1920/format,avif","video_thumbnail_process":"video/snapshot,t_1000,f_jpg,ar_auto,w_256","fields":"*","order_by":"updated_at","order_direction":"DESC"}
            """.trimIndent())
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        /*
{
            "created_at": "2023-08-26T16:17:55.571Z",
            "drive_id": "103118",
            "file_id": "64ea25b376cf8062a7194961a96b9bad6c6c1656",
            "name": "kuku的任务",
            "parent_file_id": "root",
            "starred": false,
            "type": "folder",
            "updated_at": "2023-08-26T16:17:55.571Z",
            "user_meta": "{\"channel\":\"file_upload\",\"client\":\"web\"}",
            "user_tags": {
                "channel": "file_upload",
                "client": "web",
                "device_id": "2e87985f-e41d-4c56-9ecb-570f2a9c4c98",
                "device_name": "chrome",
                "version": "v4.9.0"
            }
        },
         */
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

    // 不知道是不是看视频
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
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v1/sharedAlbum/delete") {
            setJsonBody("""{"shareAlbumId": "$id"}""")
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

    suspend fun shareAlbumInvite(aliDriveEntity: AliDriveEntity, id: String): String {
        val jsonNode = client.post("https://api.aliyundrive.com/adrive/v1/sharedAlbumMember/invite") {
            setJsonBody("""
                {"sharedAlbumId":"$id"}
            """.trimIndent())
            aliDriveEntity.appendAuth()
        }.body<JsonNode>()
        return jsonNode["message"].asText()
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

    private fun encryptKey(): AliDriveKey {
        val privateExponent = BigInteger(256, Random())
        val curveParams = CustomNamedCurves.getByName("secp256k1")
        val privateKeyParams = ECPrivateKeyParameters(privateExponent, ECDomainParameters(curveParams))
        val publicKeyParams = ECPublicKeyParameters(
            curveParams.curve.decodePoint(privateKeyParams.parameters.g.getEncoded(false)), privateKeyParams.parameters
        )

        val publicBytes = publicKeyParams.q.getEncoded(false)
        val publicKey = "04" + publicBytes.joinToString("") { "%02x".format(it) }
        return AliDriveKey(privateKeyParams, publicKey)
    }

    private suspend fun encrypt(aliDriveEntity: AliDriveEntity, encryptKey: AliDriveKey, deviceId: String, userid: String, nonce: Int = 0): AliDriveEncrypt {
        val privateKey = encryptKey.privateKey
        val publicKey = encryptKey.publicKey
        val appId = "5dde4e1bdf9e4966b387ba58f4b3fdc3"
        val data = "$appId:$deviceId:$userid:$nonce"
        val ecdsaSigner = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))
        ecdsaSigner.init(true, privateKey)

        val message = data.toByteArray()
        val sig = ecdsaSigner.generateSignature(message)

        val signatureStr = sig.joinToString("") { "%02x".format(it) } + "01"
        val accessToken = accessToken(aliDriveEntity)
        val jsonNode = if (nonce == 0) {
            OkHttpUtils.postJson("https://api.aliyundrive.com/users/v1/users/device/create_session",
                OkUtils.json("""{"deviceName":"Chrome浏览器","modelName":"Windows网页版","pubKey":"$publicKey"}"""),
                mapOf(
                    "x-device-id" to deviceId,
                    "x-signature" to signatureStr,
                    "authorization" to accessToken
                )
            )
        } else {
            client.post("https://api.aliyundrive.com/users/v1/users/device/renew_session") {
                setJsonBody("{}")
                headers {
                    append("x-device-id", deviceId)
                    append("x-signature", signatureStr)
                    append("authorization", accessToken)
                }
            }.body<JsonNode>()
        }
        jsonNode.check()
        return AliDriveEncrypt(deviceId, signatureStr)
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
    var shareAlbumId: String = ""
    var name: String = ""
    var description: String = ""
}

class AliDriveBottle {
    var bottleId: Int = 0
    var bottleName: String = ""
    var shareId: String = ""
}

data class AliDriveEncrypt(val deviceId: String, val signature: String)

data class AliDriveKey(val privateKey: ECPrivateKeyParameters, val publicKey: String)

class AliDriveSignature {
    var privateKey: ECPrivateKeyParameters = ECPrivateKeyParameters(null, null)
    var publicKey: String = ""
    var nonce: Int = 0
    var deviceId: String = ""
    var signature: String = ""
    var userid: String = ""
    private var expire: Long = System.currentTimeMillis() + 1000 * 60 * 60

    fun isExpire() = System.currentTimeMillis() > expire

    fun expireRefresh() {
        expire = System.currentTimeMillis() + 1000 * 60 * 60
    }
}
