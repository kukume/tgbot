package me.kuku.telegram.logic

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import me.kuku.telegram.entity.CoreMailEntity
import me.kuku.telegram.entity.CoreMailService
import me.kuku.utils.*
import org.jsoup.Jsoup
import org.springframework.stereotype.Service
import java.security.MessageDigest

@Service
class CoreMailLogic(
    private val coreMailService: CoreMailService
) {

    suspend fun xt(entity: CoreMailEntity): CoreMailEntity.Type {
        val text = client.get(entity.url).bodyAsText()
        return if (text.contains("xt3")) CoreMailEntity.Type.XT3
        else if (text.contains("xt5")) CoreMailEntity.Type.XT5
        else error("未知的类型")
    }

    context(HeadersBuilder)
    private fun CoreMailEntity.appendHeaders() {
        append("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
        append("referer", this@CoreMailEntity.url)
        append("origin", this@CoreMailEntity.url)
        val ck = this@CoreMailEntity.cookie
        if (ck.isNotEmpty()) append("cookie", ck + "Coremail.sid=${this@CoreMailEntity.sid}; ")
    }

    suspend fun login(entity: CoreMailEntity): CoreMailEntity {
        when (entity.type) {
            CoreMailEntity.Type.XT5 -> loginByXt5(entity)
            CoreMailEntity.Type.XT3 -> loginByXt3(entity)
        }
        return entity
    }

    private fun sha(inStr: String): String {
        val sha = MessageDigest.getInstance("SHA")
        val byteArray = inStr.toByteArray()
        val md5Bytes = sha.digest(byteArray)
        return md5Bytes.base64Encode()
    }


    suspend fun loginByXt5(entity: CoreMailEntity): CoreMailEntity {
        val url = entity.url
        val text = client.get(url).bodyAsText()
        val sid = MyUtils.regex("sid=", "\"", text) ?: error("未获取到sid")
        val jsonNode = client.post("$url/coremail/s/json?sid=$sid&func=user%3AgetPasswordKey") {
            setFormDataContent {
            }
            headers {
                entity.appendHeaders()
            }
        }.bodyAsText().toJsonNode()
        // type -> RSA
        val key = jsonNode["var"]["key"]
        val e = key["e"].asText()
        val n = key["n"].asText()
        val encryptPassword = RSAUtils.encrypt(entity.password, RSAUtils.getPublicKey(n, e))
        val response = client.post("$url/coremail/index.jsp?cus=1&sid=$sid") {
            setFormDataContent {
                append("locale", "zh_CN")
                append("nodetect", "false")
                append("destURL", "")
                append("supportLoginDevice", "true")
                if (entity.token.isNotEmpty()) {
                    append("accessToken", entity.token)
                    val timestamp = System.currentTimeMillis() / 1000
                    append("timestamp", timestamp.toString())
                    append("signature", sha("accessToken=${entity.token.toUrlEncode()}&timestamp=$timestamp&secret=${entity.secret.toUrlEncode()}"))
                } else {
                    append("accessToken", "")
                    append("timestamp", "")
                    append("signature", "")
                }
                append("nonce", "")
                append("device", """{"uuid":"webmail_windows","imie":"webmail_windows","friendlyName":"chrome 114","model":"windows","os":"windows","osLanguage":"zh-CN","deviceType":"Webmail"}""")
                append("supportDynamicPwd", "true")
                append("supportBind2FA", "true")
                append("authorizeDevice", "")
                append("loginType", "")
                append("uid", entity.username)
                append("domain", entity.suffix)
                append("password", encryptPassword)
                append("face", "auto")
                append("faceName", "自动选择")
                append("action:login", "")
            }
            headers {
                entity.appendHeaders()
            }
        }
        val cookie = response.cookie()
        val html = response.bodyAsText()
        if (cookie.isEmpty()) {
            val errReason = Jsoup.parse(Jsoup.parse(html).getElementById("asideTpl")?.html() ?: "")
                .getElementById("warnOrErrDiv")?.text() ?: "账号或密码错误"
            error(errReason)
        } else {
            entity.cookie = cookie
            val getSid = MyUtils.regex("sid=", "\";", html)!!
            val secret = MyUtils.regex("secret = \"", "\";", html)!!
            val token = MyUtils.regex("token = \"", "\";", html)!!
            val tokenPreservePeriod = MyUtils.regex("tokenPreservePeriod = ", ";", html)!!.toLong() * 1000
            entity.sid = getSid
            if (secret.isNotEmpty()) entity.secret = secret
            if (token.isNotEmpty()) entity.token = token
            entity.tokenExpire = System.currentTimeMillis() + tokenPreservePeriod
            coreMailService.save(entity)
            return entity
        }
    }

    suspend fun loginByXt3(entity: CoreMailEntity): CoreMailEntity {
        val url = entity.url
        val response = client.post("$url/coremail/index.jsp") {
            setFormDataContent {
                append("locale", "zh_CN")
                append("uid", entity.username)
                append("nodetect", "false")
                append("domain", entity.suffix)
                append("password", entity.password)
                append("useSSL", url.contains("https").toString())
                append("action:login", "")
            }
            headers { entity.appendHeaders() }
        }
        val cookie = response.cookie()
        if (cookie.isEmpty()) {
            val html = response.bodyAsText()
            val errReason = Jsoup.parse(html).select(".Error").first()?.text() ?: "账号或密码错误"
            error(errReason)
        } else {
            entity.cookie = cookie
            val location = response.headers["location"]!!
            val sid = location.substring(location.lastIndexOf('=') + 1)
            entity.sid = sid
            coreMailService.save(entity)
            return entity
        }
    }

    fun JsonNode.check() {
        val code = this["code"].asText()
        if (code != "S_OK") error(code)
    }

    suspend fun changeAlias(entity: CoreMailEntity, prefix: String) {
        when (entity.type) {
            CoreMailEntity.Type.XT5 -> changeAliasByXt5(entity, prefix)
            CoreMailEntity.Type.XT3 -> changeAliasByXt3(entity, prefix)
        }
    }

    suspend fun changeAliasByXt5(entity: CoreMailEntity, prefix: String) {
        val jsonNode = client.post("${entity.url}/coremail/s/json?sid=${entity.sid}&func=user%3AsetAcountAttrs") {
            contentType(ContentType("text", "x-json"))
            setBody("""{"attrs":{"alias_attrs":"$prefix@${entity.suffix}"}}""")
            headers { entity.appendHeaders() }
        }.bodyAsText().toJsonNode()
        jsonNode.check()
    }

    suspend fun changeAliasByXt3(entity: CoreMailEntity, prefix: String) {
        val text = client.post("${entity.url}/coremail/XT3/pref/aliasRegister.jsp?sid=${entity.sid}") {
            setFormDataContent {
                append("action", "updateAlias")
                append("alias_id", prefix)
                append("domainName", entity.suffix)
            }
            headers{ entity.appendHeaders() }
        }.bodyAsText()
        // 恭喜您！您已经成功注册别
    }

    suspend fun alias(entity: CoreMailEntity): List<String> {
        return when (entity.type) {
            CoreMailEntity.Type.XT5 -> aliasByXt5(entity)
            CoreMailEntity.Type.XT3 -> aliasByXt3(entity)
        }
    }

    suspend fun aliasByXt5(entity: CoreMailEntity): List<String> {
        val text = client.get("${entity.url}/coremail/XT5/index.jsp?sid=${entity.sid}#setting.delivery.compose"){
            headers { entity.appendHeaders() }
        }.bodyAsText()
        return MyUtils.regexGroup("(?<='addr':').*?(?=',)", text)
    }

    suspend fun aliasByXt3(entity: CoreMailEntity): List<String> {
        return listOf()
    }

    suspend fun queryForward(entity: CoreMailEntity): CoreMailForward {
        return when (entity.type) {
            CoreMailEntity.Type.XT5 -> queryForwardByXt5(entity)
            CoreMailEntity.Type.XT3 -> queryForwardByXt3(entity)
        }
    }

    suspend fun queryForwardByXt5(entity: CoreMailEntity): CoreMailForward {
        val jsonNode = client.post("${entity.url}/coremail/s/json?sid=${entity.sid}&func=user%3AgetAttrs") {
            setBody("""
                {"optionalAttrIds":["forwardactive",
                "keeplocal",
                "forwarddes"]}
            """.trimIndent())
            contentType(ContentType("text", "x-json"))
            headers { entity.appendHeaders() }
        }.bodyAsText().toJsonNode()
        jsonNode.check()
        val coreMailForward = CoreMailForward()
        val va = jsonNode["var"]
        coreMailForward.active = va["forwardactive"].asInt() == 1
        coreMailForward.emails = va["forwarddes"].asText().split(",")
        coreMailForward.keepLocal = va["keeplocal"].asInt() == 1
        return coreMailForward
    }

    suspend fun queryForwardByXt3(entity: CoreMailEntity): CoreMailForward {
        error("暂不支持")
    }

    private suspend fun setAttr(entity: CoreMailEntity, json: String) {
        val jsonNode = client.post("${entity.url}/coremail/s/json?func=user%3AsetAttrs&sid=${entity.sid}") {
            setBody(json)
            contentType(ContentType("text", "x-json"))
            headers { entity.appendHeaders() }
        }.bodyAsText().toJsonNode()
        jsonNode.check()
    }

    suspend fun changeForwardEmail(entity: CoreMailEntity, emails: String) {
        return when (entity.type) {
            CoreMailEntity.Type.XT5 -> changeForwardEmailByXt5(entity, emails)
            CoreMailEntity.Type.XT3 -> changeForwardEmailByXt3(entity, emails)
        }
    }

    suspend fun changeForwardEmailByXt5(entity: CoreMailEntity, emails: String) {
        setAttr(entity, """{"attrs":{"forwarddes":"$emails"}}""")
    }

    suspend fun changeForwardEmailByXt3(entity: CoreMailEntity, emails: String) {
    }

    suspend fun openForward(entity: CoreMailEntity) {
        return when (entity.type) {
            CoreMailEntity.Type.XT5 -> openForwardByXt5(entity)
            CoreMailEntity.Type.XT3 -> openForwardByXt3(entity)
        }
    }

    suspend fun openForwardByXt5(entity: CoreMailEntity) {
        setAttr(entity, """{"attrs":{"forwardactive":1,"keeplocal":1}}""")
    }

    suspend fun openForwardByXt3(entity: CoreMailEntity) {
    }

    suspend fun closeForward(entity: CoreMailEntity) {
        return when (entity.type) {
            CoreMailEntity.Type.XT5 -> closeForwardByXt5(entity)
            CoreMailEntity.Type.XT3 -> closeForwardByXt3(entity)
        }
    }

    suspend fun closeForwardByXt5(entity: CoreMailEntity) {
        setAttr(entity, """{"attrs":{"keeplocal":0,"forwardactive":0}}""")
    }

    suspend fun closeForwardByXt3(entity: CoreMailEntity) {
    }



}


class CoreMailForward {
    var active: Boolean = false
    var emails: List<String> = listOf()
    var keepLocal: Boolean = false
}
