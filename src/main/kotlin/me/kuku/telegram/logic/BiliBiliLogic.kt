package me.kuku.telegram.logic

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.contains
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import me.kuku.telegram.entity.BiliBiliEntity
import me.kuku.telegram.exception.qrcodeExpire
import me.kuku.telegram.exception.qrcodeNotScanned
import me.kuku.telegram.exception.qrcodeScanned
import me.kuku.telegram.utils.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.ByteString
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists

object BiliBiliLogic {

    suspend fun getIdByName(username: String): List<BiliBiliPojo> {
        val enUsername = username.toUrlEncode()
        val jsonNode = client.get("https://api.bilibili.com/x/web-interface/search/type?context=&search_type=bili_user&page=1&order=&keyword=$enUsername&category_id=&user_type=&order_sort=&changing=mid&__refresh__=true&_extra=&highlight=1&single_column=0&jsonp=jsonp&callback=__jp2") {
            referer("https://search.bilibili.com/topic?keyword=$enUsername")
        }.bodyAsText().jsonpToJsonNode()
        val dataJsonNode = jsonNode["data"]
        return if (dataJsonNode["numCommonResults"].asInt() != 0) {
            val jsonArray = dataJsonNode["result"]
            val list = mutableListOf<BiliBiliPojo>()
            for (obj in jsonArray) {
                list.add(
                    BiliBiliPojo(userId = obj["mid"].asText(),
                        name = obj["uname"].asText())
                )
            }
            list
        } else error("not result")
    }

    private fun convert(jsonNode: JsonNode): BiliBiliPojo {
        val biliBiliPojo = BiliBiliPojo()
        val descJsonNode = jsonNode["desc"]
        val infoJsonNode = descJsonNode["user_profile"]?.get("info")
        val forwardJsonNode = descJsonNode["origin"]
        biliBiliPojo.userId = infoJsonNode?.get("uid")?.asText() ?: ""
        biliBiliPojo.name = infoJsonNode?.get("uname")?.asText() ?: ""
        biliBiliPojo.id = descJsonNode["dynamic_id"].asText()
        biliBiliPojo.rid = descJsonNode["rid"].asText()
        biliBiliPojo.time = (descJsonNode["timestamp"].asText() + "000").toLong()
        biliBiliPojo.bvId = descJsonNode.get("bvid")?.asText() ?: ""
        biliBiliPojo.isForward = !forwardJsonNode.isNull
        if (!forwardJsonNode.isNull) {
            biliBiliPojo.forwardBvId = forwardJsonNode["bvid"]?.asText() ?: ""
            if (biliBiliPojo.forwardBvId.isEmpty()) {
                val rid = forwardJsonNode["rid"].asInt()
                if (rid != 0)
                    biliBiliPojo.forwardBvId = "av$rid"
            }
            forwardJsonNode.get("timestamp")?.asText()?.let {
                biliBiliPojo.forwardTime = (it + "000").toLong()
            }
            biliBiliPojo.forwardId = forwardJsonNode["dynamic_id"].asText()
        }
        var text: String? = null
        jsonNode["card"]?.asText()?.let { Jackson.readTree(it) }?.let { cardJsonNode ->
            if (biliBiliPojo.userId.isEmpty()) {
                val collectionJsonNode = cardJsonNode["collection"]
                biliBiliPojo.userId = collectionJsonNode["id"].asText()
                biliBiliPojo.name = collectionJsonNode["name"]?.asText() ?: ""
            }
            val itemJsonNode = cardJsonNode["item"]
            text = cardJsonNode["dynamic"]?.asText()
            val picList = biliBiliPojo.picList
            if (biliBiliPojo.bvId.isNotEmpty()) {
                cardJsonNode["pic"]?.asText()?.let {
                    picList.add(it)
                }
            }
            if (itemJsonNode != null) {
                if (text == null) text = itemJsonNode["description"]?.asText()
                if (text == null) text = itemJsonNode["content"]?.asText()
                itemJsonNode["pictures"]?.forEach {
                    picList.add(it["img_src"].asText())
                }
            }
            if (text == null) {
                cardJsonNode["vest"]?.let {
                    text = it["content"].asText()
                }
            }
            if (text == null && cardJsonNode.contains("title")) {
                text = cardJsonNode["title"].asText() + "------" + cardJsonNode["summary"].asText()
            }
            cardJsonNode["pub_location"]?.asText()?.let { location ->
                biliBiliPojo.ipFrom = location
            }
            val originStr = cardJsonNode["origin"]?.asText()
            if (originStr != null && (originStr.startsWith("{") || originStr.startsWith("["))) {
                val forwardPicList = biliBiliPojo.forwardPicList
                val forwardContentJsonNode = originStr.toJsonNode()
                if (biliBiliPojo.forwardBvId.isNotEmpty()) {
                    forwardContentJsonNode["pic"]?.let {
                        forwardPicList.add(it.asText())
                    }
                }
                val ctime = forwardContentJsonNode["ctime"].asInt()
                biliBiliPojo.forwardTime = ctime * 1000L
                if (forwardContentJsonNode.contains("item")) {
                    val forwardItemJsonNode = forwardContentJsonNode["item"]
                    biliBiliPojo.forwardText = forwardItemJsonNode["description"]?.asText() ?: ""
                    if (biliBiliPojo.forwardText.isEmpty())
                        biliBiliPojo.forwardText = forwardItemJsonNode["content"].asText()
                    val forwardPicJsonArray = forwardItemJsonNode["pictures"]
                    if (forwardPicJsonArray != null) {
                        for (obj in forwardPicJsonArray) {
                            forwardPicList.add(obj["img_src"].asText())
                        }
                    }
                    val forwardUserJsonNode = forwardContentJsonNode["user"]
                    if (forwardUserJsonNode != null) {
                        biliBiliPojo.forwardUserId = forwardUserJsonNode["uid"].asText()
                        biliBiliPojo.forwardName = forwardUserJsonNode["name"]?.asText() ?: forwardUserJsonNode["uname"].asText()
                    } else {
                        val forwardOwnerJsonNode = forwardContentJsonNode["owner"]
                        if (forwardOwnerJsonNode != null) {
                            biliBiliPojo.forwardUserId = forwardOwnerJsonNode["mid"].asText()
                            biliBiliPojo.forwardName = forwardOwnerJsonNode["name"].asText()

                        }
                    }
                } else {
                    biliBiliPojo.forwardText = forwardContentJsonNode["dynamic"]?.asText() ?: "没有动态内容"
                    val forwardOwnerJsonNode = forwardContentJsonNode["owner"]
                    if (forwardOwnerJsonNode != null) {
                        biliBiliPojo.forwardUserId = forwardOwnerJsonNode["mid"]?.asText() ?: ""
                        biliBiliPojo.forwardName = forwardOwnerJsonNode["name"]?.asText() ?: ""
                    } else {
                        biliBiliPojo.forwardName = forwardContentJsonNode["uname"]?.asText() ?: ""
                        biliBiliPojo.forwardUserId = forwardContentJsonNode["uid"]?.asText() ?: ""
                        biliBiliPojo.forwardText = forwardContentJsonNode["title"]?.asText() ?: ""
                    }
                }
            }
            cardJsonNode["title"]?.asText()?.takeIf { it.isNotEmpty() }?.let {
                text += "|$it"
            }
            cardJsonNode["desc"]?.asText()?.takeIf { it.isNotEmpty() }?.let {
                text += "|$it"
            }
        }
        biliBiliPojo.text = text ?: "无"
        val type = if (biliBiliPojo.bvId.isEmpty()) {
            if (biliBiliPojo.picList.isEmpty()) 17
            else 11
        }else 1
        biliBiliPojo.type = type
        return biliBiliPojo
    }

    fun convertStr(biliBiliPojo: BiliBiliPojo): String {
        val pattern = "yyyy-MM-dd HH:mm:ss"
        val bvId = biliBiliPojo.bvId
        val ipFrom = biliBiliPojo.ipFrom
        val forwardBvId = biliBiliPojo.forwardBvId
        var ss = "#${biliBiliPojo.name}\n来自：${ipFrom.ifEmpty { "无" }}\n发布时间：${Instant.ofEpochMilli(biliBiliPojo.time).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(pattern))}" +
                "\n内容：${biliBiliPojo.text}\n动态链接：https://t.bilibili.com/${biliBiliPojo.id}\n视频链接：${if (bvId.isNotEmpty()) "https://www.bilibili.com/video/$bvId" else "无"}"
        if (biliBiliPojo.isForward) {
            ss += "\n转发自：#${biliBiliPojo.forwardName}\n发布时间：${Instant.ofEpochMilli(biliBiliPojo.forwardTime).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(pattern))}\n" +
                    "内容：${biliBiliPojo.forwardText}\n动态链接：https://t.bilibili.com/${biliBiliPojo.forwardId}\n视频链接：${if (forwardBvId.isNotEmpty()) "https://www.bilibili.com/video/$forwardBvId" else "无"}"
        }
        return ss
    }

    suspend fun videoByBvId(biliBiliEntity: BiliBiliEntity, bvId: String): File {
        val htmlUrl = "https://www.bilibili.com/video/$bvId/"
        val response = client.get(htmlUrl) {
            cookieString(biliBiliEntity.cookie)
        }
        return if (response.status != HttpStatusCode.OK) {
            error("错误：${response.status}")
        } else {
            val html = response.bodyAsText()
            val jsonNode = RegexUtils.extract(html, "window.__playinfo__=", "</sc")?.toJsonNode() ?: error("未获取到内容")
            val videoUrl = jsonNode["data"]["dash"]["video"][0]["baseUrl"].asText()
            val audioUrl = jsonNode["data"]["dash"]["audio"][0]["baseUrl"].asText()
            val videoFile = client.get(videoUrl) { referer(htmlUrl) }.body<InputStream>().use {
                val path = Path.of("tmp", "$bvId.mp4")
                Files.copy(it, path)
                path
            }
            val audioFile = client.get(audioUrl) { referer(htmlUrl) }.body<InputStream>().use {
                val path = Path.of("tmp", "$bvId.mp3")
                Files.copy(it, path)
                path
            }
            val videoPath = videoFile.absolutePathString()
            val audioPath = audioFile.absolutePathString()
            val outputPath = videoPath.replace(bvId, "${bvId}output")
            ffmpeg("ffmpeg -i $videoPath -i $audioPath -c:v copy -c:a aac -strict experimental $outputPath")
            videoFile.deleteIfExists()
            audioFile.deleteIfExists()
            File(outputPath)
        }
    }

    suspend fun getDynamicById(id: String, offsetId: String = "0"): List<BiliBiliPojo> {
        val jsonNode = client.get("https://api.vc.bilibili.com/dynamic_svr/v1/dynamic_svr/space_history?visitor_uid=0&host_uid=$id&offset_dynamic_id=$offsetId&need_top=1") {
            referer("https://space.bilibili.com/$id/dynamic")
        }.body<JsonNode>()
        // next_offset  下一页开头
        val dataJsonNode = jsonNode["data"]
        val jsonArray = dataJsonNode["cards"] ?: error("动态未找到")
        val list = mutableListOf<BiliBiliPojo>()
        for (obj in jsonArray) {
            val extraJsonNode = obj["extra"]
            if (extraJsonNode != null && 1 == extraJsonNode["is_space_top"].asInt()) continue
            list.add(convert(obj))
        }
        return list
    }

    suspend fun loginByQr1(): BiliBiliQrcode {
        val jsonNode = client.get("https://passport.bilibili.com/x/passport-login/web/qrcode/generate?source=main-fe-header").body<JsonNode>()
        val data = jsonNode["data"]
        return BiliBiliQrcode(data["url"].asText(), data["qrcode_key"].asText())
    }

    suspend fun loginByQr2(qrcode: BiliBiliQrcode): BiliBiliEntity {
        val response = client.get("https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key=${qrcode.key}&source=main-fe-header")
        val jsonNode = response.body<JsonNode>()
        val data = jsonNode["data"]
        return when(data["code"].asInt()) {
            86101 -> qrcodeNotScanned()
            86090 -> qrcodeScanned()
            86038 -> qrcodeExpire()
            0 -> {
                val firstCookie = response.setCookie().renderCookieHeader()
                val url = data["url"].asText()
                val token = RegexUtils.extract(url, "bili_jct=", "\\u0026")!!
                val urlJsonNode =
                    client.get("https://passport.bilibili.com/x/passport-login/web/sso/list?biliCSRF=$token") {
                        cookieString(firstCookie)
                    }.body<JsonNode>()
                val sso = urlJsonNode["data"]["sso"]
                var cookie = ""
                sso.forEach {
                    val innerUrl = it.asText()
                    val innerResponse = client.submitForm(innerUrl, parameters {  }) {
                        headers {
                            mapOf("Referer" to "https://www.bilibili.com/", "Origin" to "https://www.bilibili.com",
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36")
                                .forEach { append(it.key, it.value) }
                        }
                    }
                    cookie = innerResponse.setCookie().renderCookieHeader()
                }
                val userid = RegexUtils.extract(cookie, "DedeUserID=", "; ")!!
                val fingerJsonNode = client.get("https://api.bilibili.com/x/frontend/finger/spi").body<JsonNode>()
                val fingerData = fingerJsonNode["data"]
                val fingerCookie = "buvid3=${fingerData["b_3"].asText()}; buvid4=${fingerData["b_4"].asText()}; "
                val biliBiliEntity = BiliBiliEntity()
                biliBiliEntity.cookie = cookie + fingerCookie
                biliBiliEntity.userid = userid
                biliBiliEntity.token = token
                biliBiliEntity
            }
            else -> error(data["message"].asText())
        }
    }

    suspend fun friendDynamic(biliBiliEntity: BiliBiliEntity): List<BiliBiliPojo> {
        val jsonNode = client.get("https://api.vc.bilibili.com/dynamic_svr/v1/dynamic_svr/dynamic_new?type_list=268435455") {
            cookieString(biliBiliEntity.cookie)
        }.body<JsonNode>()
        return if (jsonNode["code"].asInt() != 0) error("cookie已失效")
        else {
            val list = mutableListOf<BiliBiliPojo>()
            jsonNode["data"]["cards"].forEach{
                list.add(convert(it))
            }
            list
        }
    }


    suspend fun live(biliBiliEntity: BiliBiliEntity, page: Int = 1,
                     list: MutableList<BiliBiliLive> = mutableListOf()): List<BiliBiliLive> {
        val jsonNode = client.get("https://api.live.bilibili.com/xlive/web-ucenter/user/following?page=$page&page_size=29&ignoreRecord=1&hit_ab=true") {
            cookieString(biliBiliEntity.cookie)
        }.body<JsonNode>()
        jsonNode.check()
        val dataList = jsonNode["data"]["list"]
        return if (dataList.isEmpty) list
        else {
            for (node in dataList) {
                val title = node["title"].asText()
                val roomId = node["roomid"].asText()
                val url = "https://live.bilibili.com/$roomId"
                val id = node["uid"].asText()
                val imageUrl = node["room_cover"].asText()
                val status = node["live_status"].asInt()
                val uname = node["uname"].asText()
                list.add(BiliBiliLive(title, id, url, imageUrl, status == 1, uname))
            }
            delay(1000)
            live(biliBiliEntity, page + 1, list)
        }

    }

    suspend fun liveSign(biliBiliEntity: BiliBiliEntity): String {
        val jsonNode = client.get("https://api.live.bilibili.com/xlive/web-ucenter/v1/sign/DoSign") {
            cookieString(biliBiliEntity.cookie)
        }.body<JsonNode>()
        return if (jsonNode["code"].asInt() == 0) "成功"
        else error(jsonNode["message"].asText())
    }

    suspend fun like(biliBiliEntity: BiliBiliEntity, id: String, isLike: Boolean) {
        val map = mapOf("uid" to biliBiliEntity.userid, "dynamic_id" to id,
            "up" to if (isLike) "1" else "2", "csrf_token" to biliBiliEntity.token)
        val jsonNode = client.submitForm("https://api.vc.bilibili.com/dynamic_like/v1/dynamic_like/thumb",
            parameters { map.forEach { append(it.key, it.value) } }) {
            cookieString(biliBiliEntity.cookie)
        }.body<JsonNode>()
        if (jsonNode["code"].asInt() != 0) error("赞动态失败，${jsonNode["message"].asText()}")
    }

    suspend fun comment(biliBiliEntity: BiliBiliEntity, rid: String, type: String, content: String) {
        val map = mapOf("oid" to rid, "type" to type, "message" to content, "plat" to "1",
            "jsoup" to "jsoup", "csrf_token" to biliBiliEntity.token)
        val jsonNode = client.submitForm("https://api.bilibili.com/x/v2/reply/add",
            parameters { map.forEach { append(it.key, it.value) } }) {
            cookieString(biliBiliEntity.cookie)
        }.body<JsonNode>()
        if (jsonNode["code"].asInt() != 0) error("评论动态失败，${jsonNode["message"].asText()}")
    }

    suspend fun forward(biliBiliEntity: BiliBiliEntity, id: String, content: String) {
        val map = mapOf("uid" to biliBiliEntity.userid, "dynamic_id" to id,
            "content" to content, "extension" to "{\"emoji_type\":1}", "at_uids" to "", "ctrl" to "[]",
            "csrf_token" to biliBiliEntity.token)
        val jsonNode = client.submitForm("https://api.vc.bilibili.com/dynamic_repost/v1/dynamic_repost/repost", parameters {
            map.forEach { append(it.key, it.value) }
        }) {
            cookieString(biliBiliEntity.cookie)
        }.body<JsonNode>()
        if (jsonNode["code"].asInt() != 0) error("转发动态失败，${jsonNode["message"].asText()}")
    }

    suspend fun tossCoin(biliBiliEntity: BiliBiliEntity, rid: String, count: Int = 1) {
        val map = mapOf("aid" to rid, "multiply" to count.toString(), "select_like" to "1",
            "cross_domain" to "true", "csrf" to biliBiliEntity.token)
        val jsonNode = client.submitForm("https://api.bilibili.com/x/web-interface/coin/add", parameters {
            map.forEach { append(it.key, it.value) }
        }) {
            cookieString(biliBiliEntity.cookie)
        }.body<JsonNode>()
        if (jsonNode["code"].asInt() != 0) error("对该动态（视频）投硬币失败，${jsonNode["message"].asText()}")
    }

    suspend fun favorites(biliBiliEntity: BiliBiliEntity, rid: String, name: String) {
        val userid = biliBiliEntity.userid
        val cookie = biliBiliEntity.cookie
        val token = biliBiliEntity.token
        val firstJsonNode = client.get("https://api.bilibili.com/x/v3/fav/folder/created/list-all?type=2&rid=$rid&up_mid=$userid") {
            cookieString(cookie)
        }.body<JsonNode>()
        if (firstJsonNode["code"].asInt() != 0) error("收藏失败，请重新绑定哔哩哔哩")
        val jsonArray = firstJsonNode["data"]["list"]
        var favoriteId: String? = null
        for (obj in jsonArray) {
            if (obj["title"].asText() == name) {
                favoriteId = obj["id"].asText()
            }
        }
        if (favoriteId == null) {
            val map = mapOf("title" to name, "privacy" to "0", "jsonp" to "jsonp", "csrf" to token)
            val jsonNode = client.submitForm("https://api.bilibili.com/x/v3/fav/folder/add",
                parameters { map.forEach { append(it.key, it.value) } }) {
                cookieString(cookie)
            }.body<JsonNode>()
            if (jsonNode["code"].asInt() != 0) error("您并没有该收藏夹，而且创建该收藏夹失败，请重试！！")
            favoriteId = jsonNode["data"]["id"].asText()
        }
        val map = mapOf("rid" to rid, "type" to "2", "add_media_ids" to favoriteId,
            "del_media_ids" to "", "jsonp" to "jsonp", "csrf" to token)
        val jsonNode = client.submitForm("https://api.bilibili.com/x/v3/fav/resource/deal",
            parameters { map.forEach { append(it.key, it.value ?: "") } }) {
            cookieString(cookie)
        }.body<JsonNode>()
        if (jsonNode["code"].asInt() != 0) error("收藏视频失败，" + jsonNode["message"].asText())
    }

    private suspend fun uploadImage(biliBiliEntity: BiliBiliEntity, byteArray: ByteArray): JsonNode {
        val jsonNode = client.submitFormWithBinaryData("https://api.vc.bilibili.com/api/v1/drawImage/upload",
            formData {
                append("biz", "draw")
                append("category", "daily")
                append("file_up", byteArray)
            }) {
            cookieString(biliBiliEntity.cookie)
        }.body<JsonNode>()
        return if (jsonNode["code"].asInt() == 0) jsonNode["data"]
        else error("图片上传失败，" + jsonNode["message"].asText())
    }

    suspend fun publishDynamic(biliBiliEntity: BiliBiliEntity, content: String, images: List<String>) {
        val jsonArray = Jackson.createArrayNode()
        images.forEach{
            jsonArray.addPOJO(uploadImage(biliBiliEntity, client.get(it).body()))
        }
        val map = mapOf("biz" to "3", "category" to "3", "type" to "0", "pictures" to jsonArray.toString(),
            "title" to "", "tags" to "", "description" to content, "content" to content, "setting" to "{\"copy_forbidden\":0,\"cachedTime\":0}",
            "from" to "create.dynamic.web", "up_choose_comment" to "0", "extension" to "{\"emoji_type\":1,\"from\":{\"emoji_type\":1},\"flag_cfg\":{}}",
            "at_uids" to "", "at_control" to "", "csrf_token" to biliBiliEntity.token)
        val jsonNode = client.submitForm("https://api.vc.bilibili.com/dynamic_svr/v1/dynamic_svr/create_draw", parameters {
            map.forEach { append(it.key, it.value) }
        }) {
            cookieString(biliBiliEntity.cookie)
        }.body<JsonNode>()
        if (jsonNode["code"].asInt() != 0) error("发布动态失败，" + jsonNode["message"].asText())
    }

    suspend fun ranking(biliBiliEntity: BiliBiliEntity): List<BiliBiliRanking> {
        val jsonNode = client.get("https://api.bilibili.com/x/web-interface/ranking/v2?rid=0&type=all") {
            referer("https://www.bilibili.com")
            cookieString(biliBiliEntity.cookie)
            userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
        }.body<JsonNode>()
        val jsonArray = jsonNode["data"]["list"]
        val list = mutableListOf<BiliBiliRanking>()
        for (singleJsonNode in jsonArray) {
            val biliBiliRanking = BiliBiliRanking()
            biliBiliRanking.aid = singleJsonNode["aid"].asText()
            biliBiliRanking.cid = singleJsonNode["cid"].asText()
            biliBiliRanking.title = singleJsonNode["title"].asText()
            biliBiliRanking.desc = singleJsonNode["desc"].asText()
            biliBiliRanking.username = singleJsonNode["owner"]["name"].asText()
            biliBiliRanking.dynamic = singleJsonNode["dynamic"].asText()
            biliBiliRanking.bv = singleJsonNode["bvid"].asText()
            biliBiliRanking.duration = singleJsonNode["bvid"].asInt()
            list.add(biliBiliRanking)
        }
        return list
    }

    suspend fun report(biliBiliEntity: BiliBiliEntity, aid: String, cid: String, proGRes: Int) {
        val map = mapOf("aid" to aid, "cid" to cid, "progres" to proGRes.toString(),
            "csrf" to biliBiliEntity.token)
        val jsonNode = client.submitForm("https://api.bilibili.com/x/v2/history/report",
            parameters { map.forEach { append(it.key, it.value) } }) {
            cookieString(biliBiliEntity.cookie)
        }.body<JsonNode>()
        if (jsonNode["code"].asInt() != 0) error(jsonNode["message"].asText())
    }

    private fun JsonNode.check() {
        if (this["code"].asInt() != 0) error(this["message"].asText())
    }

    suspend fun watchVideo(biliBiliEntity: BiliBiliEntity, biliBiliRanking: BiliBiliRanking) {
        val startTs = System.currentTimeMillis().toString()
        val map = mutableMapOf(
            "mid" to biliBiliEntity.userid,
            "aid" to biliBiliRanking.aid,
            "cid" to biliBiliRanking.cid,
            "part" to "1",
            "lv" to "5",
            "ftime" to System.currentTimeMillis().toString(),
            "stime" to startTs,
            "jsonp" to "jsonp",
            "type" to "3",
            "sub_type" to "0",
            "refer_url" to "",
            "spmid" to "333.788.0.0",
            "from_spmid" to "333.1007.tianma.1-1-1.click",
            "csrf" to biliBiliEntity.token,
        )
        val jsonNode = client.submitForm("https://api.bilibili.com/x/click-interface/click/web/h5",
            parameters {  map.forEach { (t, u) -> append(t, u) } }) {
            cookieString(biliBiliEntity.cookie)
        }.body<JsonNode>()
        jsonNode.check()
        delay(3000)
        map["start_ts"] = startTs
        map["dt"] = "2"
        map["play_type"] = "0"
        map["realtime"] = (biliBiliRanking.duration - 5).toString()
        map["played_time"] = (biliBiliRanking.duration - 1).toString()
        map["real_played_time"] = (biliBiliRanking.duration - 1).toString()
        map["quality"] = "80"
        map["video_duration"] = biliBiliRanking.duration.toString()
        map["last_play_progress_time"] = (biliBiliRanking.duration - 2).toString()
        map["max_play_progress_time"] = (biliBiliRanking.duration - 2).toString()
        val watchJsonNode = client.submitForm("https://api.bilibili.com/x/click-interface/web/heartbeat",
            parameters { map.forEach { (t, u) -> append(t, u) } }) {
            cookieString(biliBiliEntity.cookie)
        }.body<JsonNode>()
        watchJsonNode.check()
    }

    suspend fun share(biliBiliEntity: BiliBiliEntity, aid: String) {
        val jsonNode = client.submitForm("https://api.bilibili.com/x/web-interface/share/add",
            parameters {
                append("aid", aid)
                append("csrf", biliBiliEntity.token)
            }) {
            cookieString(biliBiliEntity.cookie)
        }.body<JsonNode>()
        if (jsonNode["code"].asInt() !in listOf(0, 71000)) error(jsonNode["message"].asText())
    }

    suspend fun getReplay(biliBiliEntity: BiliBiliEntity, oid: String, page: Int): List<BiliBiliReplay> {
        val jsonNode = client.get(
            "https://api.bilibili.com/x/v2/reply?callback=jQuery17207366906764958399_${System.currentTimeMillis()}&jsonp=jsonp&pn=$page&type=1&oid=$oid&sort=2&_=${System.currentTimeMillis()}") {
            cookieString(biliBiliEntity.cookie)
            referer("https://www.bilibili.com/")
        }.bodyAsText().jsonpToJsonNode()
        return if (jsonNode["code"].asInt() == 0) {
            val jsonArray = jsonNode["data"]["replies"]
            val list = mutableListOf<BiliBiliReplay>()
            for (obj in jsonArray) {
                val biliReplay = BiliBiliReplay(obj["rpid"].asText(), obj["content"]["message"].asText())
                list.add(biliReplay)
            }
            list
        }else listOf()
    }

    suspend fun reportComment(biliBiliEntity: BiliBiliEntity, oid: String, rpId: String, reason: Int) {
        // 违法违规 9   色情  2    低俗 10    赌博诈骗  12
        // 人身攻击  7   侵犯隐私 15
        // 垃圾广告 1   引战 4    剧透   5    刷屏   3      抢楼 16    内容不相关   8     青少年不良信息  17
        //  其他 0
        val map = mapOf("oid" to oid, "type" to "1", "rpid" to rpId, "reason" to reason.toString(),
            "content" to "", "ordering" to "heat", "jsonp" to "jsonp", "csrf" to biliBiliEntity.token)
        val jsonNode = client.submitForm("https://api.bilibili.com/x/v2/reply/report",
            parameters { map.forEach { append(it.key, it.value) } }) {
            cookieString(biliBiliEntity.cookie)
            referer("https://www.bilibili.com/")
        }.body<JsonNode>()
        if (jsonNode["code"].asInt() != 0) error("举报评论失败！！")
    }

    suspend fun getOidByBvId(bvId: String): String {
        val html = client.get("https://www.bilibili.com/video/$bvId").bodyAsText()
        val jsonStr = RegexUtils.extract(html, "INITIAL_STATE__=", ";\\(function\\(\\)")!!
        val jsonNode = jsonStr.toJsonNode()
        return jsonNode["aid"].asText()
    }

    suspend fun followed(biliBiliEntity: BiliBiliEntity): List<BiliBiliFollowed> {
        val list = mutableListOf<BiliBiliFollowed>()
        var i = 1
        while (true) {
            val jsonNode = onceFollowed(biliBiliEntity, i++)
            if (jsonNode["code"].asInt() == 0) {
                val jsonArray = jsonNode["data"]["list"]
                if (jsonArray.size() == 0) break
                for (any in jsonArray) {
                    list.add(BiliBiliFollowed(any["mid"].asText(), any["uname"].asText()))
                }
            } else error(jsonNode["message"].asText())
        }
        return list
    }

    private suspend fun onceFollowed(biliBiliEntity: BiliBiliEntity, i: Int): JsonNode {
        val headers = mapOf("referer" to "https://space.bilibili.com/${biliBiliEntity.userid}/fans/follow",
            "user-agent" to "", "cookie" to biliBiliEntity.cookie)
        return client.get("https://api.bilibili.com/x/relation/followings?vmid=${biliBiliEntity.userid}&pn=$i&ps=100&order=desc&order_type=attention&jsonp=jsonp&callback=__jp5") {
            headers {
                headers.forEach { (t, u) -> append(t, u) }
            }
        }.bodyAsText().jsonpToJsonNode()
    }


}

data class BiliBiliPojo(
    var userId: String = "",
    var name: String = "",
    var id: String = "",
    var rid: String = "",
    var type: Int = -1,
    var time: Long = 0,
    var text: String = "",
    var bvId: String = "",
    var ipFrom: String = "",
    var picList: MutableList<String> = mutableListOf(),
    var isForward: Boolean = false,
    var forwardUserId: String = "",
    var forwardName: String = "",
    var forwardId: String = "",
    var forwardTime: Long = 0,
    var forwardText: String = "",
    var forwardBvId: String = "",
    var forwardPicList: MutableList<String> = mutableListOf()
)

data class BiliBiliLive(
    var title: String = "",
    var id: String = "",
    var url: String = "",
    var imageUrl: String = "",
    var status: Boolean = false,
    var uname: String = ""
)

data class BiliBiliRanking(
    var aid: String = "",
    var cid: String = "",
    var title: String = "",
    var desc: String = "",
    var username: String = "",
    var dynamic: String = "",
    var bv: String = "",
    var duration: Int = 0
)

data class BiliBiliReplay(
    var id: String = "",
    var content: String = ""
)

data class BiliBiliFollowed(
    var id: String = "",
    var name: String = ""
)

data class BiliBiliQrcode(
    val url: String,
    val key: String
)
