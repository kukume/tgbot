package me.kuku.telegram.logic

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import me.kuku.telegram.entity.BuffEntity
import me.kuku.utils.*

object BuffLogic {

    suspend fun login1(phone: String) {
        val jsonNode = client.get("https://api.kukuqaq.com/buff/login?phone=$phone").body<JsonNode>()
        if (!jsonNode["success"].asBoolean()) error(jsonNode["message"].asText())
        val sliderJsonNode = client.get("https://api.kukuqaq.com/buff/slider?phone=$phone").body<JsonNode>()
        if (!sliderJsonNode["success"].asBoolean()) error(sliderJsonNode["message"].asText())
    }

    suspend fun login2(phone: String, code: String): BuffEntity {
        val jsonNode = client.get("https://api.kukuqaq.com/buff/code?phone=$phone&code=$code").body<JsonNode>()
        if (jsonNode["success"]?.asBoolean() == false) error(jsonNode["message"].asText())
        val cookie = jsonNode["data"].asText()
        val csrf = OkUtils.cookie(cookie, "csrf_token") ?: error("cookie无效，请重新登陆")
        return BuffEntity().also {
            it.cookie = cookie
            it.csrf = csrf
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

    suspend fun sell(id: Int, min: Double? = null, max: Double? = null): List<Accessory> {
        val jsonNode = client.get("https://buff.163.com/api/market/goods/sell_order?game=csgo&goods_id=$id&page_num=1&sort_by=default&mode=&allow_tradable_cooldown=1${if (min != null && max != null) "&min_paintwear=0.15&max_paintwear=0.18" else ""}&_=${System.currentTimeMillis()}")
            .body<JsonNode>()
        if (jsonNode["code"].asText() != "OK") error(jsonNode["error"]?.asText() ?: "未知错误")
        val data = jsonNode["data"]
        val goods = data["goods_infos"][id.toString()]
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

data class PaintWearInterval(val min: Double, val max: Double)

data class Accessory(val goodsId: Int, var sellId: String, val name: String, val paintWear: Double, val price: Double, val description: String, val userid: Int, val fraudWarnings: String)

data class GoodsInfo(val id: Int, val name: String, val hashName: String, val shortName: String, val steamPrice: Double, val steamPriceCny: Double)