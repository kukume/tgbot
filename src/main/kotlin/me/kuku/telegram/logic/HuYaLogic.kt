package me.kuku.telegram.logic

import me.kuku.pojo.CommonResult
import me.kuku.pojo.UA
import me.kuku.telegram.entity.HuYaEntity
import me.kuku.utils.*
import org.springframework.stereotype.Service

@Service
class HuYaLogic {

    suspend fun getQrcode(): HuYaQrcode {
        val requestId = MyUtils.randomNum(8)
        val response = OkHttpKtUtils.post("https://udblgn.huya.com/qrLgn/getQrId", OkUtils.json("""
            {"uri":"70001","version":"2.4","context":"WB-b11031a6ccf245169759e35fc6adc5d9-C9D11B3412B00001BAEA164B1FD4176D-","requestId":"$requestId","appId":"5002","data":{"behavior":"%7B%22a%22%3A%22m%22%2C%22w%22%3A520%2C%22h%22%3A340%2C%22b%22%3A%5B%5D%7D","type":"","domainList":"","page":"https%3A%2F%2Fwww.huya.com%2F"}}
        """.trimIndent()))
        val jsonNode = OkUtils.json(response)
        val qrId = jsonNode["data"].getString("qrId")
        return HuYaQrcode("https://udblgn.huya.com/qrLgn/getQrImg?k=$qrId&appId=5002", qrId, OkUtils.cookie(response), requestId)
    }

    suspend fun checkQrcode(huYaQrcode: HuYaQrcode): CommonResult<HuYaEntity> {
        val response = OkHttpKtUtils.post("https://udblgn.huya.com/qrLgn/tryQrLogin", OkUtils.json("""
            {"uri":"70003","version":"2.4","context":"WB-b11031a6ccf245169759e35fc6adc5d9-C9D11B3412B00001BAEA164B1FD4176D-","requestId":"${huYaQrcode.requestId}","appId":"5002","data":{"qrId":"${huYaQrcode.id}","remember":"1","domainList":"","behavior":"%7B%22a%22%3A%22m%22%2C%22w%22%3A520%2C%22h%22%3A340%2C%22b%22%3A%5B%5D%7D","page":"https%3A%2F%2Fwww.huya.com%2F"}}
        """.trimIndent()), OkUtils.cookie(huYaQrcode.cookie))
        val jsonNode = OkUtils.json(response)
        return when (val stage = jsonNode["data"].getInteger("stage")) {
            0, 1 -> CommonResult.failure("等待扫码", null, 0)
            2 -> {
                val cookie = OkUtils.cookie(response)
                CommonResult.success(HuYaEntity().also {
                    it.cookie = cookie
                })
            }
            5 -> CommonResult.failure("二维码已失效")
            else -> CommonResult.failure("错误代码为$stage")
        }
    }

    fun live(huYaEntity: HuYaEntity): CommonResult<List<HuYaLive>> {
        var i = 0
        val resultList = mutableListOf<HuYaLive>()
        while (true) {
            val response = OkHttpUtils.get("https://live.huya.com/liveHttpUI/getUserSubscribeToInfoList?iPageIndex=${i++}&_=${System.currentTimeMillis()}",
                OkUtils.headers(huYaEntity.cookie, "", UA.PC))
            if (response.code == 200) {
                val jsonNode = OkUtils.json(response)
                val list = jsonNode["vItems"]
                if (list.isEmpty) break
                for (ss in list) {
                    val huYaLive = HuYaLive(ss.getLong("iRoomId"), ss.getString("sLiveDesc"), ss.getString("sGameName"),
                        ss.getInteger("iIsLive") == 1, ss.getString("sNick"), ss.getString("sVideoCaptureUrl"), "https://www.huya.com/${ss.getLong("iRoomId")}")
                    resultList.add(huYaLive)
                }
            } else return CommonResult.failure<List<HuYaLive>>("查询失败，可能cookie已失效").also { response.close() }
        }
        return CommonResult.success(resultList)
    }

}

data class HuYaQrcode(val url: String, val id: String, val cookie: String, val requestId: String)

data class HuYaLive(val roomId: Long, val liveDesc: String, val gameName: String, val isLive: Boolean, val nick: String, val videoCaptureUrl: String, val url: String)