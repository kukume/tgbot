package me.kuku.telegram.logic

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.kuku.telegram.utils.*
import org.jsoup.Jsoup
import java.lang.Exception
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

object HostLocLogic {

    private val mutex = Mutex()

    private lateinit var globalCookie: MutableList<Cookie>

    private val cookieKeyList = listOf("hkCM_2132_lastact", "hkCM_2132_connect_is_bind", "hkCM_2132_sid")

    private val client by lazy {
        val ktorClient = HttpClient(OkHttp) {
            engine {
                config {
                    followRedirects(false)
                }

                addInterceptor { chain ->
                    synchronized(this) {
                        TimeUnit.SECONDS.sleep(2)
                        val request = chain.request()
                        request.header("ignore")?.let {
                            return@addInterceptor chain.proceed(request)
                        }
                        val queryCookie = request.header("cookie")?.let {
                            var newCookie = it
                            for (cookie in globalCookie) {
                                val split1 = newCookie.split("; ")
                                val find = split1.find { s -> s.startsWith("${cookie.name}=") }
                                if (find != null) {
                                    val split2 = find.split("=")
                                    newCookie = newCookie.replace("${cookie.name}=${split2[1]}", "${cookie.name}=${cookie.value}")
                                } else {
                                    newCookie += "${cookie.name}=${cookie.value}; "
                                }
                            }
                            newCookie
                        } ?: globalCookie.renderCookieHeader()
                        val response = chain.proceed(request.newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                            .header("Accept", "*/*")
                            .removeHeader("cookie")
                            .header("cookie", queryCookie)
                            .build())
                        val returnResponse = if (response.code != 200) {
                            val html = response.body?.string() ?: return@addInterceptor response
                            val cookie = prepareCookie(html)
                            if (cookie.isNotEmpty()) {
                                val headerCookie = request.headers["cookie"] ?: ""
                                val newCookie = "$headerCookie$cookie"
                                val newRequest = request.newBuilder()
                                    .header("cookie", newCookie)
                                    .build()
                                TimeUnit.SECONDS.sleep(2)
                                chain.proceed(newRequest)
                            } else response
                        } else response
                        val headers = returnResponse.headers("Set-Cookie")
                        for (header in headers) {
                            val split = header.split("; ")[0].split("=")
                            val key = split[0]
                            if (key !in cookieKeyList) continue
                            val value = split[1]
                            globalCookie = globalCookie.filter { it.name != key }.toMutableList()
                            globalCookie.add(Cookie(key, value))
                        }
                        response
                    }
                }
            }

            followRedirects = false

            install(ContentNegotiation) {
                jackson()
            }


            install(Logging)

        }
        runBlocking {
            val response = ktorClient.get("https://hostloc.com/forum.php") {
                headers {
                    append("ignore", "true")
                }
            }
            globalCookie = response.setCookie().filter { it.name in cookieKeyList }.toMutableList()
        }
        ktorClient
    }

    suspend fun login(username: String, password: String): String {
        val response = client.submitForm("https://hostloc.com/member.php?mod=logging&action=login&loginsubmit=yes&infloat=yes&lssubmit=yes&inajax=1", parameters {
            append("fastloginfield", "username")
            append("username", username)
            append("cookietime", "2592000")
            append("password", password)
            append("quickforward", "yes")
            append("handlekey", "ls")
        }) {
            referer("https://hostloc.com/forum.php")
        }
        val str = response.bodyAsText()
        return if (str.contains("https://hostloc.com/forum.php"))
            response.setCookie().renderCookieHeader()
        else error("账号或密码错误或其他原因登录失败！")
    }

    private suspend fun checkLogin(cookie: String) {
        val html = client.get("https://hostloc.com/home.php?mod=spacecp") {
            cookieString(cookie)
        }.bodyAsText()
        delay(1000)
        val text = Jsoup.parse(html).getElementsByTag("title").first()!!.text()
        val b = text.contains("个人资料")
        if (!b) error("cookie已失效")
    }


    suspend fun singleSign(cookie: String) {
        mutex.withLock {
            checkLogin(cookie)
            val url = "https://hostloc.com/space-uid-${Random.nextInt(10000, 50000)}.html"
            kotlin.runCatching {
                client.get(url) {
                    cookieString(cookie)
                    referer("https://hostloc.com/forum.php")
                }
                delay(1000)
            }
        }
    }

    suspend fun sign(cookie: String) {
        mutex.withLock {
            checkLogin(cookie)
            val urlList = mutableListOf<String>()
            for (i in 0..12) {
                val num = Random.nextInt(10000, 50000)
                urlList.add("https://hostloc.com/space-uid-$num.html")
            }
            for (url in urlList) {
                delay(5000)
                kotlin.runCatching {
                    client.get(url) {
                        cookieString(cookie)
                        referer("https://hostloc.com/forum.php")
                    }
                }
            }
        }
    }

    suspend fun post(): List<HostLocPost> {
        val list = mutableListOf<HostLocPost>()
        val html = kotlin.runCatching {
            client.get("https://hostloc.com/forum.php?mod=forumdisplay&fid=45&filter=author&orderby=dateline") {
                referer("https://hostloc.com/forum.php")
            }.bodyAsText()
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
            val id = RegexUtils.extract(url, "tid=", "&")?.toInt() ?: 0
            val hostLocPost = HostLocPost(id, name, time, title, url)
            list.add(hostLocPost)
        }
        return list
    }

    suspend fun postContent(url: String, cookie: String = ""): String {
        val str = client.get(url) {
            cookieString(cookie)
        }.bodyAsText()
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

    private fun prepareCookie(html: String): String {
        val group = "(?<=toNumbers\\(\").*?(?=\"\\))".toRegex().findAll(html).map { it.value }.toList()
        if (group.isEmpty()) return ""
        val a = intArrToByteArr(toNumbers(group[0]))
        val b = intArrToByteArr(toNumbers(group[1]))
        val c = intArrToByteArr(toNumbers(group[2]))

        val decryptedValue = decrypt(a, b, c)!!
        val cookieValue = decryptedValue.hex()

        return "cnsL7=$cookieValue; "
    }

    private fun decrypt(aseKey: ByteArray?, iv: ByteArray, data: ByteArray): ByteArray? {
        return try {
            val cipher = Cipher.getInstance("AES/CBC/NoPadding")
            val secretKey: SecretKey = SecretKeySpec(aseKey, "AES")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            cipher.doFinal(data)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}


data class HostLocPost(
    val id: Int,
    val name: String,
    val time: String,
    val title: String,
    val url: String
)
