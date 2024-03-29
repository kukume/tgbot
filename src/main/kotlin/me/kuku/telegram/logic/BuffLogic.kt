package me.kuku.telegram.logic

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import me.kuku.telegram.entity.BuffEntity
import me.kuku.telegram.exception.qrcodeExpire
import me.kuku.telegram.exception.qrcodeNotScanned
import me.kuku.telegram.exception.qrcodeScanned
import me.kuku.utils.*
import java.lang.IllegalStateException

object BuffLogic {


    suspend fun login1(): BuffQrcode {
        val response = client.get("https://buff.163.com/account/api/qr_code_login_open?_=${System.currentTimeMillis()}")
        val cookie = response.cookie()
        val jsonNode = client.post("https://buff.163.com/account/api/qr_code_create") {
            setJsonBody("""
                {"code_type":1,"extra_param":"{}"}
            """.trimIndent())
            userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/119.0")
            cookieString(cookie)
        }.body<JsonNode>()
        return BuffQrcode(jsonNode["data"]["url"].asText(), jsonNode["data"]["code_id"].asText(), cookie)
    }

    suspend fun login2(qrcode: BuffQrcode): BuffEntity {
        val response = client.get("https://buff.163.com/account/api/qr_code_poll?item_id=${qrcode.code.toUrlEncode()}&_=${System.currentTimeMillis()}") {
            headers {
                cookieString(qrcode.cookie)
            }
            userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/119.0")
        }
        val jsonNode = response.body<JsonNode>()
        val state = jsonNode["data"]["state"].asInt()
        return when(state) {
            1 -> qrcodeNotScanned()
            2 -> qrcodeScanned()
            3 -> {
                val cookie = response.cookie() + qrcode.cookie
                val csrfToken = OkUtils.cookie(cookie, "csrf_token") ?: error("获取token失败，登陆失败")
                val loginResponse = client.post("https://buff.163.com/account/api/qr_code_login") {
                    setJsonBody("""
                        {"item_id":"${qrcode.code}"}
                    """.trimIndent())
                    cookieString(cookie)
                    referer("https://buff.163.com/")
                    userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/119.0")
                    headers {
                        append("X-CSRFToken", csrfToken)
                    }
                }
                val newCookie = loginResponse.cookie()
                val newCsrfToken = OkUtils.cookie(cookie, "csrf_token") ?: error("获取token失败，登陆失败")
                BuffEntity().also {
                    it.cookie = newCookie
                    it.csrf = newCsrfToken
                }
            }
            5 -> qrcodeExpire()
            else -> qrcodeExpire()
        }
    }

    suspend fun search(buffEntity: BuffEntity, name: String): List<BuffSearch> {
        val jsonNode = client.get("https://buff.163.com/api/market/search/suggest?text=${name.toUrlEncode()}&game=csgo") {
            headers {
                cookieString(buffEntity.cookie)
            }
        }.body<JsonNode>()
        if (jsonNode["code"].asText() != "OK") error(jsonNode["error"]?.asText() ?: "未知错误")
        val list = mutableListOf<BuffSearch>()
        for (node in jsonNode["data"]["suggestions"]) {
            val id = node["goods_ids"].asInt()
            val text = node["option"].asText()
            list.add(BuffSearch(id, text))
        }
        return list
    }

    suspend fun paintWear(id: Int): List<PaintWearInterval> {
        val html = client.get("https://buff.163.com/goods/$id?from=market#tab=selling&page_num=1").bodyAsText()
        val sortJsonStr = MyUtils.regex("(?<=sort_by_fields: )[\\s\\S]*?(?=type)", html)!!.trim()
            .removeSuffix(",")
        val sortNode = Jackson.parse(sortJsonStr)
        var b = true
        out@for (node in sortNode["list"]) {
            val nodeList = node["list"]
            for (innerNode in nodeList) {
                if (innerNode["title"].asText().contains("磨损")) {
                    b = false
                    break@out
                }
            }
        }
        if (b) error("该饰品不具备磨损属性")
        val jsonStr = MyUtils.regex("paintwear_choices: ", "]],", html)?.plus("]]") ?: return listOf()
        val list = mutableListOf<PaintWearInterval>()
        val jsonNode = Jackson.parse(jsonStr)
        for (node in jsonNode) {
            val min = node[0].asDouble()
            val max = node[1].asDouble()
            list.add(PaintWearInterval(min, max))
        }
        return list
    }

    suspend fun goodsInfo(id: Int): GoodsInfo {
        val jsonNode = client.get("https://buff.163.com/api/market/goods/sell_order?game=csgo&goods_id=$id&page_num=1&sort_by=default&mode=&allow_tradable_cooldown=1&_=${System.currentTimeMillis()}")
            .body<JsonNode>()
        if (jsonNode["code"].asText() != "OK") error(jsonNode["error"]?.asText() ?: "未知错误")
        val node = jsonNode["data"]["goods_infos"][id.toString()]
        val name = node["name"].asText()
        val shortName = node["short_name"].asText()
        val hashName = node["market_hash_name"].asText()
        val steamPrice = node["steam_price"].asDouble()
        val steamPriceCny = node["steam_price_cny"].asDouble()
        return GoodsInfo(id, name, hashName, shortName, steamPrice, steamPriceCny)
    }

    suspend fun sell(buffEntity: BuffEntity, id: Int, min: Double? = null, max: Double? = null, pageSize: Int = 10, pageNum: Int = 1): List<Accessory> {
        //                                     https://buff.163.com/api/market/goods/sell_order?game=csgo&goods_id=912923&page_num=1&sort_by=default&mode=&allow_tradable_cooldown=1&_=1683264283697
        val jsonNode = client.get("https://buff.163.com/api/market/goods/sell_order?game=csgo&goods_id=$id&page_num=$pageNum&page_size=$pageSize&sort_by=default&mode=&allow_tradable_cooldown=1${if (min != null && max != null) "&min_paintwear=$min&max_paintwear=$max" else ""}&_=${System.currentTimeMillis()}") {
            headers {
                cookieString(buffEntity.cookie)
            }
        }.body<JsonNode>()
        if (jsonNode["code"].asText() != "OK") error(jsonNode["error"]?.asText() ?: "未知错误")
        val data = jsonNode["data"]
        val goods = data["goods_infos"][id.toString()] ?: return listOf()
        val name = goods["name"].asText()
        val items = data["items"]
        val list = mutableListOf<Accessory>()
        for (item in items) {
            val assetInfo = item["asset_info"]
            val goodsId = assetInfo["goods_id"].asInt()
            val paintWear = assetInfo["paintwear"].asDouble()
            val description = item["description"].asText()
            val sellId = item["id"].asText()
            val userid = item["user_id"].asInt()
            val price = item["price"].asDouble()
            val info = assetInfo["info"]
            val fraudWarnings = info["fraudwarnings"].asText()
            list.add(Accessory(goodsId, sellId, name, paintWear, price, description, userid, fraudWarnings))
        }
        return list
    }

    suspend fun sellRepeat(buffEntity: BuffEntity, id: Int, min: Double? = null, max: Double? = null, pageSize: Int = 10): List<Accessory> {
        val allList = mutableListOf<Accessory>()
        var i = 1
        var status = true
        while (true) {
            val list = try {
                sell(buffEntity, id, min, max, pageSize, i++)
            } catch (e: IllegalStateException) {
                delay(1000 * 30)
                if (!status) {
                    error("获取失败，异常：" + e.message)
                }
                status = false
                continue
            }
            status = true
            delay(10000)
            allList.addAll(list)
            if (list.isEmpty()) break
        }
        return allList
    }

    // 3 buff 余额 支付宝  10  支付宝花呗  1 buff余额 银行卡  6 微信
    suspend fun buy(buffEntity: BuffEntity, sellId: String, goodsId: Int, price: Double, payMethod: Int = 6) {
        val cookie = buffEntity.cookie
        val csrf = OkUtils.cookie(cookie, "csrf_token") ?: error("cookie中不存在csrf")
        val jsonNode = client.get("https://buff.163.com/api/market/goods/buy/preview?game=csgo&sell_order_id=$sellId&goods_id=$goodsId&price=$price&allow_tradable_cooldown=0&cdkey_id=&_=${System.currentTimeMillis()}") {
            headers {
                cookieString(cookie)
            }
        }.body<JsonNode>()
        if (jsonNode["code"].asText() != "OK") error(jsonNode["error"]?.asText() ?: "未知错误")
        val buyJsonNode = client.post("https://buff.163.com/api/market/goods/buy") {
            contentType(ContentType.Application.Json)
            setBody("""
                {"game":"csgo","goods_id":$goodsId,"sell_order_id":"$sellId","price":$price,"pay_method":$payMethod,"allow_tradable_cooldown":0,"token":"","cdkey_id":""}
            """.trimIndent())
            headers {
                cookieString(cookie)
                append("x-csrftoken", csrf)
                referer("https://buff.163.com/goods/$goodsId")
                userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36")
            }
        }.body<JsonNode>()
        if (buyJsonNode["code"].asText() != "OK") error(buyJsonNode["error"]?.asText() ?: "未知错误")
    }


}


data class BuffSearch(val id: Int, val name: String)

data class PaintWearInterval(val min: Double, val max: Double) {
    fun min() = if (min == 0.0) null else min
    fun max() = if (max == 0.0) null else max
}

data class Accessory(val goodsId: Int, var sellId: String, val name: String, val paintWear: Double, val price: Double, val description: String, val userid: Int, val fraudWarnings: String) {
    fun price() = if (price == 0.0) null else price
}

data class GoodsInfo(val id: Int, val name: String, val hashName: String, val shortName: String, val steamPrice: Double, val steamPriceCny: Double)

data class BuffQrcode(val url: String, val code: String, val cookie: String)