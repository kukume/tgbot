package me.kuku.telegram.logic

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.kuku.telegram.utils.*
import org.jsoup.Jsoup
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path

@Service
class ToolLogic {

    suspend fun positiveEnergy(date: String): File {
        val html = client.get("http://tv.cctv.com/lm/xwlb/day/$date.shtml").bodyAsText()
        val url =
            Jsoup.parse(html).getElementsByTag("li").first()?.getElementsByTag("a")?.last()?.attr("href") ?: error("未找到新闻联播链接")
        val nextHtml = client.get(url).bodyAsText()
        val guid = RegexUtils.extract("guid = \"", "\";", nextHtml) ?: error("没有找到guid")
        val tsp = System.currentTimeMillis().toString().substring(0, 10)
        val vc = "${tsp}204947899B86370B879139C08EA3B5E88267BF11E55294143CAE692F250517A4C10C".md5().uppercase()
        val jsonNode =
            client.get("https://vdn.apps.cntv.cn/api/getHttpVideoInfo.do?pid=$guid&client=flash&im=0&tsp=$tsp&vn=2049&vc=$vc&uid=BF11E55294143CAE692F250517A4C10C&wlan=")
                .bodyAsText().toJsonNode()
        val urlList = jsonNode["video"]["chapters4"].map { it["url"].asText() }
        val list = mutableListOf<File>()
        for (i in urlList.indices) {
            client.get(urlList[i]).bodyAsChannel().toInputStream().use { iis ->
                val path = Path.of("tmp", "$date-$i.mp4")
                Files.copy(iis, path)
                list.add(path.toFile())
            }
        }
        val sb = StringBuilder()
        for (file in list) {
            sb.appendLine("file ${file.absolutePath.replace("\\", "/")}")
        }
        sb.removeSuffix("\n")
        val txtFile = File("$date.txt")
        val txtFos = withContext(Dispatchers.IO) {
            FileOutputStream(txtFile)
        }
        sb.toString().byteInputStream().copyTo(txtFos)
        val newPath = list[0].absolutePath.replace("$date-0.mp4", "$date-output.mp4")
        ffmpeg("ffmpeg -f concat -safe 0 -i $date.txt -c copy $newPath")
        list.forEach { it.delete() }
        txtFile.delete()
        return File(newPath)
    }

}

data class SaucenaoResult(
    val similarity: String, val thumbnail: String, val indexName: String, val extUrls: List<String>, val title: String, val author: String, val authUrl: String,
    var daId: Long? = null, var pixivId: Long? = null, var faId: Long? = null, var tweetId: Long? = null
)
