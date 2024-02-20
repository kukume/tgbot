package me.kuku.telegram.logic

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import me.kuku.telegram.entity.IqyEntity
import me.kuku.telegram.exception.qrcodeNotScanned
import me.kuku.telegram.exception.qrcodeScanned
import me.kuku.utils.*

object IqyLogic {

    suspend fun login1(): IqyQrcode {
        val response = client.post("https://passport.iqiyi.com/apis/qrcode/gen_login_token.action") {
            setFormDataContent {
                append("agenttype", "1")
                append("app_version", "")
                append("device_id", "")
                append("device_name", "网页端")
                append("fromSDK", "1")
                append("ptid", "01010021010000000000")
                append("sdk_version", "1.0.0")
                append("surl", "1")
                append("device_auth_uid", "")
                append("new_device_auth", "")
            }
        }
        val jsonNode = response.body<JsonNode>()
        jsonNode.check()
        val data = jsonNode["data"]
        val token = data["token"].asText()
        val url = data["url"].asText()
        val enUrl = url.toUrlEncode()
        val salt = "35f4223bb8f6c8638dc91d94e9b16f5$enUrl".md5()
        val cookie = response.cookie()
        return IqyQrcode(token, url, "https://qrcode.iqiyipic.com/login/?data=$enUrl&property=0&salt=$salt&width=162&_=0.${MyUtils.randomNum(16)}",
            cookie)
    }

    suspend fun login2(qrcode: IqyQrcode): IqyEntity {
        val response = client.post("https://passport.iqiyi.com/apis/qrcode/is_token_login.action") {
            setFormDataContent {
                append("agenttype", "1")
                append("app_version", "")
                append("device_id", "")
                append("fromSDK", "1")
                append("ptid", "01010021010000000000")
                append("sdk_version", "1.0.0")
                append("token", qrcode.token)
                append("dfp", "a17fefafa0ba5e426c96ffc2a0e9d1516066b7e9eebfbc3ce429d53c05e305085e")
            }
        }
        val jsonNode = response.body<JsonNode>()
        // {"msg":"找不到用户登录信息，可能手机端尚未确认","code":"A00001"}
        val code = jsonNode["code"].asText()
        return when (code) {
            "A00001" -> qrcodeNotScanned()
            "P01006" -> qrcodeScanned()
            "A00000" -> {
                val data = jsonNode["data"]
                val authCookie = data["authcookie"].asText()
                val userid = data["userinfo"]["uid"].asLong()
                val platform = MyUtils.randomLetterLowerNum(16)
                val deviceId = MyUtils.randomLetterLowerNum(32)
                val cookie = response.cookie()
                val iqyEntity = IqyEntity()
                iqyEntity.authCookie = authCookie
                iqyEntity.userid = userid
                iqyEntity.platform = platform
                iqyEntity.deviceId = deviceId
                iqyEntity.qyId = deviceId
                iqyEntity.cookie = cookie
                iqyEntity.p00001 = OkUtils.cookie(cookie, "P00001") ?: error("没有找到P00001的cookie")
                iqyEntity
            }
            else -> error("未知错误")
        }
    }


    private fun JsonNode.check() {
        if (this["code"].asText() !in listOf("A00000", "Q00504")) error(this["msg"]?.asText() ?: this["message"].asText())
    }

    private fun messageId() = (System.currentTimeMillis() + MyUtils.randomNum(9).toLong()).toString().md5()

    private fun sign(iqyEntity: IqyEntity, taskCode: String, timestamp: Long): String {
        val str = "agentType=1|agentversion=1.0|appKey=basic_pcw|authCookie=${iqyEntity.authCookie}|qyid=${iqyEntity.qyId}|task_code=$taskCode|timestamp=$timestamp|typeCode=point|userId=${iqyEntity.userid}|UKobMjDMsDoScuWOfp6F"
        return str.md5()
    }

    private suspend fun receiveTask(iqyEntity: IqyEntity, taskCode: String) {
        val jsonNode = client.get("https://tc.vip.iqiyi.com/taskCenter/task/joinTask?P00001=${iqyEntity.p00001}&taskCode=${taskCode}&platform=${iqyEntity.platform}&lang=zh_CN&app_lm=cn")
            .body<JsonNode>()
        jsonNode.check()
    }

    private suspend fun receiveTaskAward(iqyEntity: IqyEntity, taskCode: String) {
        val jsonNode = client.get("https://tc.vip.iqiyi.com/taskCenter/task/getTaskRewards?P00001=${iqyEntity.p00001}&taskCode=$taskCode&dfp=&platform=${iqyEntity.platform}&lang=zh_CN&app_lm=cn&deviceID=${iqyEntity.deviceId}&token=&multiReward=1&fv=bed99b2cf5722bfe")
            .body<JsonNode>()
        jsonNode.check()
    }

    private suspend fun watchPage(iqyEntity: IqyEntity, taskCode: String) {
        val jsonNode = client.get("https://tc.vip.iqiyi.com/taskCenter/task/notify?taskCode=$taskCode&P00001=${iqyEntity.p00001}&platform=${iqyEntity.platform}&lang=cn&bizSource=component_browse_timing_tasks&_=${System.currentTimeMillis()}")
            .body<JsonNode>()
        jsonNode.check()
    }

    suspend fun task(iqyEntity: IqyEntity): IqyTask {
        val jsonNode = client.get("https://tc.vip.iqiyi.com/taskCenter/task/getTaskStatus?appname=PCW&messageId=${messageId()}&version=2.0&invokeType=outer_http&lang=zh_cn&P00001=${iqyEntity.p00001}&taskCode=8a2186bb5f7bedd4,b6e688905d4e7184,acf8adbb5870eb29,843376c6b3e2bf00,8ba31f70013989a8,CHANGE_SKIN&newH5=1&fv=bed99b2cf5722bfe")
            .body<JsonNode>()
        jsonNode.check()
        return jsonNode["data"].convertValue()
    }

    private suspend fun taskSign(iqyEntity: IqyEntity, taskCode: String) {
        val timestamp = System.currentTimeMillis()
        val sign = sign(iqyEntity, taskCode, timestamp)
        val jsonNode = client.post("https://community.iqiyi.com/openApi/task/execute?agentType=1&agentversion=1.0&appKey=basic_pcw&authCookie=${iqyEntity.authCookie}&qyid=${iqyEntity.qyId}&task_code=$taskCode&timestamp=$timestamp&typeCode=point&userId=${iqyEntity.userid}&sign=$sign") {
            setJsonBody("""
                {"$taskCode":{"agentType":1,"agentversion":1,"authCookie":"${iqyEntity.authCookie}","dfp":"","qyid":"${iqyEntity.qyId}","verticalCode":"iQIYI","taskCode":"iQIYI_mofhr"}}
            """.trimIndent())
        }.body<JsonNode>()
        jsonNode.check()
    }

    suspend fun taskSign(iqyEntity: IqyEntity) {
        taskSign(iqyEntity, "natural_month_sign")
        taskSign(iqyEntity, "natural_month_sign_process")
    }

    private suspend fun taskWatch(iqyEntity: IqyEntity, taskCode: String) {
        receiveTask(iqyEntity, taskCode)
        watchPage(iqyEntity, taskCode)
        receiveTaskAward(iqyEntity, taskCode)
    }

    suspend fun finishTaskWatch(iqyEntity: IqyEntity) {
        taskWatch(iqyEntity, "b6e688905d4e7184")
        taskWatch(iqyEntity, "a7f02e895ccbf416")
        taskWatch(iqyEntity, "GetReward")
    }

}

class IqyTask {
    var finishedCount: Int = 0
    var count: Int = 0
    var tasks: List<Task> = mutableListOf()
    class Task {
        var name: String = ""
        var taskCode: String = ""
        var priority: Int = 0
        var status: Int = 0
        var belong: String = ""
        var group: String = ""
        var joinTime: String = ""
        var rewardTime: String = ""
        var taskGiftValue: Int = 0
        var taskTitle: String = ""
        @JsonProperty("taskDescrip")
        var taskDescription: String = ""
        var androidUrl: String = ""
        var bonusDes: String = ""
    }
}

data class IqyQrcode(val token: String, val url: String, val imageUrl: String, val cookie: String)