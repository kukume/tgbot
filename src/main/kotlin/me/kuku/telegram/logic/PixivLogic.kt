package me.kuku.telegram.logic

import me.kuku.telegram.entity.PixivEntity
import me.kuku.utils.OkHttpKtUtils
import me.kuku.utils.OkUtils
import java.io.InputStream

object PixivLogic {

    fun loginByWeibo() {

    }


    suspend fun followImage(pixivEntity: PixivEntity): List<PixivPojo> {
        val jsonNode = OkHttpKtUtils.getJson("https://www.pixiv.net/ajax/follow_latest/illust?p=1&mode=all&lang=zh",
            OkUtils.headers(pixivEntity.cookie, "https://www.pixiv.net/bookmark_new_illust.php"))
        if (jsonNode["error"].asBoolean()) error(jsonNode["message"].asText())
        val list = mutableListOf<PixivPojo>()
        for (node in jsonNode["body"]["thumbnails"]["illust"]) {
            val id = node["id"].asLong()
            val userid = node["userId"].asLong()
            val username = node["userName"].asText()
            val title = node["title"].asText()
            val alt = node["alt"].asText()
            val createDate = node["createDate"].asText()
            val updateDate = node["updateDate"].asText()
            val pojo = PixivPojo(id, userid, username, title, alt, createDate, updateDate)
            node["tags"].forEach {
                pojo.tags.add(it.asText())
            }
            list.add(pojo)
        }
        return list
    }

    suspend fun imageById(id: Long): List<String> {
        val jsonNode = OkHttpKtUtils.getJson("https://www.pixiv.net/ajax/illust/$id/pages?lang=zh",
            OkUtils.referer("https://www.pixiv.net/artworks/101070399"))
        if (jsonNode["error"].asBoolean()) error(jsonNode["message"].asText())
        val list = mutableListOf<String>()
        val body = jsonNode["body"]
        for (node in body) {
            val url = node["urls"]["original"].asText()
            list.add(url)
        }
        return list
    }

    fun convertStr(pixivPojo: PixivPojo): String {
        val sb = StringBuilder()
        sb.appendLine("#${pixivPojo.username}")
            .appendLine("标题：${pixivPojo.title}")
            .appendLine("描述：${pixivPojo.alt}")
            .appendLine("创建时间：${pixivPojo.createDate}")
            .appendLine("更新时间：${pixivPojo.updateDate}")
            .append("tag：")
        pixivPojo.tags.forEach {
            sb.append("$it ")
        }
        return sb.toString()
    }

    suspend fun imageIs(url: String): InputStream {
        return OkHttpKtUtils.getByteStream(url, OkUtils.referer("https://www.pixiv.net"))
    }


}

data class PixivPojo(val id: Long, val userid: Long, val username: String, val title: String, val alt: String,
                     val createDate: String, val updateDate: String,
                     val tags: MutableList<String> = mutableListOf())
