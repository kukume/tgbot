package me.kuku.telegram.logic

import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import me.kuku.pojo.UA
import me.kuku.utils.*
import org.jsoup.Jsoup

object HostLocLogic {

    suspend fun login(username: String, password: String): String {
        val prepareCookie = prepareCookie()
        val response = client.submitForm("https://hostloc.com/member.php?mod=logging&action=login&loginsubmit=yes&infloat=yes&lssubmit=yes&inajax=1", parameters {
            append("fastloginfield", "username")
            append("username", username)
            append("cookietime", "2592000")
            append("password", password)
            append("quickforward", "yes")
            append("handlekey", "ls")
        }) {
            cookieString(prepareCookie)
            referer("https://hostloc.com/forum.php")
        }
        val str = response.bodyAsText()
        return if (str.contains("https://hostloc.com/forum.php"))
            response.cookie()
        else error("账号或密码错误或其他原因登录失败！")
    }

    private suspend fun checkLogin(cookie: String) {
        val html = client.get("https://hostloc.com/home.php?mod=spacecp") {
            cookieString(cookie)
        }.bodyAsText()
        val text = Jsoup.parse(html).getElementsByTag("title").first()!!.text()
        val b = text.contains("个人资料")
        if (!b) error("cookie已失效")
    }

    suspend fun singleSign(cookie: String) {
        val prepareCookie = prepareCookie()
        val newCookie = prepareCookie + cookie
        checkLogin(newCookie)
        val url = "https://hostloc.com/space-uid-${MyUtils.randomInt(10000, 50000)}.html"
        kotlin.runCatching {
            OkHttpKtUtils.get(url, OkUtils.headers(newCookie, "https://hostloc.com/forum.php", UA.PC))
                .close()
        }
    }

    suspend fun sign(cookie: String) {
        val prepareCookie = prepareCookie()
        val newCookie = prepareCookie + cookie
        checkLogin(newCookie)
        val urlList = mutableListOf<String>()
        for (i in 0..12) {
            val num = MyUtils.randomInt(10000, 50000)
            urlList.add("https://hostloc.com/space-uid-$num.html")
        }
        for (url in urlList) {
            delay(5000)
            kotlin.runCatching {
                OkHttpKtUtils.get(url, OkUtils.headers(newCookie, "https://hostloc.com/forum.php", UA.PC))
                    .close()
            }
        }
    }

    suspend fun post(): List<HostLocPost> {
        val list = mutableListOf<HostLocPost>()
        val html = kotlin.runCatching {
            OkHttpKtUtils.getStr("https://hostloc.com/forum.php?mod=forumdisplay&fid=45&filter=author&orderby=dateline",
                OkUtils.headers("", "https://hostloc.com/forum.php", UA.PC))
        }.onFailure {
            return list
        }
        val elements = Jsoup.parse(html.getOrThrow()).getElementsByTag("tbody")
        for (ele in elements) {
            if (!ele.attr("id").startsWith("normalth")) continue
            val s = ele.getElementsByClass("s").first()!!
            val title = s.text()
            val url = "https://hostloc.com/${s.attr("href")}"
            val time = ele.select("em a span").first()?.text() ?: ""
            val name = ele.select("cite a").first()?.text() ?: ""
            val id = MyUtils.regex("tid=", "&", url)?.toInt() ?: 0
            val hostLocPost = HostLocPost(id, name, time, title, url)
            list.add(hostLocPost)
        }
        return list
    }

    suspend fun postContent(url: String, cookie: String = ""): String {
        val str = OkHttpKtUtils.getStr(url, OkUtils.headers(cookie, "", UA.PC))
        val pct = Jsoup.parse(str).getElementsByClass("pct")
        return pct.first()?.text() ?: "未获取到内容，需要权限查看"
    }

    private fun toNumbers(secret: String): IntArray {
        val length = secret.length
        val arr = IntArray(length / 2)
        var num = 0
        var i = 0
        while (i < length) {
            var lastNum = i + 2
            if (lastNum > length) {
                lastNum = i + 1
            }
            val ss = secret.substring(i, lastNum)
            if (num < arr.size) arr[num++] = ss.toInt(16)
            i += 2
        }
        return arr
    }

    private fun intArrToByteArr(intArr: IntArray): ByteArray {
        val bytes = ByteArray(intArr.size)
        for (i in intArr.indices) {
            val num = intArr[i]
            bytes[i] = num.toByte()
        }
        return bytes
    }

    private suspend fun prepareCookie(): String {
        val html = client.get("https://hostloc.com/forum.php").bodyAsText()
        val group = MyUtils.regexGroup("(?<=toNumbers\\(\").*?(?=\"\\))", html)
        if (group.isEmpty()) return ""
        val a = intArrToByteArr(toNumbers(group[0]))
        val b = intArrToByteArr(toNumbers(group[1]))
        val c = intArrToByteArr(toNumbers(group[2]))

        val decryptedValue = AESUtils.decryptLoc(a, b, c)!!
        val cookieValue = HexUtils.byteArrayToHex(decryptedValue)

        return "cnsL7=$cookieValue; "
    }

}


data class HostLocPost(
    val id: Int,
    val name: String,
    val time: String,
    val title: String,
    val url: String
)
