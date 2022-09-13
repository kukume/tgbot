package me.kuku.telegram.logic

import com.fasterxml.jackson.databind.node.NullNode
import me.kuku.pojo.CommonResult
import me.kuku.pojo.UA
import me.kuku.telegram.entity.DouYuEntity
import me.kuku.utils.*
import org.springframework.stereotype.Service

@Service
class DouYuLogic {

    private val qqApp = QqApp(716027609, 383, 101047385)


    suspend fun getQrcode(): DouYuQqQrcode {
        val response = OkHttpKtUtils.get("https://www.douyu.com/member/oauth/signin/qq?biz_type=1&ref_url=https%3A%2F%2Fwww.douyu.com%2F&room_id=0&cate_id=0&tag_id=0&child_id=0&vid=0&fac=&type=login&isMultiAccount=0").also { it.close() }
        val location = response.header("location")!!
        val cookie = OkUtils.cookie(response)
        val state = MyUtils.regex("state=", "&", location)
        val qrcode = QqQrCodeLoginUtils.getQrcode(qqApp)
        return DouYuQqQrcode(qrcode, state!!, cookie)
    }


    suspend fun checkQrcode(douYuQqQrcode: DouYuQqQrcode): CommonResult<DouYuEntity> {
        val ss = QqQrCodeLoginUtils.authorize(qqApp, douYuQqQrcode.qqLoginQrcode.sig, douYuQqQrcode.state, "https://www.douyu.com/member/oauth/signin/qq")
        return if (ss.failure()) CommonResult.failure(code = ss.code,  message = ss.message)
        else {
            val url = ss.data()
            val headers = OkUtils.headers(douYuQqQrcode.cookie, "https://graph.qq.com/", UA.PC)
            val firstResponse = OkHttpKtUtils.get(url, headers).apply { close() }
            val firstUrl = firstResponse.header("location")!!
            val secondResponse = OkHttpKtUtils.get(firstUrl, headers).apply { close() }
            val secondUrl = secondResponse.header("location")!!
            val thirdResponse = OkHttpKtUtils.get("https:$secondUrl", headers).apply { close() }
            val cookie = OkUtils.cookie(thirdResponse)
            CommonResult.success(DouYuEntity().also {
                it.cookie = cookie
            })
        }
    }

    suspend fun room(douYuEntity: DouYuEntity): CommonResult<List<DouYuRoom>> {
        var i = 1
        val resultList = mutableListOf<DouYuRoom>()
        while (true) {
            val jsonNode = OkHttpKtUtils.getJson("https://www.douyu.com/wgapi/livenc/liveweb/follow/list?sort=0&cid1=0&page=${i++}",
                OkUtils.headers(douYuEntity.cookie, "", UA.PC))
            if (jsonNode.getInteger("error") == 0) {
                val list = jsonNode["data"]["list"] ?: break
                if (list is NullNode) break
                for (singleJsonNode in list) {
                    val douYuRoom = DouYuRoom(singleJsonNode.getString("room_name"), singleJsonNode.getString("nickname"),
                        "https://www.douyu.com${singleJsonNode.getString("url")}", singleJsonNode.getString("game_name"), singleJsonNode.getInteger("show_status") == 1 && singleJsonNode.getInteger("videoLoop") == 0 ,
                        singleJsonNode.getString("online"), singleJsonNode.getLong("room_id"))
                    resultList.add(douYuRoom)
                }
            } else return CommonResult.failure(jsonNode.getString("msg"))
        }
        return CommonResult.success(resultList)
    }

}

data class DouYuQqQrcode(val qqLoginQrcode: QqLoginQrcode, val state: String, val cookie: String)

data class DouYuRoom(val name: String, val nickName: String, val url: String, val gameName: String, val showStatus: Boolean, val online: String, val roomId: Long)