package me.kuku.telegram.logic

import me.kuku.pojo.CommonResult
import me.kuku.utils.*
import okhttp3.MultipartBody
import org.jsoup.Jsoup
import org.springframework.stereotype.Service
import java.nio.charset.Charset

@Service
class ToolLogic {

    suspend fun baiKe(text: String): String {

        suspend fun baiKeByUrl(url: String): CommonResult<String> {
            var response = OkHttpKtUtils.get(url)
            while (response.code == 302) {
                response.close()
                val location = response.header("location")!!
                if (location.startsWith("//baike.baidu.com/search/none")) return CommonResult.failure("")
                val resultUrl = if (location.startsWith("//")) "https:$location"
                else "https://baike.baidu.com$location"
                response = OkHttpKtUtils.get(resultUrl)
            }
            val html = OkUtils.str(response)
            val doc = Jsoup.parse(html)
            val result = doc.select(".lemma-summary .para").first()?.text()
                ?: return CommonResult.failure(code = 210, message = "", data = "https://baike.baidu.com" + doc.select("li[class=list-dot list-dot-paddingleft]").first()?.getElementsByTag("a")?.first()?.attr("href"))
            return CommonResult.success(result)
        }

        val encodeText = text.toUrlEncode()
        val url = "https://baike.baidu.com/search/word?word=$encodeText"
        val result = baiKeByUrl(url)
        return if (result.success())
            """
                ${result.data}
                查看详情： $url
            """.trimIndent()
        else if (result.code == 210) {
            val resultUrl = result.data()
            """
                ${baiKeByUrl(resultUrl).data}
                查看详情：$resultUrl
            """.trimIndent()
        } else """
            抱歉，没有找到与"$text"相关的百科结果
        """.trimIndent()
    }

    suspend fun saucenao(url: String): List<SaucenaoResult> {
        val urlJsonNode = OkHttpKtUtils.getJson("https://saucenao.com/search.php?output_type=2&numres=16&url=${url.toUrlEncode()}&api_key=${"TW1GbE5qUTVNalF3TkRObVltVmtOemxrTkRVM1lUUm1OVEUzTmpZNE5XRXdOR1UyWlRRM1lnPT0=".base64Decode().toString(Charset.defaultCharset()).base64Decode().toString(Charset.defaultCharset())}")
        if (urlJsonNode.get("header").getInteger("status") != 0) error(urlJsonNode.get("header").getString("message"))
        val jsonList = urlJsonNode.get("results")
        val list = mutableListOf<SaucenaoResult>()
        for (jsonNode in jsonList) {
            val header = jsonNode.get("header")
            val data = jsonNode.get("data")
            val similarity = header["similarity"]?.asText() ?: ""
            val thumbnail = header["thumbnail"]?.asText() ?: ""
            val indexName = header["index_name"]?.asText() ?: ""
            val extUrls = data.get("ext_urls")?.let {
                val letList = mutableListOf<String>()
                it.forEach { k -> letList.add(k.asText()) }
                letList
            } ?: listOf()
            val author = data.get("creator_name")?.asText() ?: data.get("member_name")?.asText() ?: data.get("author_name")?.asText() ?: ""
            val title = data.get("title")?.asText() ?: data.get("jp_name")?.asText() ?: ""
            val authorUrl = data.get("author_url")?.asText() ?: ""
            list.add(SaucenaoResult(similarity, thumbnail, indexName, extUrls, title, author, authorUrl).also {
                it.daId = data["da_id"]?.asLong() ?: 0
                it.pixivId = data["pixiv_id"]?.asLong() ?: 0
                it.faId = data["fa_id"]?.asLong() ?: 0
                it.tweetId = data["tweet_id"]?.asLong() ?: 0
            })
        }
        return list
    }

    suspend fun dCloudUpload(url: String): String {
        val jsonNode = OkHttpKtUtils.postJson("https://api.kukuqaq.com/upload", MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("type", "3")
            .addFormDataPart("file", "${MyUtils.randomLetter(6)}.jpg", OkUtils.streamBody(OkHttpKtUtils.getBytes(url))).build())
        return jsonNode["url"]?.asText() ?: error(jsonNode["message"].asText())
    }

}

data class SaucenaoResult(
    val similarity: String, val thumbnail: String, val indexName: String, val extUrls: List<String>, val title: String, val author: String, val authUrl: String,
    var daId: Long? = null, var pixivId: Long? = null, var faId: Long? = null, var tweetId: Long? = null
)