package me.kuku.telegram.logic

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import me.kuku.telegram.config.api
import me.kuku.telegram.entity.LinuxDoEntity
import me.kuku.telegram.entity.LinuxDoService
import me.kuku.telegram.utils.*
import org.jsoup.Jsoup
import org.springframework.stereotype.Service

@Service
class LinuxDoLogic(
    private val linuxDoService: LinuxDoService
) {

    companion object {

        suspend fun check(cookie: String) {
            val html = client.get("https://linux.do/") {
                cookieString(cookie)
            }.bodyAsText()
            if (html.contains("""<input type="submit" id="signin-button" value="登录">""")) error("未登陆，cookie不正确")
        }

        suspend fun latestTopic(): List<LinuxDoTopic> {
            val html = client.get("https://linux.do/latest").bodyAsText()
            val document = Jsoup.parse(html)
            val elements = document.getElementsByClass("topic-list-item")
            val list = mutableListOf<LinuxDoTopic>()
            for (element in elements) {
                val title = element.select(".title").text()
                val category = element.select(".category-name").text()
                val text = element.select(".excerpt").text()
                val url = element.select("a[itemprop=\"url\"]").attr("href")
                val suffix = url.replace("https://linux.do/t/topic/", "")
                list.add(LinuxDoTopic().also {
                    it.title = title
                    it.category = category
                    it.text = text
                    it.url = url
                    it.suffix = suffix
                })
                /**
                 * td class="replies"><span class="posts" title="帖子">819</span></td>
                 *  <td class="views"><span class="views" title="浏览量">31644</span></td>
                 *  <td>2024 年5 月 11 日</td>
                 */

            }
            return list
        }

        suspend fun login(username: String, password: String): String {
            val csrfResponse = client.get("https://linux.do/session/csrf") {
                headers {
                    append("X-Requested-With", "XMLHttpRequest")
                }
            }
            val jsonNode = csrfResponse.body<JsonNode>()
            val csrf = jsonNode["csrf"].asText()
            val tempCookie = csrfResponse.setCookie().renderCookieHeader()
            val response = client.submitForm("https://linux.do/session",
                parameters {
                    append("login", username)
                    append("password", password)
                    append("second_factor_method", "1")
                    append("timezone", "Asia/Shanghai")
                }) {
                headers {
                    append("X-CSRF-Token", csrf)
                    append("X-Requested-With", "XMLHttpRequest")
                    cookieString(tempCookie)
                    origin("https://linux.do")
                    referer("https://linux.do/")
                }
            }
            val loginNode = response.body<JsonNode>()
            if (loginNode.has("error")) error(loginNode["error"].asText())
            return response.setCookie().renderCookieHeader()
        }


    }

    suspend fun index(linuxDoEntity: LinuxDoEntity) {
        val jsonNode = client.get("$api/linuxdo/index") {
            cookieString(linuxDoEntity.cookie)
        }.body<JsonNode>()
        val newCookie = jsonNode["cookie"].asText()
        linuxDoEntity.cookie = newCookie
        linuxDoService.save(linuxDoEntity)
    }

    suspend fun topic(linuxDoEntity: LinuxDoEntity, id: String) {
        val jsonNode = client.get("$api/linuxdo/topic/$id") {
            cookieString(linuxDoEntity.cookie)
        }.body<JsonNode>()
        val newCookie = jsonNode["cookie"].asText()
        linuxDoEntity.cookie = newCookie
        linuxDoService.save(linuxDoEntity)
    }

}


class LinuxDoTopic {
    var title: String = ""
    var category: String = ""
    var text: String = ""
    var url: String = ""
    var suffix: String = ""
}