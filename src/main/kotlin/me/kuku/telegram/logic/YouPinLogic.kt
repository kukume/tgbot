package me.kuku.telegram.logic

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import me.kuku.telegram.entity.YouPinEntity
import me.kuku.utils.*
import java.util.UUID

object YouPinLogic {

    private const val ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36"

    suspend fun uk(): String {
        val time = DateTimeFormatterUtils.formatNow("HH:mm:ss")
        val json = """
            {"availHeight":1019,"availWidth":1707,"appCodeName":"Mozilla","appName":"Netscape","hardwareConcurrency":20,"language":"zh-CN","languages":["zh-CN"],"onLine":true,"platform":"Win32","product":"Gecko","productSub":"20030107","userAgent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36","vendor":"Google Inc.","vendorSub":"","plugins":"487f7b22f68312d2c1bbc93b1aea445b","doNotTrack":"","innerWidth":244,"innerHeight":916,"dateGMT":"$time GMT+0800 (中国标准时间)","src":"pc","bcn":"u","client_time":${System.currentTimeMillis()},"iud":"ca02d4d6-1473-4a53-acb1-31af388cf8f3","fonts":["nc_iconfont","ncpc_iconfont"],"cv":"ec355cd745d02d67028a50dca7c3216c","devtoolData":[${System.currentTimeMillis()}]}
        """.trimIndent()
        val w = AESUtils.aesEncrypt("DGlNrfZIKr3MCT0e", json)!!.base64Encode()
        val text = client.post("https://s.youpin898.com/api/w") {
            setBody(w)
            contentType(ContentType.Text.Plain)
            headers {
                append("D", "p=p&b=u&v=1")
            }
        }.bodyAsText()
        val ss = String(AESUtils.aesDecrypt(
            "F5D1eG6jG7ubETY7".toByteArray(),
            text.base64Decode()
        )!!).toJsonNode()
        return ss["u"].asText()
    }

    private fun HeadersBuilder.uk(uk: String) {
        append("Uk", uk)
    }

    suspend fun smsLogin1(phone: String): YouPinLoginCache {
        val uk = uk()
        val uuid = UUID.randomUUID().toString()
        val jsonNode = client.post("https://api.youpin898.com/api/user/Auth/SendSignInSmsCode") {
            setJsonBody("""
                {"Mobile":"$phone","SessionId":"$uuid"}
            """.trimIndent()
            )
            headers {
                uk(uk)
                userAgent(ua)
            }
        }.body<JsonNode>()
        if (jsonNode["Code"].asInt() != 0) error(jsonNode["Msg"].asText())
        return YouPinLoginCache(uk, uuid, phone)
    }

    suspend fun smsLogin2(cache: YouPinLoginCache, code: String): YouPinEntity {
        val jsonNode = client.post("https://api.youpin898.com/api/user/Auth/SmsSignIn") {
            setJsonBody("""
                {"Mobile":"${cache.phone}","Code":"$code","SessionId":"${cache.uuid}","TenDay":1}
            """.trimIndent())
            headers {
                uk(cache.uk)
                userAgent(ua)
            }
        }.body<JsonNode>()
        if (jsonNode["Code"].asInt() != 0) error(jsonNode["Msg"].asText())
        val token = jsonNode["Data"]["Token"].asText()
        val entity = YouPinEntity().also {
            it.uk = cache.uk
            it.token = "Bearer $token"
        }
        val userInfo = userInfo(entity)
        entity.userid = userInfo.userid
        return entity
    }
    context(HttpRequestBuilder)
    private fun YouPinEntity.appendHeader() {
        headers {
            uk(this@appendHeader.uk)
            userAgent(ua)
            append("authorization", this@appendHeader.token)
        }
    }

    private fun JsonNode.check() {
        if (this["Code"].asInt() != 0) error(this["Msg"].asText())
    }

    suspend fun userInfo(youPinEntity: YouPinEntity): YouPinUserInfo {
        val jsonNode = client.get("https://api.youpin898.com/api/user/Account/GetUserInfo") {
            youPinEntity.appendHeader()
        }.body<JsonNode>().apply { check() }
        val data = jsonNode["Data"]
        return YouPinUserInfo(data["UserId"].asLong(), data["NickName"].asText(), data["Mobile"].asText(),
            data["Balance"].asDouble())
    }

    suspend fun match(youPinEntity: YouPinEntity, keyword: String): List<YouPinMatch> {
        val jsonNode = client.post("https://api.youpin898.com/api/homepage/search/match") {
            setJsonBody("""{"keyWords":"$keyword","listType":"10"}""")
            youPinEntity.appendHeader()
        }.body<JsonNode>()
        jsonNode.check()
        val list = mutableListOf<YouPinMatch>()
        for (node in jsonNode["Data"]["dataList"]) {
            list.add(YouPinMatch(node["commodityName"].asText(), node["templateId"].asInt()))
        }
        return list
    }

    suspend fun market(youPinEntity: YouPinEntity, templateId: Int, sellOrRent: Int = 1,
                       listSortType: Int = 1, sortType: Int = 1,
                       page: Int = 1, size: Int = 20, minAbrade: Double? = null, maxAbrade: Double? = null): YouPinMarket {
        // listSortType sortType  1 1 价格升序 1 2 价格降序 5 1磨损度升序 5 2 磨损度降序
        // 2 1 短租升序 2 2 短租降序 3 长租 4 押金
        val body = """
            {"stickersIsSort":false,"stickers":{}}
        """.trimIndent().toJsonNode() as ObjectNode
        body.put("templateId", templateId)
        body.put("listSortType", listSortType)
        body.put("sortType", sortType)
        if (sellOrRent == 1) {
            body.put("listType", 10)
        } else {
            body.put("listType", 30)
        }
        body.put("userId", youPinEntity.userid)
        body.put("pageIndex", page)
        body.put("pageSize", size)
        minAbrade?.let { body.put("minAbrade", it) }
        maxAbrade?.let { body.put("maxAbrade", it) }
        val jsonNode = client.post("https://api.youpin898.com/api/homepage/v2/es/commodity/GetCsGoPagedList") {
            setJsonBody(body)
            youPinEntity.appendHeader()
        }.body<JsonNode>()
        return jsonNode["Data"].convertValue()
    }



}

data class YouPinLoginCache(val uk: String, val uuid: String, val phone: String)

data class YouPinMatch(val commodityName: String, val templateId: Int)

data class YouPinUserInfo(val userid: Long, val nickname: String, val mobile: String, val balance: Double)

class YouPinMarket {
    @JsonProperty("CommodityList")
    var commodities: MutableList<Commodity>? = mutableListOf()
    @JsonProperty("Filters")
    var filters: MutableList<Filter> = mutableListOf()
    @JsonProperty("TemplateInfo")
    var templateInfo: TemplateInfo = TemplateInfo()

    fun haveAbrade() =
        filters.find { it.filterKey == "abrade" }?.isShow == true

    fun abrade() = filters.find { it.filterKey == "abrade" }


    class Commodity {
        @JsonProperty("Id")
        var id: Long = 0
        @JsonProperty("UserId")
        var userid: Long = 0
        @JsonProperty("GameId")
        var gameId: Long = 0
        @JsonProperty("CommodityNo")
        var no: String = ""
        @JsonProperty("CommodityName")
        var name: String = ""
        @JsonProperty("TemplateId")
        var templateId: Int = 0
        @JsonProperty("IconUrlLarge")
        var iconUrlLarge: String = ""
        @JsonProperty("Price")
        var price: Double = 0.0
        @JsonProperty("Remark")
        var remark: String? = ""
        @JsonProperty("UserNickName")
        var userNickname: String = ""
        @JsonProperty("Abrade")
        var abrade: Double = 0.0
        // 短租 1天
        @JsonProperty("LeaseUnitPrice")
        var leaseUnitPrice: Double = 0.0
        // 长租 1天
        @JsonProperty("LongLeaseUnitPrice")
        var longLeaseUnitPrice: Double = 0.0
        // 保证金
        @JsonProperty("LeaseDeposit")
        var leaseDeposit: Double = 0.0
        @JsonProperty("LeaseMaxDays")
        var leaseMaxDays: Int = 0
    }

    class Filter {
        @JsonProperty("Name")
        var name: String = ""
        @JsonProperty("FilterKey")
        var filterKey: String = ""
        @JsonProperty("ControlType")
        var controlType: Int = 0
        @JsonProperty("FilterType")
        var filterType: Int = 0
        @JsonProperty("IsShow")
        var isShow: Boolean = false
        @JsonProperty("SubName")
        var subName: String = ""
        @JsonProperty("MaxVal")
        var maxVal: Double = 0.0
        @JsonProperty("MinVal")
        var minVal: Double = 0.0
        @JsonProperty("Items")
        var items: MutableList<Item>? = mutableListOf()

        class Item {
            @JsonProperty("Name")
            var name: String = ""
            @JsonProperty("ValType")
            var valType: Boolean = false
            @JsonProperty("MaxVal")
            var maxVal: Double = 0.0
            @JsonProperty("MinVal")
            var minVal: Double = 0.0
            @JsonProperty("FixedVal")
            var fixedVal: Double = 0.0
            @JsonProperty("QueryString")
            var queryString: String = ""

        }
    }

    class TemplateInfo {
        @JsonProperty("CommodityName")
        var name: String = ""
        @JsonProperty("CommodityHashName")
        var hashName: String = ""
        @JsonProperty("GroupHashName")
        var groupHashName: String = ""
        @JsonProperty("SteamPrice")
        var steamPrice: Double = 0.0
        @JsonProperty("SteamUSDPrice")
        var steamUsdPrice: Double = 0.0
        @JsonProperty("MinPrice")
        var minPrice: Double = 0.0
        @JsonProperty("LeaseUnitPrice")
        var leaseUnitPrice: Double = 0.0
        @JsonProperty("LongLeaseUnitPrice")
        var longLeaseUnitPrice: Double = 0.0
        @JsonProperty("LeaseDeposit")
        var leaseDeposit: Double = 0.0
        @JsonProperty("OnSaleCount")
        var onSaleCount: Int = 0
        @JsonProperty("OnLeaseCount")
        var onLeaseCount: Int = 0
        @JsonProperty("TypeName")
        var typeName: String = ""
        @JsonProperty("Rarity")
        var rarity: String = ""
        @JsonProperty("Quality")
        var quality: String = ""
        @JsonProperty("Exterior")
        var exterior: String = ""
        @JsonProperty("ExteriorColor")
        var exteriorColor: String = ""
    }

}
