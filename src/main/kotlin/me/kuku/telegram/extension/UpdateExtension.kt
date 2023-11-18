package me.kuku.telegram.extension

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.kuku.telegram.context.MixSubscribe
import me.kuku.telegram.context.Privacy
import me.kuku.utils.client
import me.kuku.utils.convertValue
import me.kuku.utils.setJsonBody
import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import java.io.FileOutputStream
import java.io.InputStream

@Component
class UpdateExtension {

    val mutex = Mutex()

    fun MixSubscribe.update() {

        ability {
            sub(name = "update", privacy = Privacy.CREATOR) {
                val os = System.getProperty("os.name")
                if (os.lowercase().contains("windows")) error("不支持windows系统")
                val files = listFile("/tgbot")
                val list = mutableListOf<Array<InlineKeyboardButton>>()
                files.forEach {
                    list.add(arrayOf(InlineKeyboardButton(it.name).callbackData("update|${it.name}")))
                }
                sendMessage("请选择需要更新的文件\n/updatelog可查询github提交日志", replyKeyboard = InlineKeyboardMarkup(*list.toTypedArray()))
            }
        }

        telegram {
            callbackStartsWith("update|") {
                mutex.withLock {
                    editMessageText("下载指定jar中")
                    val suffix = query.data().split("|")[1]
                    val find = listFile("/tgbot/$suffix").find { it.name == "tgbot-1.0-SNAPSHOT.jar" } ?: error("未找到该目录下的文件")
                    val url = "https://pan.kuku.me/d/tgbot/$suffix/tgbot-1.0-SNAPSHOT.jar?sign=${find.sign}"
                    val str = client.get(url).bodyAsText()
                    val newUrl = Jsoup.parse(str).getElementsByTag("a").first()?.attr("href") ?: error("未获取到文件链接")
                    val iis = client.get(newUrl).body<InputStream>()
                    iis.transferTo(FileOutputStream("tmp${java.io.File.separator}tgbot-1.0-SNAPSHOT-new.jar"))
                    "kuku".toByteArray().inputStream().transferTo(FileOutputStream("update.pid"))
                    editMessageText("""
                        下载完成，更新中...
                        更新完成不会提示，一般10秒以内即可更新完成
                    """.trimIndent())
                }
            }
        }

    }

}


private suspend fun listFile(path: String): List<File> {
    val jsonNode = client.post("https://pan.kuku.me/api/fs/list") {
        setJsonBody("""{"path":"$path","password":"","page":1,"per_page":0,"refresh":false}""")
    }.body<JsonNode>()
    return jsonNode["data"]["content"].convertValue<List<File>>().stream().limit(5).toList()
}


private class File {
    var name: String = ""
    var size: Long = 0
    @JsonProperty("is_dir")
    var isDir: Boolean = false
    var modified: String = ""
    var created: String = ""
    var sign: String = ""
    var thumb: String = ""
    var type: Int = 0
    @JsonProperty("hashinfo")
    var hashInfo: String = ""

}