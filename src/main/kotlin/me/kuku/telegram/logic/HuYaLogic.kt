package me.kuku.telegram.logic

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import me.kuku.telegram.entity.HuYaEntity
import me.kuku.telegram.exception.qrcodeExpire
import me.kuku.telegram.exception.qrcodeNotScanned
import me.kuku.telegram.utils.*
import org.springframework.stereotype.Service

@Service
class HuYaLogic {

    suspend fun getQrcode(): HuYaQrcode {
        val requestId = RandomUtils.num(8)
        val response = client.post("https://udblgn.huya.com/qrLgn/getQrId") {
            setJsonBody("""
                {"uri":"70001","version":"2.4","context":"WB-b11031a6ccf245169759e35fc6adc5d9-C9D11B3412B00001BAEA164B1FD4176D-","requestId":"$requestId","appId":"5002","data":{"behavior":"%7B%22a%22%3A%22m%22%2C%22w%22%3A520%2C%22h%22%3A340%2C%22b%22%3A%5B%5D%7D","type":"","domainList":"","page":"https%3A%2F%2Fwww.huya.com%2F"}}
            """.trimIndent())
        }
        val jsonNode = response.body<JsonNode>()
        val qrId = jsonNode["data"]["qrId"].asText()
        return HuYaQrcode("https://udblgn.huya.com/qrLgn/getQrImg?k=$qrId&appId=5002", qrId, response.setCookie().renderCookieHeader(), requestId)
    }

    suspend fun checkQrcode(huYaQrcode: HuYaQrcode): HuYaEntity {
        val response = client.post("https://udblgn.huya.com/qrLgn/tryQrLogin") {
            setJsonBody("""
                {"uri":"70003","version":"2.4","context":"WB-b11031a6ccf245169759e35fc6adc5d9-C9D11B3412B00001BAEA164B1FD4176D-","requestId":"${huYaQrcode.requestId}","appId":"5002","data":{"qrId":"${huYaQrcode.id}","remember":"1","domainList":"","behavior":"%7B%22a%22%3A%22m%22%2C%22w%22%3A520%2C%22h%22%3A340%2C%22b%22%3A%5B%5D%7D","page":"https%3A%2F%2Fwww.huya.com%2F"}}
            """.trimIndent())
            cookieString(huYaQrcode.cookie)
        }
        val jsonNode = response.body<JsonNode>()
        return when (val stage = jsonNode["data"]["stage"].asInt()) {
            0, 1 -> qrcodeNotScanned()
            2 -> {
                val cookie = response.setCookie().renderCookieHeader()
                HuYaEntity().also {
                    it.cookie = cookie
                }
            }
            5 -> qrcodeExpire()
            else -> error("错误代码为$stage")
        }
    }

    suspend fun live(huYaEntity: HuYaEntity): List<HuYaLive> {
        var i = 0
        val resultList = mutableListOf<HuYaLive>()
        while (true) {
            val response = client.get("https://live.huya.com/liveHttpUI/getUserSubscribeToInfoList?iPageIndex=${i++}&_=${System.currentTimeMillis()}") {
                cookieString(huYaEntity.cookie)
            }
            if (response.status == HttpStatusCode.OK) {
                val jsonNode = response.body<JsonNode>()
                val list = jsonNode["vItems"]
                if (list.isEmpty) break
                for (ss in list) {
                    val huYaLive = HuYaLive(ss["iRoomId"].asLong(), ss["sLiveDesc"].asText(), ss["sGameName"].asText(),
                        ss["iIsLive"].asInt() == 1, ss["sNick"].asText(), ss["sVideoCaptureUrl"].asText(), "https://www.huya.com/${ss["iRoomId"].asLong()}")
                    resultList.add(huYaLive)
                }
            } else error("查询失败，可能cookie已失效")
        }
        return resultList
    }

}

data class HuYaQrcode(val url: String, val id: String, val cookie: String, val requestId: String)

data class HuYaLive(val roomId: Long, val liveDesc: String, val gameName: String, val isLive: Boolean, val nick: String, val videoCaptureUrl: String, val url: String)