package me.kuku.telegram.logic

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import me.kuku.pojo.CommonResult
import me.kuku.pojo.UA
import me.kuku.telegram.config.api
import me.kuku.telegram.entity.NetEaseEntity
import me.kuku.utils.*
import okhttp3.internal.toHexString

object NetEaseLogic {

    private const val domain = "https://music.163.com"

    private const val ua = "NeteaseMusic/8.7.22.220331222744(8007022);Dalvik/2.1.0 (Linux; U; Android 12; M2007J3SC Build/SKQ1.211006.001)"

    private fun aesEncode(secretData: String, secret: String): String {
        val vi = "0102030405060708"
        return AESUtils.encrypt(secretData, secret, vi)!!
    }

    private fun prepare(map: Map<String, String>, netEaseEntity: NetEaseEntity? = null): Map<String, String> {
        return prepare(Jackson.toJsonString(map), netEaseEntity)
    }

    private fun HttpRequestBuilder.setParams(map: Map<String, String>, netEaseEntity: NetEaseEntity? = null) {
        setFormDataContent {
            prepare(Jackson.toJsonString(map), netEaseEntity).forEach { (k, v) ->
                append(k, v)
            }
        }
    }

    private fun prepare(json: String, netEaseEntity: NetEaseEntity? = null): Map<String, String> {
        val nonce = "0CoJUm6Qyw8W8jud"
        val secretKey = "TA3YiYCfY2dDJQgg"
        val encSecKey =
            "84ca47bca10bad09a6b04c5c927ef077d9b9f1e37098aa3eac6ea70eb59df0aa28b691b7e75e4f1f9831754919ea784c8f74fbfadf2898b0be17849fd656060162857830e241aba44991601f137624094c114ea8d17bce815b0cd4e5b8e2fbaba978c6d1d14dc3d1faf852bdd28818031ccdaaa13a6018e1024e2aae98844210"
        val jsonNode = json.toJsonNode()
        netEaseEntity?.let {
            (jsonNode as ObjectNode).put("csrf_token", netEaseEntity.csrf)
        }
        var param = aesEncode(jsonNode.toString(), nonce)
        param = aesEncode(param, secretKey)
        return mapOf("params" to param, "encSecKey" to encSecKey)
//        val nonce = "0CoJUm6Qyw8W8jud"
//        val secretKey = MyUtils.randomLetterLowerNum(16).toByteArray().hex().substring(0, 16)
//        val ss = BigInteger(HexUtils.byteArrayToHex(secretKey.reversed().toByteArray()), 16)
//            .pow("010001".toInt(16))
//        val sss = BigInteger("00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7", 16)
//        val d = ss.divideAndRemainder(sss)[1]
//        val encSecKey = d.toString(16)
//        val jsonNode = JSON.parseObject(json)
//        netEaseEntity?.let {
//            jsonNode["csrf_token"] = netEaseEntity.csrf
//        }
//        var param = aesEncode(jsonNode.toString(), nonce)
//        param = aesEncode(param, secretKey)
//        return mapOf("params" to param, "encSecKey" to encSecKey)
    }

    suspend fun login(phone: String, password: String): CommonResult<NetEaseEntity> {
        val map = mapOf("countrycode" to "86", "password" to if (password.length == 32) password else password.md5(), "phone" to phone,
            "rememberLogin" to "true")
        val response = OkHttpKtUtils.post("$domain/weapi/login/cellphone", prepare(map),
            mapOf("Referer" to "https://music.163.com", "User-Agent" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.30 Safari/537.36",
                "cookie" to "os=ios; appver=8.7.01; __remember_me=true; "))
        val jsonNode = OkUtils.json(response)
        return if (jsonNode["code"].asInt() == 200) {
            val cookie = OkUtils.cookie(response)
            val csrf = OkUtils.cookie(cookie, "__csrf")!!
            val musicU = OkUtils.cookie(cookie, "MUSIC_U")!!
            CommonResult.success(NetEaseEntity().also {
                it.csrf = csrf
                it.musicU = musicU
            })
        } else CommonResult.failure(jsonNode.getString("message"))
    }

    suspend fun qrcode(): String {
        val jsonNode = OkHttpKtUtils.postJson("$domain/weapi/login/qrcode/unikey", prepare("""{"type": 1}"""))
        return jsonNode.getString("unikey")
    }

    suspend fun checkQrcode(key: String): CommonResult<NetEaseEntity> {
        val response = OkHttpKtUtils.post("https://music.163.com/weapi/login/qrcode/client/login", prepare("""{"type":1,"key":"$key"}"""), mapOf("crypto" to "weapi"))
        val jsonNode = OkUtils.json(response)
        return when (jsonNode.getInteger("code")) {
            803 -> {
                val cookie = OkUtils.cookie(response)
                val csrf = OkUtils.cookie(cookie, "__csrf")
                val musicU = OkUtils.cookie(cookie, "MUSIC_U")
                CommonResult.success(NetEaseEntity().also {
                    it.csrf = csrf!!
                    it.musicU = musicU!!
                })
            }
            801 -> CommonResult.failure(code = 0, message = "等待扫码")
            802 -> CommonResult.failure(code = 1, message = "${jsonNode.getString("nickname")}已扫码，等待确认登陆")
            800 -> CommonResult.failure("二维码已过期")
            else -> CommonResult.failure(jsonNode.getString("message"))
        }
    }

    suspend fun sign(netEaseEntity: NetEaseEntity) {
        val map = mapOf("type" to "0")
        val jsonNode = OkHttpKtUtils.postJson("$domain/weapi/point/dailyTask", prepare(map),
            mapOf("cookie" to netEaseEntity.cookie(), "crypto" to "webapi", "referer" to "https://music.163.com/discover",
                "user-agent" to UA.PC.value, "origin" to "https://music.163.com")
        )
        val code = jsonNode.getInteger("code")
        if (code != 200 && code != -2) error(jsonNode.getString("message"))
    }

    private suspend fun recommend(netEaseEntity: NetEaseEntity): MutableList<String> {
        val jsonNode = OkHttpKtUtils.postJson("$domain/weapi/v1/discovery/recommend/resource",
            prepare(mapOf("csrf_token" to netEaseEntity.csrf)), OkUtils.headers(netEaseEntity.cookie(), domain, UA.PC)
        )
        return when (jsonNode.getInteger("code")) {
            200 -> {
                val jsonArray = jsonNode["recommend"]
                val list = mutableListOf<String>()
                jsonArray.forEach { list.add(it.getString("id")) }
                list
            }
            301 -> error("您的网易云音乐cookie已失效")
            else -> error(jsonNode.getString("message"))
        }
    }


    private suspend fun songId(playListId: String): JsonNode {
        val jsonNode = OkHttpKtUtils.postJson("$domain/weapi/v3/playlist/detail",
            prepare(mapOf("id" to playListId, "total" to "true", "limit" to "1000", "n" to "1000"))
        )
        return jsonNode["playlist"]["trackIds"]
    }

    suspend fun listenMusic(netEaseEntity: NetEaseEntity) {
        val playList = recommend(netEaseEntity)
        val ids = Jackson.createArrayNode()
        while (ids.size() < 310) {
            val songIds = songId(playList.random())
            var k = 0
            while (ids.size() < 310 && k < songIds.size()) {
                val jsonNode = Jackson.createObjectNode()
                jsonNode.put("download", 0)
                jsonNode.put("end", "playend")
                jsonNode.put("id", songIds[k].getInteger("id"))
                jsonNode.put("sourceId", "")
                jsonNode.put("time", 240)
                jsonNode.put("type", "song")
                jsonNode.put("wifi", "0")
                val totalJsonNode = Jackson.createObjectNode()
                totalJsonNode.set<ObjectNode>("json", jsonNode)
                totalJsonNode.put("action", "play")
                ids.add(totalJsonNode)
                k++
            }
        }
        val jsonNode = OkHttpKtUtils.postJson("$domain/weapi/feedback/weblog", prepare(mapOf("logs" to ids.toString())),
            OkUtils.headers(netEaseEntity.cookie(), domain, UA.PC))
        if (jsonNode.getInteger("code") != 200) error(jsonNode.getString("message"))
    }

    private suspend fun musicianStageMission(netEaseEntity: NetEaseEntity): MutableList<Mission> {
        val jsonNode = OkHttpKtUtils.postJson("$domain/weapi/nmusician/workbench/mission/stage/list", prepare(mapOf()),
            OkUtils.headers(netEaseEntity.cookie(), domain, UA.PC))
        return if (jsonNode.getInteger("code") == 200) {
            val jsonArray = jsonNode["data"]["list"]
            val list = mutableListOf<Mission>()
            jsonArray.forEach {
                list.add(Mission(it["userStageTargetList"][0]["userMissionId"]?.asLong(), it.getInteger("period"), it.getInteger("type"), it.getString("description")))
            }
            list
        } else error(jsonNode.getString("message"))
    }

    private suspend fun musicianCycleMission(netEaseEntity: NetEaseEntity): List<Mission> {
        val jsonNode = OkHttpKtUtils.postJson("$domain/weapi/nmusician/workbench/mission/cycle/list",
            prepare(mapOf("actionType" to "", "platform" to "")),
            OkUtils.headers(netEaseEntity.cookie(), domain, UA.PC))
        return if (jsonNode.getInteger("code") == 200) {
            val jsonArray = jsonNode["data"]["list"]
            val list = mutableListOf<Mission>()
            jsonArray.forEach {
                list.add(Mission(it.get("userMissionId")?.asLong(), it.getInteger("period"), it.getInteger("type"), it.getString("description")))
            }
            list
        } else error(jsonNode.getString("message"))
    }

    private suspend fun musicianReceive(netEaseEntity: NetEaseEntity, mission: Mission) {
        val missionId = mission.userMissionId?.toString() ?: error("userMissionId为空")
        val jsonNode = OkHttpKtUtils.postJson("https://interface.music.163.com/weapi/nmusician/workbench/mission/reward/obtain/new",
            prepare(mapOf("userMissionId" to missionId, "period" to mission.period.toString())),
            OkUtils.headers(netEaseEntity.androidCookie(), domain, "Mozilla/5.0 (Linux; Android 12; M2007J3SC Build/SKQ1.211006.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/120.0.6099.210 Mobile Safari/537.36 CloudMusic/0.1.2 NeteaseMusic/9.0.10")
        )
        val code = jsonNode.getInteger("code")
        if (code != 200 && code != -2) error(jsonNode.getString("message"))
    }

    private suspend fun userAccess(netEaseEntity: NetEaseEntity) {
        val jsonNode = OkHttpKtUtils.postJson("$domain/weapi/creator/user/access", prepare(mapOf()), OkUtils.headers(netEaseEntity.cookie(), domain, UA.PC))
        if (jsonNode.getInteger("code") != 200) error(jsonNode.getString("message"))
    }

    suspend fun musicianSign(netEaseEntity: NetEaseEntity) {
        userAccess(netEaseEntity)
        val list = musicianCycleMission(netEaseEntity)
        for (mission in list) {
            if (mission.description == "音乐人中心签到") {
//                    if (mission.type != 100) {
//                        userAccess(netEaseEntity)
//                    }
                return musicianReceive(netEaseEntity, mission)
            }
        }
        error("没有找到音乐人签到任务")
    }

    private suspend fun myMusic(netEaseEntity: NetEaseEntity): List<NetEaseSong> {
        val jsonNode = OkHttpKtUtils.postJson("$domain/weapi/nmusician/production/common/artist/song/item/list/get?csrf_token=${netEaseEntity.csrf}",
            prepare(mapOf("fromBackend" to "0", "limit" to "10", "offset" to "0", "online" to "1")),
            mapOf("user-agent" to UA.PC.value, "cookie" to netEaseEntity.cookie(), "referer" to "https://music.163.com/nmusician/web/albums/work/actor/song/self/pub"))
        return if (jsonNode.getInteger("code") == 200) {
            val list = mutableListOf<NetEaseSong>()
            jsonNode["data"]["list"].forEach {
                list.add(NetEaseSong(it.getString("songName"), it.getLong("songId"), it.getLong("albumId"), it.getString("albumName")))
            }
            list
        } else error(jsonNode.getString("message"))
    }

    private suspend fun personalizedPlaylist(netEaseEntity: NetEaseEntity): List<Play> {
        val jsonNode = OkHttpKtUtils.postJson("$domain/weapi/personalized/playlist",
            prepare(mapOf("limit" to "9")), OkUtils.headers(netEaseEntity.cookie(), domain, UA.PC)
        )
        return if (jsonNode.getInteger("code") == 200) {
            val list = mutableListOf<Play>()
            jsonNode["result"].forEach {
                list.add(Play(it.getString("name"), it.getLong("id"), it.getLong("playCount")))
            }
            list
        } else error(jsonNode["message"]?.asText() ?: "获取失败")
    }

    private suspend fun shareResource(netEaseEntity: NetEaseEntity, id: Long, message: String = "每日分享"): Long {
        val jsonNode = OkHttpKtUtils.postJson("$domain/weapi/share/friends/resource",
            prepare(mapOf("type" to "playlist", "id" to id.toString(), "message" to message)),
            mapOf("cookie" to netEaseEntity.cookie(), "referer" to domain, "user-agent" to UA.PC.value)
        )
        return if (jsonNode.getInteger("code") == 200)
            jsonNode.getLong("id")
        else error(jsonNode.getString("message"))
    }

    private suspend fun removeDy(netEaseEntity: NetEaseEntity, id: Long) {
        val jsonNode = OkHttpKtUtils.postJson("$domain/weapi/event/delete",
            prepare(mapOf("id" to id.toString())),
            OkUtils.headers(netEaseEntity.pcCookie(), domain, UA.PC)
        )
        if (jsonNode.getInteger("code") != 200) error(jsonNode["message"]?.asText() ?: "删除动态失败")
    }

    private suspend fun finishCycleMission(netEaseEntity: NetEaseEntity, name: String) {
        val list = musicianCycleMission(netEaseEntity)
        for (mission in list) {
            if (mission.description == name) {
                if (mission.type != 100) {
                    userAccess(netEaseEntity)
                }
                return musicianReceive(netEaseEntity, mission)
            }
        }
        error("没有找到音乐人签到任务：$name")
    }

    private suspend fun finishStageMission(netEaseEntity: NetEaseEntity, name: String) {
        val list = musicianStageMission(netEaseEntity)
        for (mission in list) {
            if (mission.description == name) {
                if (mission.type != 100) {
                    userAccess(netEaseEntity)
                }
                return musicianReceive(netEaseEntity, mission)
            }
        }
        error("没有找到音乐人签到任务：$name")
    }

    suspend fun publish(netEaseEntity: NetEaseEntity) {
        val list = myMusic(netEaseEntity)
        val netEaseSong = list.random()
        val commentId = shareMySong(netEaseEntity, netEaseSong.songId)
        removeDy(netEaseEntity, commentId)
        finishStageMission(netEaseEntity, "发布动态")
    }

    private suspend fun mLogNosToken(netEaseEntity: NetEaseEntity, url: String): MLogInfo {
        val bizKey = StringBuilder()
        for (i in 0..8) {
            bizKey.append(MyUtils.randomInt(0, 15).toHexString().replace("0x", ""))
        }
        val fileName = "album.jpg"
        val bytes =
            OkHttpKtUtils.getBytes(url)
        val size = bytes.size
        val md5 = bytes.md5()
        val jsonNode = OkHttpKtUtils.postJson("$domain/weapi/nos/token/whalealloc",
            prepare(mapOf("bizKey" to bizKey.toString(), "filename" to fileName, "bucket" to "yyimgs",
                "md5" to md5, "type" to "image", "fileSize" to size.toString())),
            OkUtils.headers(netEaseEntity.cookie(), domain, UA.PC)
        )
        return if (jsonNode.getInteger("code") == 200) {
            val dataJsonNode = jsonNode["data"]
            MLogInfo(dataJsonNode.getLong("resourceId"), dataJsonNode.getString("objectKey"), dataJsonNode.getString("token"),
                dataJsonNode.getString("bucket"), bytes)
        } else error(jsonNode.getString("message"))
    }

    private suspend fun uploadFile(netEaseEntity: NetEaseEntity, mLogInfo: MLogInfo): UploadFileInfo {
        val url = "http://45.127.129.8/${mLogInfo.bucket}/${mLogInfo.objectKey}?offset=0&complete=true&version=1.0"
        val contentType = "image/jpg"
        val jsonNode = OkHttpKtUtils.postJson(url, OkUtils.streamBody(mLogInfo.byteArray, contentType),
            mapOf("x-nos-token" to mLogInfo.token, "cookie" to netEaseEntity.cookie(), "referer" to domain))
        return Jackson.parseObject(Jackson.toJsonString(jsonNode))
    }

    private suspend fun songDetail(netEaseEntity: NetEaseEntity, id: Long): SongDetail {
        val jsonNode = OkHttpKtUtils.postJson("$domain/weapi/v3/song/detail",
            prepare("""
                {"c": "[{\"id\": $id}]", "ids": "[$id]"}
            """.trimIndent(), netEaseEntity),
            OkUtils.headers(netEaseEntity.cookie(), domain, UA.PC)
        )
        return if (jsonNode.getInteger("code") == 200) {
            val songJsonNode = jsonNode["songs"][0]
            SongDetail(songJsonNode.getString("name"), songJsonNode["ar"][0].getString("name"),
                songJsonNode["al"].getString("picUrl") + "?param=500y500")
        } else error(jsonNode.getString("message"))
    }

    suspend fun publishMLog(netEaseEntity: NetEaseEntity) {
        val list = myMusic(netEaseEntity)
        val songId = list.random().songId
        val songDetail = songDetail(netEaseEntity, songId)
        val mLogInfo = mLogNosToken(netEaseEntity, songDetail.pic)
        uploadFile(netEaseEntity, mLogInfo)
        val songName = songDetail.name
        val text = "分享${songDetail.artistName}的歌曲: ${songDetail.name}"
        val jsonStr = """
            {"type":1,"mlog":"{\"content\": {\"image\": [{\"height\": 500, \"width\": 500, \"more\": false, \"nosKey\": \"${mLogInfo.bucket}/${mLogInfo.objectKey}\", \"picKey\": ${mLogInfo.resourceId}}], \"needAudio\": false, \"song\": {\"endTime\": 0, \"name\": \"${songName}\", \"songId\": $songId, \"startTime\": 30000}, \"text\": \"$text\"}, \"from\": 0, \"type\": 1}"}
        """.trimIndent()
        val jsonNode = OkHttpKtUtils.postJson("$domain/weapi/mlog/publish/v1", prepare(jsonStr), OkUtils.headers(netEaseEntity.cookie(), domain, UA.PC))
        return if (jsonNode.getInteger("code") == 200) {
            val resourceId = jsonNode["data"]["event"]["info"].getLong("resourceId")
            removeDy(netEaseEntity, resourceId)
//            finishCycleMission(netEaseEntity, "发布mlog")
        } else error(jsonNode.getString("message"))
    }

    private suspend fun musicComment(netEaseEntity: NetEaseEntity, id: Long, comment: String = "欢迎大家收听"): Long {
        val jsonNode = OkHttpKtUtils.postJson("$domain/weapi/v1/resource/comments/add",
            prepare(mapOf("threadId" to "R_SO_4_$id", "content" to comment)),
            OkUtils.headers(netEaseEntity.cookie().replace("os=pc; ", "") + "os=android; ", domain, ua)
        )
        jsonNode.check()
        return jsonNode["comment"].getLong("commentId")
    }

    private suspend fun userid(netEaseEntity: NetEaseEntity): String {
        val html = client.get("https://music.163.com/discover") {
            cookieString(netEaseEntity.cookie())
            userAgent(UA.PC.value)
        }.body<String>()
        return MyUtils.regex("\\{userId:", ",", html) ?: error("获取userid失败")
    }

    private suspend fun dynamicComment(netEaseEntity: NetEaseEntity, id: Long, comment: String = "评论"): Long {
        val userid = userid(netEaseEntity)
        val jsonNode = OkHttpKtUtils.postJson("$domain/weapi/resource/comments/add",
            prepare(mapOf("threadId" to "A_EV_2_${id}_$userid", "content" to comment)),
            OkUtils.headers(netEaseEntity.cookie().replace("os=pc; ", "") + "os=android; ", domain, ua)
        )
        return if (jsonNode.getInteger("code") == 200) {
            jsonNode["comment"].getLong("commentId")
        } else error(jsonNode.getString("message"))
    }

    private suspend fun deleteMusicComment(netEaseEntity: NetEaseEntity, id: Long, commentId: Long) {
        val jsonNode = OkHttpKtUtils.postJson("$domain/weapi/resource/comments/delete",
            prepare(mapOf("commentId" to commentId.toString(), "threadId" to "R_SO_4_$id")),
            OkUtils.headers(netEaseEntity.pcCookie(), domain, UA.PC)
        )
        if (jsonNode.getInteger("code") != 200) error(jsonNode.getString("message"))
    }

    // /api/nmusician/workbench/upgrade/production/song/publish/list
    // {"limit":10,"offset":0,"publishStatus":200,"operations":"[\\"MANAGE_SONG\\",\\"EDIT_SONG\\",\\"OFFLINE_SONG\\",\\"SET_SCHEDULE_TIME\\"]","onlyBeat":false}

    suspend fun myMusicComment(netEaseEntity: NetEaseEntity) {
        val list = myMusic(netEaseEntity)
        val netEaseSong = list.random()
        val commentId1 = musicComment(netEaseEntity, netEaseSong.songId)
        delay(2000)
        deleteMusicComment(netEaseEntity, netEaseSong.songId, commentId1)
        val commentId2 = musicComment(netEaseEntity, netEaseSong.songId)
        delay(2000)
        deleteMusicComment(netEaseEntity, netEaseSong.songId, commentId2)
//        finishStageMission(netEaseEntity, "发表主创说")
    }

    private fun JsonNode.check() {
        if (this["code"].asInt() != 200) error(this["message"].asText())
    }

    /**
     * https://interface.music.163.com/api/vipnewcenter/app/level/task/reward/getall
     * https://interface.music.163.com/weapi/batch?csrf_token=b04a98c9c484bba93a1bcc16147ac6e7
     * {"/api/vipnewcenter/app/level/myvip":"","/api/vipnewcenter/app/vipcenter/level/privilege/guide":"","/api/music-vip-membership/front/vip/info":"","/api/vip-center-bff/task/list":"","/api/vipnewcenter/app/user/max/score":""}
     */
    suspend fun vipSign(netEaseEntity: NetEaseEntity) {
        val jsonNode = client.post("https://interface.music.163.com/weapi/vip-center-bff/task/sign?csrf_token=${netEaseEntity.csrf}") {
            setParams(mapOf())
            cookieString(netEaseEntity.cookie())
        }.body<JsonNode>()
        jsonNode.check()
    }

    suspend fun receiveTaskReward(netEaseEntity: NetEaseEntity) {
        val jsonNode = client.post("https://interface.music.163.com/api/vipnewcenter/app/level/task/reward/getall") {
            setParams(mapOf())
            cookieString(netEaseEntity.cookie())
        }.body<JsonNode>()
        jsonNode.check()
    }

    private suspend fun shareMySong(netEaseEntity: NetEaseEntity, songId: Long, msg: String = "每日分享"): Long {
        val checkTokenNode = client.post("$api/exec/netEase/checkToken").body<JsonNode>()
        val checkToken = checkTokenNode["checkToken"]?.asText() ?: error("获取checkToken失败，请重试或检查api")
        val jsonNode = client.post("$domain/weapi/share/friends/resource") {
            setParams(mapOf("type" to "song", "id" to songId.toString(), "msg" to msg,
                "uuid" to "publish-${System.currentTimeMillis()}${MyUtils.randomNum(5)}", "checkToken" to checkToken))
            cookieString(netEaseEntity.cookie())
        }.body<JsonNode>()
        jsonNode.check()
        return jsonNode["id"].asLong()
    }

    suspend fun shareMySong(netEaseEntity: NetEaseEntity) {
        val list = myMusic(netEaseEntity)
        val netEaseSong = list.random()
        val commentId = shareMySong(netEaseEntity, netEaseSong.songId)
        removeDy(netEaseEntity, commentId)
        finishCycleMission(netEaseEntity, "在动态分享歌曲")
    }

    suspend fun commentMyDy(netEaseEntity: NetEaseEntity) {
        val list = myMusic(netEaseEntity)
        val netEaseSong = list.random()
        val commentId = shareMySong(netEaseEntity, netEaseSong.songId)
        dynamicComment(netEaseEntity, commentId)
        removeDy(netEaseEntity, commentId)
        finishCycleMission(netEaseEntity, "在自己动态下发布评论")
    }

    suspend fun publishAndShareMySongAndComment(netEaseEntity: NetEaseEntity) {
        val list = myMusic(netEaseEntity)
        val netEaseSong = list.random()
        val commentId = shareMySong(netEaseEntity, netEaseSong.songId)
        dynamicComment(netEaseEntity, commentId)
        removeDy(netEaseEntity, commentId)
        delay(5000)
        finishStageMission(netEaseEntity, "发布动态")
        finishCycleMission(netEaseEntity, "在动态分享歌曲")
        finishCycleMission(netEaseEntity, "在自己动态下发布评论")
    }

}

data class Mission(val userMissionId: Long?, val period: Int, val type: Int, val description: String = "")

data class NetEaseSong(val songName: String, val songId: Long, val albumId: Long, val albumName: String)

data class Play(val name: String, val id: Long, val playCount: Long)

data class MLogInfo(val resourceId: Long, val objectKey: String, val token: String, val bucket: String, val byteArray: ByteArray)

data class UploadFileInfo(var callbackRetmessage: String = "", var offset: Long = 0, var requestId: String = "", var context: String = "", var callbackRetMsg: String = "")

data class SongDetail(val name: String, val artistName: String, val pic: String)
