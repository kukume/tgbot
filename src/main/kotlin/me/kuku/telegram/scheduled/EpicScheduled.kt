package me.kuku.telegram.scheduled

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import me.kuku.telegram.config.Cron
import me.kuku.telegram.config.telegramBot
import me.kuku.telegram.context.sendPic
import me.kuku.telegram.entity.ConfigService
import me.kuku.telegram.entity.Status
import me.kuku.utils.DateTimeFormatterUtils
import me.kuku.utils.MyUtils
import me.kuku.utils.client
import me.kuku.utils.toJsonNode
import java.time.ZoneOffset

@Cron("1h")
suspend fun pushFreeGame() {
    val list = ConfigService.findByEpicFreeGamePush(Status.ON)
    if (list.isEmpty()) return
    val jsonNode = client.get("https://store-site-backend-static-ipv4.ak.epicgames.com/freeGamesPromotions?locale=zh-CN&country=US&allowCountries=US")
        .body<JsonNode>()
    val elements = jsonNode["data"]["Catalog"]["searchStore"]["elements"].filter { it["offerType"].asText() in listOf("OTHERS", "BASE_GAME") }
    for (element in elements) {
        val promotion = element["promotions"]?.get("promotionalOffers")?.get(0)?.get("promotionalOffers")?.get(0)
            ?: element["promotions"]?.get("upcomingPromotionalOffers")?.get(0)?.get("promotionalOffers")?.get(0)  ?: continue
        val startDate = promotion["startDate"].asText().replace(".000Z", "")
        val startTimeStamp = DateTimeFormatterUtils.parseToLocalDateTime(startDate, "yyyy-MM-dd'T'HH:mm:ss")
            .toInstant(ZoneOffset.of("+0")).toEpochMilli()
        val nowTimeStamp = System.currentTimeMillis()
        val diff = nowTimeStamp - startTimeStamp
        if (diff < 1000 * 60 * 60 && diff > 0) {
            val title = element["title"].asText()
            val imageUrl = element["keyImages"][0]["url"].asText()
            val slug = element["productSlug"].takeIf { it !is NullNode }?.asText() ?: element["catalogNs"]["mappings"][0]["pageSlug"].asText()
            val html =
                client.get("https://store.epicgames.com/zh-CN/p/$slug").bodyAsText()
            val queryJsonNode =
                MyUtils.regex("REACT_QUERY_INITIAL_QUERIES__ \\= ", ";", html)?.toJsonNode() ?: continue
            val queries = queryJsonNode["queries"]
            val mappings = queries.filter { it["queryKey"]?.get(0)?.asText() == "getCatalogOffer" }
            for (mapping in mappings) {
                val catalogOffer = mapping["state"]["data"]["Catalog"]["catalogOffer"]
                val offerType = catalogOffer["offerType"].asText()
                if (offerType != "BASE_GAME") continue
                val namespace = catalogOffer["namespace"].asText()
                val id = catalogOffer["id"].asText()
                val innerTitle = catalogOffer["title"].asText()
                val description = catalogOffer["description"].asText()
                val longDescription = catalogOffer["longDescription"].asText()
                val fmtPrice = catalogOffer["price"]["totalPrice"]["fmtPrice"]
                val originalPrice = fmtPrice["originalPrice"].asText()
                val discountPrice = fmtPrice["discountPrice"].asText()
                val url = "https://store.epicgames.com/purchase?highlightColor=0078f2&offers=1-$namespace-$id&showNavigation=true#/purchase/payment-methods"
                for (configEntity in list) {
                    telegramBot.sendPic(configEntity.tgId,
                        "#Epic免费游戏推送\n游戏名称: $title\n游戏内部名称: $innerTitle\n游戏描述: $description\n游戏长描述: $longDescription\n原价: $originalPrice\n折扣价: $discountPrice\n订单地址：$url",
                        listOf(imageUrl))
                }
            }
        }
    }
}