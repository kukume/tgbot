package me.kuku.telegram.logic

import io.ktor.client.request.*
import io.ktor.client.statement.*
import me.kuku.telegram.utils.client
import me.kuku.telegram.utils.toUrlEncode
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.springframework.stereotype.Service

@Suppress("DuplicatedCode")
@Service
class YgoLogic {

    suspend fun search(name: String): List<Card> {
        val str = client.get("https://ygocdb.com/?search=${name.toUrlEncode()}").bodyAsText()
        val elements = Jsoup.parse(str).select(".card")
        val list = mutableListOf<Card>()
        for (element in elements) {
            val spans = element.select("span")
            val chineseName = spans[0].text()
            val japaneseName = spans[1].text()
            val englishName = if (spans.size == 6) spans[2].text() else ""
            val cardPassword = if (spans.size == 6) spans[3].text() else spans[2].text()
            val a = element.select(".cardimg a").first()!!
            val href = a.attr("href")
            val url = "https://ygocdb.com$href"
            val imgUrl = a.select("img").first()!!.attr("data-original").replace("!half", "")
            val desc = element.select(".desc").first()!!
            val nameHtml = desc.select(".name").toString()
            val effect = desc.removeClass("name").html().replace(nameHtml, "").replace("<hr>", "\n").replace("<br>", "\n").replace("\n\n", "\n")
            list.add(Card(chineseName, japaneseName, englishName, cardPassword, effect, url, imgUrl))
        }
        return list
    }

    suspend fun searchDetail(id: Long): Card {
        val url = "https://ygocdb.com/card/$id"
        val str = client.get(url).bodyAsText()
        val document = Jsoup.parse(str)
        val imageUrl = document.select(".cardimg img").first()!!.attr("src")
        val spans = document.select(".detail .names span").filter { it -> it.attributes().isEmpty }
        val chineseName = spans[0].text()
        val japaneseName = spans[1].text()
        val englishName = spans[2].text()
        val cardPassword = spans.getOrNull(3)?.text() ?: ""
        val desc = document.select(".desc").first()!!
        val childNodes = desc.childNodes()
        val sb = StringBuilder()
        for (childNode in childNodes) {
            val toString = childNode.toString()
            if (toString == "<br>" || toString == "<hr>") sb.append("\n")
            else {
                when (childNode) {
                    is Element -> {
                        sb.append(childNode.text())
                    }
                    is TextNode -> {
                        sb.append(childNode.text())
                    }
                    else -> sb.append(childNode.toString())
                }
            }
        }
        val effect = sb.toString()
        return Card(chineseName, japaneseName, englishName, cardPassword, effect, url, imageUrl)
    }


}

data class Card(
    val chineseName: String,
    val japaneseName: String,
    val englishName: String,
    val cardPassword: String,
    val effect: String,
    val url: String,
    val imageUrl: String
)
