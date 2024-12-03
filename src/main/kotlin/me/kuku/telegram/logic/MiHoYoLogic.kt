package me.kuku.telegram.logic

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.delay
import me.kuku.telegram.entity.MiHoYoEntity
import me.kuku.telegram.exception.qrcodeNotScanned
import me.kuku.telegram.utils.*
import org.springframework.stereotype.Service
import java.util.*

@Service
class MiHoYoLogic(
    private val twoCaptchaLogic: TwoCaptchaLogic
) {

    context(HttpRequestBuilder)
    private fun MiHoYoFix.append() {
        val jsonNode = Jackson.readTree(Jackson.writeValueAsString(this@MiHoYoFix))
        headers {
            for (entry in jsonNode.fields()) {
                val key = entry.key
                val value = entry.value.asText()
                append(key, value)
            }
        }
    }

    context(HttpRequestBuilder)
    private fun MiHoYoEntity.cookieAppend() {
        cookieString(cookie)
    }


    context(HttpRequestBuilder)
    private fun MiHoYoFix.commonAppend(ds: MiHoYoDs) {
        val fix = this@MiHoYoFix
        headers {
            append("x-rpc-device_id", fix.device.replace("-", ""))
            append("x-rpc-client_type", ds.clientType)
            append("x-rpc-app_version", ds.appVersion)
            append("X-Rpc-Device_fp", fix.fp)
            append("DS", ds.ds)
        }
    }

    context(HttpRequestBuilder)
    private fun MiHoYoFix.appAppend() {
        val ds = ds()
        commonAppend(ds)
        headers {
            append("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) miHoYoBBS/${ds.appVersion}")
        }
    }

    context(HttpRequestBuilder)
    private fun MiHoYoFix.appNewAppend(data: Map<String, Any>? = null) {
        val ds = newDs(data)
        commonAppend(ds)
        headers {
            referer("https://app.mihoyo.com")
            append("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) miHoYoBBS/${ds.appVersion}")
        }
    }

    context(HttpRequestBuilder)
    private fun MiHoYoFix.webAppend() {
        val ds = webDs()
        commonAppend(ds)
        headers {
            referer("https://www.miyoushe.com/")
            append("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
        }
    }

    context(HttpRequestBuilder)
    private fun MiHoYoFix.hubAppend() {
        val ds = hubDs()
        commonAppend(ds)
        headers {
            referer("https://app.mihoyo.com")
        }
    }

    context(HttpRequestBuilder)
    private fun MiHoYoFix.hubNewAppend(data: Map<String, Any>? = null) {
        val ds = hubNewDs(data)
        commonAppend(ds)
        headers {
            referer("https://app.mihoyo.com")
        }
    }

    private fun JsonNode.check() {
        if (this["retcode"].asInt() != 0) error(this["message"].asText())
    }

    suspend fun qrcodeLogin1(): MiHoYoQrcode {
        val fix = MiHoYoFix()
        val jsonNode = client.post("https://passport-api.miyoushe.com/account/ma-cn-passport/web/createQRLogin") {
            fix.append()
        }.body<JsonNode>()
        jsonNode.check()
        val data = jsonNode["data"]
        return MiHoYoQrcode(fix, data["url"].asText(), data["ticket"].asText())
    }

    suspend fun qrcodeLogin2(qrcode: MiHoYoQrcode): MiHoYoEntity {
        val response = client.post("https://passport-api.miyoushe.com/account/ma-cn-passport/web/queryQRLoginStatus") {
            setJsonBody("""{"ticket":"${qrcode.ticket}"}""")
            qrcode.fix.append()
        }
        val jsonNode = response.body<JsonNode>()
        jsonNode.check()
        val data = jsonNode["data"]
        return when (val status = data["status"].asText()) {
            "Created", "Scanned" -> qrcodeNotScanned()
            "Confirmed" -> {
                var cookie = response.setCookie().renderCookieHeader()
                val loginResponse = client.post("https://bbs-api.miyoushe.com/user/wapi/login") {
                    setJsonBody("""{"gids":"2"}""")
                    qrcode.fix.append()
                    headers {
                        cookieString(cookie)
                    }
                }
                val loginJsonNode = loginResponse.body<JsonNode>()
                loginJsonNode.check()
                val setCookie = loginResponse.setCookie()
                cookie += setCookie.renderCookieHeader()
                val entity = MiHoYoEntity()
                entity.fix = qrcode.fix
                entity.cookie = cookie
                val userInfo = data["user_info"]
                entity.aid = userInfo["aid"].asText()
                entity.mid = userInfo["mid"].asText()
                val token = setCookie.find { it.name == "cookie_token_v2" }?.value ?: ""
                entity.token = token
                entity
            }
            else -> error("米游社登陆失败，未知的状态：$status")
        }
    }

    suspend fun login(account: String, password: String, tgId: Long? = null): MiHoYoEntity {
        val rsaKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDDvekdPMHN3AYhm/vktJT+YJr7cI5DcsNKqdsx5DZX0gDuWFuIjzdwButrIYPNmRJ1G8ybDIF7oDW2eEpm5sMbL9zs9ExXCdvqrn51qELbqj0XxtMTIpaCHFSI50PfPpTFV9Xt/hmyVwokoOXFlAEgCn+QCgGs52bFoYMtyi+xEQIDAQAB"
        val newAccount = account.rsaEncrypt(rsaKey)
        val newPassword = password.rsaEncrypt(rsaKey)
        val fix = MiHoYoFix()
        val headerMap = mutableMapOf(
            "x-rpc-app_id" to fix.app,
            "x-rpc-client_type" to "2",
            "x-rpc-device_id" to "5094e8dc3f0d4570",
            "x-rpc-device_fp" to "38d7f057d0dd6",
            "x-rpc-device_name" to "Redmi+K30S+Ultra",
            "x-rpc-device_model" to "M2007J3SC",
            "x-rpc-sys_version" to "12",
            "x-rpc-game_biz" to "bbs_cn",
            "x-rpc-app_version" to "2.63.1",
            "x-rpc-sdk_version" to "2.19.0",
            "x-rpc-lifecycle_id" to UUID.randomUUID().toString(),
            "x-rpc-account_version" to "2.19.0",
            "x-rpc-aigis" to "",
            "DS" to ds().ds,
            "user-agent" to "okhttp/4.9.3"
        )
        val response = client.post("https://passport-api.mihoyo.com/account/ma-cn-passport/app/loginByPassword") {
            setJsonBody("""{"account":"$newAccount","password":"$newPassword"}""")
            headers {
                headerMap.forEach { (t, u) -> append(t, u) }
            }
        }
        var jsonNode = response.body<JsonNode>()
        val code = jsonNode["retcode"].asInt()
        if (code == -3101) {
            val gisJsonNode = response.headers["X-Rpc-Aigis"]!!.toJsonNode()
            val sessionId = gisJsonNode["session_id"].asText()
            val data = gisJsonNode["data"].asText().toJsonNode()
            val gt = data["gt"].asText()
            val challenge = data["challenge"].asText()
            val rr = twoCaptchaLogic.geeTest(gt, challenge,"https://static.mohoyo.com", tgId = tgId)
            val aiGis = "$sessionId;" + """
                {"geetest_challenge":"${rr.challenge}","geetest_seccode":"${rr.validate}|jordan","geetest_validate":"${rr.validate}"}
            """.trimIndent().encodeBase64()
            headerMap["x-rpc-aigis"] = aiGis
            headerMap["DS"] = ds().ds
            jsonNode = client.post("https://passport-api.mihoyo.com/account/ma-cn-passport/app/loginByPassword") {
                setJsonBody("""{"account":"$newAccount","password":"$newPassword"}""")
                headers {
                    headerMap.forEach { (t, u) -> append(t, u) }
                }
            }.body<JsonNode>()
        }
        jsonNode.check()
        val data = jsonNode["data"]
        val ticket = data["login_ticket"].asText()
        val token = data["token"]["token"].asText()
        val user = data["user_info"]
        val aid = user["aid"].asText()
        val mid = user["mid"].asText()
        val entity = MiHoYoEntity().also {
            it.ticket = ticket
            it.token = token
            it.aid = aid
            it.mid = mid
            it.fix = fix
        }
        val sToken = sToken(entity)
        entity.sToken = sToken
        return entity
    }

    suspend fun webLogin(account: String, password: String, tgId: Long? = null): MiHoYoEntity {
        val fix = MiHoYoFix()
        val rsaKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDDvekdPMHN3AYhm/vktJT+YJr7cI5DcsNKqdsx5DZX0gDuWFuIjzdwButrIYPNmRJ1G8ybDIF7oDW2eEpm5sMbL9zs9ExXCdvqrn51qELbqj0XxtMTIpaCHFSI50PfPpTFV9Xt/hmyVwokoOXFlAEgCn+QCgGs52bFoYMtyi+xEQIDAQAB"
        var response = client.post("https://passport-api.miyoushe.com/account/ma-cn-passport/web/loginByPassword") {
            setJsonBody("""{"account":"${account.rsaEncrypt(rsaKey)}","password":"${password.rsaEncrypt(rsaKey)}"}""")
            fix.loginHeader()
        }
        var cookie = response.setCookie().renderCookieHeader()
        var loginJsonNode = response.body<JsonNode>()
        val code = loginJsonNode["retcode"].asInt()
        if (code == -3101) {
            val captchaJsonNode = response.headers["X-Rpc-Aigis"]!!.toJsonNode()
            val sessionId = captchaJsonNode["session_id"].asText()
            val captchaDataJsonNode = captchaJsonNode["data"].asText().toJsonNode()
            val captchaId = captchaDataJsonNode["gt"].asText()
            val riskType = captchaDataJsonNode["risk_type"].asText()
            twoCaptchaLogic.geeTestV4(captchaId, "https://user.mihoyo.com/",
                mapOf("captcha_id" to captchaId, "session_id" to sessionId, "risk_type" to riskType), tgId)
            response = client.post("https://passport-api.miyoushe.com/account/ma-cn-passport/web/loginByPassword") {
                setJsonBody("""{"account":"${account.rsaEncrypt(rsaKey)}","password":"${password.rsaEncrypt(rsaKey)}"}""")
                fix.loginHeader()
            }
            cookie = response.setCookie().renderCookieHeader()
            loginJsonNode = response.body<JsonNode>()
        }
        if (loginJsonNode["retcode"].asInt() != 0) error(loginJsonNode["message"].asText())
        val infoDataJsonNode = loginJsonNode["data"]["user_info"]
        val aid = infoDataJsonNode["aid"].asText()
        val mid = infoDataJsonNode["mid"].asText()
        val loginResponse = client.post("https://bbs-api.mihoyo.com/user/wapi/login") {
            setJsonBody("{\"gids\":\"2\"}")
            cookieString(cookie)
        }
        val finaCookie = loginResponse.body<JsonNode>()
        cookie += finaCookie
        val entity = MiHoYoEntity().also {
            it.cookie = cookie
            it.aid = aid
            it.mid = mid
            it.token = ""
        }
        return entity
    }

    context(HttpRequestBuilder)
    private fun MiHoYoFix.loginHeader() {
        val fix = this@MiHoYoFix
        headers {
            referer("https://user.mihoyo.com/")
            origin("https://user.mihoyo.com")
            append("x-rpc-app_id", fix.app)
            append("X-Rpc-Client_type", "4")
            append("X-Rpc-Device_fp", fix.fp)
            append("X-Rpc-Device_id", fix.device)
            append("X-Rpc-Device_model", "Windows%2010%2064-bit")
            append("X-Rpc-Device_name", "Chrome%20119.0.0.0")
            append("X-Rpc-Game_biz", "account_cn")
            append("X-Rpc-Mi_referrer", "https://user.mihoyo.com/")
            append("X-Rpc-Source", "accountWebsite")
            append("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
            append("cookie", "_MHYUUID=${fix.device}; DEVICEFP_SEED_ID=${RandomUtils.num(16)}; DEVICEFP_SEED_TIME=${System.currentTimeMillis()}; DEVICEFP=${fix.fp};")
        }
    }

    private fun webDs(): MiHoYoDs {
        val salt = "mx1x4xCahVMVUDJIkJ9H3jsHcUsiASGZ"
        val time = System.currentTimeMillis() / 1000
        val randomLetter = RandomUtils.letter(6)
        val md5 = "salt=$salt&t=$time&r=$randomLetter".md5()
        val ds = "$time,$randomLetter,$md5"
        return MiHoYoDs("2.64.0", "4", ds)
    }

    private fun hubDs(): MiHoYoDs {
        val salt = "AcpNVhfh0oedCobdCyFV8EE1jMOVDy9q"
        val time = System.currentTimeMillis() / 1000
        val randomLetter = RandomUtils.letter(6)
        val md5 = "salt=$salt&t=$time&r=$randomLetter".md5()
        val ds = "$time,$randomLetter,$md5"
        return MiHoYoDs("2.60.1", "2", ds)
    }

    private fun hubNewDs(data: Map<String, Any>? = null, params: Map<String, Any>? = null): MiHoYoDs {
        val salt = "t0qEgfub6cvueAPgR5m9aQWWVciEer7v"
        val t = System.currentTimeMillis() / 1000
        val r = (100001..200000).random().toString()
        val q = params?.let { urlParams -> urlEncode(urlParams) } ?: ""
        val b = data?.let { Jackson.writeValueAsString(it) } ?: "{}"
        val c = "salt=$salt&t=$t&r=$r&b=$b&q=$q".md5()
        val ds = "$t,$r,$c"
        return MiHoYoDs("2.60.1", "2", ds)
    }

    private fun ds(): MiHoYoDs {
        val salt = "JwYDpKvLj6MrMqqYU6jTKF17KNO2PXoS"
        val time = System.currentTimeMillis() / 1000
        val randomLetter = RandomUtils.letter(6)
        val md5 = "salt=$salt&t=$time&r=$randomLetter".md5()
        val ds = "$time,$randomLetter,$md5"
        return MiHoYoDs("2.63.1", "5", ds)
    }

    private fun newDs(data: Map<String, Any>? = null, params: Map<String, Any>? = null): MiHoYoDs {
        val salt = "JwYDpKvLj6MrMqqYU6jTKF17KNO2PXoS"
        val t = System.currentTimeMillis() / 1000
        val r = (100001..200000).random().toString()
        val b = data?.let { Jackson.writeValueAsString(it) } ?: "{}"
        val q = params?.let { urlParams -> urlEncode(urlParams) } ?: ""
        val c = "salt=$salt&t=$t&r=$r&b=$b&q=$q".md5()
        val ds = "$t,$r,$c"
        return MiHoYoDs("2.63.1", "2", ds)
    }

    private fun urlEncode(params: Map<String, Any>): String {
        return params.map { (key, value) -> "$key=$value" }.joinToString("&")
    }

    private suspend fun sign(miHoYoEntity: MiHoYoEntity, jsonNode: JsonNode, geeTest: GeeTest? = null): JsonNode {
        return client.post("https://api-takumi.mihoyo.com/event/luna/sign") {
            setJsonBody("""
                    {"act_id":"e202311201442471","region":"${jsonNode["region"].asText()}","uid":"${jsonNode["game_uid"].asText()}","lang":"zh-cn"}
                """.trimIndent())
            miHoYoEntity.fix.appAppend()
            headers {
                cookieString(miHoYoEntity.cookie)
                append("x-rpc-signgame", "hk4e")
                geeTest?.let {
                    append("x-rpc-validate", it.validate)
                    append("x-rpc-seccode", it.secCode)
                    append("x-rpc-challenge", it.challenge)
                }
            }
        }.body<JsonNode>()
    }

    suspend fun sign(miHoYoEntity: MiHoYoEntity, tgId: Long? = null) {
        val ssJsonNode = client.get("https://api-takumi.mihoyo.com/binding/api/getUserGameRolesByCookie?game_biz=hk4e_cn") {
            cookieString(miHoYoEntity.cookie)
        }.body<JsonNode>()
        if (ssJsonNode["retcode"].asInt() != 0) error(ssJsonNode["message"].asText())
        val jsonArray = ssJsonNode["data"]["list"]
        if (jsonArray.size() == 0) error("您还没有原神角色！！")
        for (obj in jsonArray) {
            val jsonNode = sign(miHoYoEntity, obj)
            when (jsonNode["retcode"].asInt()) {
                0, -5003 -> {
                    val data = jsonNode["data"]
                    val gt = data["gt"]?.asText() ?: ""
                    if (gt.isNotEmpty()) {
                        val challenge = data["challenge"].asText()
                        val rr = twoCaptchaLogic.geeTest(gt, challenge, "https://webstatic.mihoyo.com/", tgId = tgId)
                        val node = sign(miHoYoEntity, obj, rr)
                        if (node["retcode"].asInt() !in listOf(-5003, 0)) error(jsonNode["message"].asText())
                    }
                }
                else -> error(jsonNode["message"].asText() ?: "未知错误")
            }
        }

    }

    suspend fun post(): List<MiHoYoPost> {
        val jsonNode = client.get("https://bbs-api.miyoushe.com/post/wapi/getForumPostList?forum_id=26&gids=2&is_good=false&is_hot=false&page_size=20&sort_type=2")
            .body<JsonNode>()
        jsonNode.check()
        return jsonNode["data"]["list"].convertValue()
    }

    suspend fun like(miHoYoEntity: MiHoYoEntity, postId: Int, like: Boolean = true) {
        val response = client.post("https://bbs-api.mihoyo.com/apihub/sapi/upvotePost") {
            setJsonBody("""
                {"csm_source":"home","is_cancel":${!like},"post_id":"$postId","upvote_type":"1"}
            """.trimIndent())
            cookieString(miHoYoEntity.hubCookie())
            miHoYoEntity.fix.hubAppend()
        }
        val jsonNode = response.body<JsonNode>()
        jsonNode.check()
    }

    suspend fun watchPost(miHoYoEntity: MiHoYoEntity, postId: Int) {
        val jsonNode = client.get("https://bbs-api.mihoyo.com/post/api/getPostFull?post_id=$postId&is_cancel=false") {
            miHoYoEntity.fix.hubAppend()
            cookieString(miHoYoEntity.hubCookie())
        }.body<JsonNode>()
        jsonNode.check()
    }

    suspend fun sharePost(miHoYoEntity: MiHoYoEntity, postId: Int) {
        val jsonNode = client.get("https://bbs-api.mihoyo.com/apihub/api/getShareConf?entity_id=$postId&entity_type=1") {
            miHoYoEntity.fix.hubAppend()
            cookieString(miHoYoEntity.hubCookie())
        }.body<JsonNode>()
        jsonNode.check()
    }

    private suspend fun hubVerifyGeeTest(miHoYoEntity: MiHoYoEntity) {
        val jsonNode = client.get("https://bbs-api.miyoushe.com/misc/api/createVerification?is_high=true") {
            miHoYoEntity.fix.hubNewAppend()
            cookieString(miHoYoEntity.hubCookie())
        }.body<JsonNode>()
        jsonNode.check()
        val data = jsonNode["data"]
        val challenge = data["challenge"].asText()
        val gt = data["gt"].asText()
        val rr = twoCaptchaLogic.geeTest(gt, challenge, "https://bbs.mihoyo.com", tgId = miHoYoEntity.tgId)
        val verifyJsonNode = client.post("https://bbs-api.miyoushe.com/misc/api/verifyVerification") {
            miHoYoEntity.fix.hubNewAppend()
            cookieString(miHoYoEntity.hubCookie())
            setJsonBody("""
                {"geetest_challenge":"${rr.challenge}","geetest_seccode":"${rr.validate}|jordan","geetest_validate":"${rr.validate}"}
            """.trimIndent())
        }.body<JsonNode>()
        verifyJsonNode.check()
    }

    // 2 5 8 6 1 3 4
    suspend fun hubSign(miHoYoEntity: MiHoYoEntity) {
        val list = listOf(2, 5, 8, 6, 1, 3, 4)
        for (i in list) {
            delay(1500)
            var jsonNode = client.post("https://bbs-api.miyoushe.com/apihub/app/api/signIn") {
                setJsonBody("""{"gids":"$i"}""")
                miHoYoEntity.fix.hubNewAppend(mapOf("gids" to "$i"))
                cookieString(miHoYoEntity.hubCookie())
            }.body<JsonNode>()
            val code = jsonNode["retcode"].asInt()
            if (code == 1034) hubVerifyGeeTest(miHoYoEntity)
            if (code in listOf(1008, 0)) continue
            jsonNode = client.post("https://bbs-api.miyoushe.com/apihub/app/api/signIn") {
                setJsonBody("""{"gids":"$i"}""")
                miHoYoEntity.fix.hubNewAppend(mapOf("gids" to "$i"))
                cookieString(miHoYoEntity.hubCookie())
            }.body<JsonNode>()
            jsonNode.check()
        }
    }

    suspend fun sToken(miHoYoEntity: MiHoYoEntity): String {
        val ticket = miHoYoEntity.ticket.ifEmpty { error("请重新使用账号或密码登陆") }
        val accountId = miHoYoEntity.aid.ifEmpty { error("请重新使用账号或密码登陆") }
        val jsonNode = client.get("https://api-takumi.mihoyo.com/auth/api/getMultiTokenByLoginTicket?login_ticket=$ticket&token_types=3&uid=$accountId")
            .body<JsonNode>()
        jsonNode.check()
        return jsonNode["data"]["list"][0]["token"].asText()
    }

    suspend fun mysSign(miHoYoEntity: MiHoYoEntity) {
        val post = post()
        for (i in 0 until 3) {
            watchPost(miHoYoEntity, post[i].post.postId)
        }
        for (i in 0 until 5) {
            like(miHoYoEntity, post[i].post.postId)
        }
        sharePost(miHoYoEntity, post[0].post.postId)
        hubSign(miHoYoEntity)
    }

}

class MiHoYoFix {
    var referer: String = "https://user.miyoushe.com/"
    @JsonProperty("X-Rpc-Device_fp")
    var fp: String = RandomUtils.letter(13)
    @JsonProperty("X-Rpc-Device_id")
    var device: String = UUID.randomUUID().toString()
    @JsonProperty("User-Agent")
    var userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36"
    @JsonProperty("X-Rpc-App_id")
    var app: String = "bll8iq97cem8"
}

data class MiHoYoQrcode(val fix: MiHoYoFix, val url: String, val ticket: String)

data class MiHoYoDs(val appVersion: String, val clientType: String, val ds: String)

class MiHoYoPost {
    var post: Post = Post()

    class Post {
        @JsonProperty("post_id")
        var postId: Int = 0
        var subject: String = ""
    }
}