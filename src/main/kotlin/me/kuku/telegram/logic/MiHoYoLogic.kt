package me.kuku.telegram.logic

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import me.kuku.pojo.CommonResult
import me.kuku.pojo.UA
import me.kuku.telegram.entity.MiHoYoEntity
import me.kuku.utils.*
import org.springframework.stereotype.Service
import java.util.*

@Service
class MiHoYoLogic(
    private val geeTestLogic: GeeTestLogic
) {

    context(HttpRequestBuilder)
    private fun MiHoYoFix.append() {
        val jsonNode = Jackson.parse(Jackson.toJsonString(this@MiHoYoFix))
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

    suspend fun qrcodeLogin2(qrcode: MiHoYoQrcode): CommonResult<MiHoYoEntity> {
        val response = client.post("https://passport-api.miyoushe.com/account/ma-cn-passport/web/queryQRLoginStatus") {
            setJsonBody("""{"ticket":"${qrcode.ticket}"}""")
            qrcode.fix.append()
        }
        val jsonNode = response.body<JsonNode>()
        jsonNode.check()
        val data = jsonNode["data"]
        return when (val status = data["status"].asText()) {
            "Created", "Scanned" -> CommonResult.fail("未扫码", null, 0)
            "Confirmed" -> {
                var cookie = response.cookie()
                val loginResponse = client.post("https://bbs-api.miyoushe.com/user/wapi/login") {
                    setJsonBody("""{"gids":"2"}""")
                    qrcode.fix.append()
                    headers {
                        cookieString(cookie)
                    }
                }
                val loginJsonNode = loginResponse.body<JsonNode>()
                loginJsonNode.check()
                cookie += loginResponse.cookie()
                val entity = MiHoYoEntity()
                entity.fix = qrcode.fix
                entity.cookie = cookie
                val userInfo = data["user_info"]
                entity.aid = userInfo["aid"].asText()
                entity.mid = userInfo["mid"].asText()
                CommonResult.success(entity)
            }
            else -> error("米游社登陆失败，未知的状态：$status")
        }
    }

    suspend fun login(account: String, password: String): MiHoYoEntity {
        val rsaKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDDvekdPMHN3AYhm/vktJT+YJr7cI5DcsNKqdsx5DZX0gDuWFuIjzdwButrIYPNmRJ1G8ybDIF7oDW2eEpm5sMbL9zs9ExXCdvqrn51qELbqj0XxtMTIpaCHFSI50PfPpTFV9Xt/hmyVwokoOXFlAEgCn+QCgGs52bFoYMtyi+xEQIDAQAB"
        val newAccount = account.rsaEncrypt(rsaKey)
        val newPassword = password.rsaEncrypt(rsaKey)
        val fix = MiHoYoFix()
        val jsonNode = client.post("https://passport-api.mihoyo.com/account/ma-cn-passport/app/loginByPassword") {
            setJsonBody("""{"account":"$newAccount","password":"$newPassword"}""")
            headers {
                append("x-rpc-app_id", fix.app)
                append("x-rpc-client_type", "2")
                append("x-rpc-device_id", "5094e8dc3f0d4570")
                append("x-rpc-device_fp", "38d7f057d0dd6")
                append("x-rpc-device_name", "Redmi+K30S+Ultra")
                append("x-rpc-device_model", "M2007J3SC")
                append("x-rpc-sys_version", "12")
                append("x-rpc-game_biz", "bbs_cn")
                append("x-rpc-app_version", "2.63.1")
                append("x-rpc-sdk_version", "2.19.0")
                append("x-rpc-lifecycle_id", UUID.randomUUID().toString())
                append("x-rpc-account_version", "2.19.0")
                append("x-rpc-aigis", "")
                append("DS", newDs().ds)
                append("user-agent", "okhttp/4.9.3")
            }
        }.body<JsonNode>()
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
        val beforeJsonNode = OkHttpKtUtils.getJson("https://webapi.account.mihoyo.com/Api/create_mmt?scene_type=1&now=${System.currentTimeMillis()}&reason=bbs.mihoyo.com")
        val dataJsonNode = beforeJsonNode["data"]["mmt_data"]
        val challenge = dataJsonNode.getString("challenge")
        val gt = dataJsonNode.getString("gt")
        val mmtKey = dataJsonNode.getString("mmt_key")
        val rr = geeTestLogic.rr(gt, "https://bbs.mihoyo.com/ys/", challenge, tgId = tgId)
        val cha = rr.challenge
        val validate = rr.validate
        val rsaKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDDvekdPMHN3AYhm/vktJT+YJr7cI5DcsNKqdsx5DZX0gDuWFuIjzdwButrIYPNmRJ1G8ybDIF7oDW2eEpm5sMbL9zs9ExXCdvqrn51qELbqj0XxtMTIpaCHFSI50PfPpTFV9Xt/hmyVwokoOXFlAEgCn+QCgGs52bFoYMtyi+xEQIDAQAB"
        val enPassword = password.rsaEncrypt(rsaKey)
        val map = mapOf("is_bh2" to "false", "account" to account, "password" to enPassword,
            "mmt_key" to mmtKey, "is_crypto" to "true", "geetest_challenge" to cha, "geetest_validate" to validate,
            "geetest_seccode" to "${validate}|jordan")
        val response = OkHttpKtUtils.post("https://webapi.account.mihoyo.com/Api/login_by_password", map, OkUtils.ua(UA.PC))
        val loginJsonNode = OkUtils.json(response)
        val infoDataJsonNode = loginJsonNode["data"]
        if (infoDataJsonNode.getInteger("status") != 1) error(infoDataJsonNode.getString("msg"))
        var cookie = OkUtils.cookie(response)
        val infoJsonNode = infoDataJsonNode["account_info"]
        val accountId = infoJsonNode.getString("account_id")
        val ticket = infoJsonNode.getString("weblogin_token")
        val cookieJsonNode = OkHttpKtUtils.getJson("https://webapi.account.mihoyo.com/Api/cookie_accountinfo_by_loginticket?login_ticket=$ticket&t=${System.currentTimeMillis()}",
            OkUtils.headers(cookie, "", UA.PC))
        val cookieToken = cookieJsonNode["data"]["cookie_info"]["cookie_token"].asText()
        cookie += "cookie_token=$cookieToken; account_id=$accountId; "
        val loginResponse = OkHttpKtUtils.post("https://bbs-api.mihoyo.com/user/wapi/login",
            OkUtils.json("{\"gids\":\"2\"}"), OkUtils.cookie(cookie)).also { it.close() }
        val finaCookie = OkUtils.cookie(loginResponse)
        cookie += finaCookie
        cookie += "login_ticket=$ticket; "
        val entity = MiHoYoEntity().also {
            it.cookie = cookie
            it.aid = accountId
            it.ticket = ticket
        }
        val sToken = sToken(entity)
        entity.sToken = sToken
        return entity
    }

    context(HttpRequestBuilder)
    private fun MiHoYoFix.loginHeader() {
        val fix = this@MiHoYoFix
        headers {
            referer("https://user.mihoyo.com/")
            append("X-Rpc-Client_type", "4")
            append("X-Rpc-Device_fp", fix.fp)
            append("X-Rpc-Device_id", fix.device)
            append("X-Rpc-Device_model", "Windows%2010%2064-bit")
            append("X-Rpc-Device_name", "Chrome%20119.0.0.0")
            append("X-Rpc-Game_biz", "account_cn")
            append("X-Rpc-Mi_referrer", "https://user.mihoyo.com/")
            append("X-Rpc-Source", "accountWebsite")
            append("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
            append("cookie", "_MHYUUID=${fix.device}; DEVICEFP_SEED_ID=${MyUtils.randomNum(16)}; DEVICEFP_SEED_TIME=${System.currentTimeMillis()}; DEVICEFP=${fix.fp};")
        }
    }

    suspend fun webNewLogin(account: String, password: String, tgId: Long? = null): MiHoYoEntity {
        val fix = MiHoYoFix()
        val beforeJsonNode = client.get("https://webapi.account.mihoyo.com/Api/create_mmt?scene_type=1&now=${System.currentTimeMillis()}&reason=user.mihoyo.com%23%2Flogin%2Fpassword&action_type=login_by_password&account=$account&t=${System.currentTimeMillis()}") {
            fix.loginHeader()
        }.body<JsonNode>()
        val dataJsonNode = beforeJsonNode["data"]["mmt_data"]
        val gt = dataJsonNode.getString("gt")
        val mmtKey = dataJsonNode.getString("mmt_key")
        val rr = geeTestLogic.rr(gt, "https://user.mihoyo.com/", mmtKey = mmtKey, tgId = tgId,
            riskType = "icon")
        val rsaKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDDvekdPMHN3AYhm/vktJT+YJr7cI5DcsNKqdsx5DZX0gDuWFuIjzdwButrIYPNmRJ1G8ybDIF7oDW2eEpm5sMbL9zs9ExXCdvqrn51qELbqj0XxtMTIpaCHFSI50PfPpTFV9Xt/hmyVwokoOXFlAEgCn+QCgGs52bFoYMtyi+xEQIDAQAB"
        val enPassword = password.rsaEncrypt(rsaKey)
        val data = Jackson.toJsonString(rr.secCode)
        val response = client.post("https://webapi.account.mihoyo.com/Api/login_by_password") {
            setFormDataContent {
                append("account", account)
                append("password", enPassword)
                append("is_crypto", "true")
                append("mmt_key", mmtKey)
                append("geetest_v4_data", data)
                append("source", "user.mihoyo.com")
                append("t", System.currentTimeMillis().toString())
            }
            fix.loginHeader()
        }
        val loginJsonNode = response.body<JsonNode>()
        if (loginJsonNode["data"]["status"].asInt() != 1) error(loginJsonNode["data"]["msg"].asText())
        val infoDataJsonNode = loginJsonNode["data"]
        val cookie = response.cookie()
        val infoJsonNode = infoDataJsonNode["account_info"]
        val accountId = infoJsonNode.getString("account_id")
        val ticket = infoJsonNode.getString("weblogin_token")
        val cookieResponse = client.get("https://api-takumi.mihoyo.com/account/auth/api/getAccountInfoByLoginTicket") {
            cookieString(cookie)
        }
        val accountCookie = cookieResponse.cookie()
        return MiHoYoEntity().also {
            it.ticket = ticket
            it.aid = accountId
            it.cookie = cookie + accountCookie
        }
    }

    private fun webDs(): MiHoYoDs {
        val salt = "mx1x4xCahVMVUDJIkJ9H3jsHcUsiASGZ"
        val time = System.currentTimeMillis() / 1000
        val randomLetter = MyUtils.randomLetter(6)
        val md5 = "salt=$salt&t=$time&r=$randomLetter".md5()
        val ds = "$time,$randomLetter,$md5"
        return MiHoYoDs("2.64.0", "4", ds)
    }

    private fun hubDs(): MiHoYoDs {
        val salt = "AcpNVhfh0oedCobdCyFV8EE1jMOVDy9q"
        val time = System.currentTimeMillis() / 1000
        val randomLetter = MyUtils.randomLetter(6)
        val md5 = "salt=$salt&t=$time&r=$randomLetter".md5()
        val ds = "$time,$randomLetter,$md5"
        return MiHoYoDs("2.60.1", "2", ds)
    }

    private fun hubNewDs(data: Map<String, Any>? = null, params: Map<String, Any>? = null): MiHoYoDs {
        val salt = "t0qEgfub6cvueAPgR5m9aQWWVciEer7v"
        val t = System.currentTimeMillis() / 1000
        val r = (100001..200000).random().toString()
        val q = params?.let { urlParams -> urlEncode(urlParams) } ?: ""
        val b = data?.let { Jackson.toJsonString(it) } ?: "{}"
        val c = "salt=$salt&t=$t&r=$r&b=$b&q=$q".md5()
        val ds = "$t,$r,$c"
        return MiHoYoDs("2.60.1", "2", ds)
    }

    private fun ds(): MiHoYoDs {
        val salt = "JwYDpKvLj6MrMqqYU6jTKF17KNO2PXoS"
        val time = System.currentTimeMillis() / 1000
        val randomLetter = MyUtils.randomLetter(6)
        val md5 = "salt=$salt&t=$time&r=$randomLetter".md5()
        val ds = "$time,$randomLetter,$md5"
        return MiHoYoDs("2.63.1", "5", ds)
    }

    private fun newDs(data: Map<String, Any>? = null, params: Map<String, Any>? = null): MiHoYoDs {
        val salt = "JwYDpKvLj6MrMqqYU6jTKF17KNO2PXoS"
        val t = System.currentTimeMillis() / 1000
        val r = (100001..200000).random().toString()
        val b = data?.let { Jackson.toJsonString(it) } ?: "{}"
        val q = params?.let { urlParams -> urlEncode(urlParams) } ?: ""
        val c = "salt=$salt&t=$t&r=$r&b=$b&q=$q".md5()
        val ds = "$t,$r,$c"
        return MiHoYoDs("2.63.1", "2", ds)
    }

    private fun urlEncode(params: Map<String, Any>): String {
        return params.map { (key, value) -> "$key=$value" }.joinToString("&")
    }

    private suspend fun sign(miHoYoEntity: MiHoYoEntity, jsonNode: JsonNode, rrOcrResult: RrOcrResult? = null): JsonNode {
        return client.post("https://api-takumi.mihoyo.com/event/luna/sign") {
            setJsonBody("""
                    {"act_id":"e202311201442471","region":"${jsonNode["region"].asText()}","uid":"${jsonNode.getString("game_uid")}","lang":"zh-cn"}
                """.trimIndent())
            miHoYoEntity.fix.appAppend()
            headers {
                cookieString(miHoYoEntity.cookie)
                append("x-rpc-signgame", "hk4e")
                rrOcrResult?.let {
                    append("x-rpc-validate", it.validate)
                    append("x-rpc-seccode", "${it.validate}|jordan")
                    append("x-rpc-challenge", it.challenge)
                }
            }
        }.body<JsonNode>()
    }

    suspend fun sign(miHoYoEntity: MiHoYoEntity, tgId: Long? = null) {
        val ssJsonNode = OkHttpKtUtils.getJson("https://api-takumi.mihoyo.com/binding/api/getUserGameRolesByCookie?game_biz=hk4e_cn",
            OkUtils.cookie(miHoYoEntity.cookie))
        if (ssJsonNode.getInteger("retcode") != 0) error(ssJsonNode.getString("message"))
        val jsonArray = ssJsonNode["data"]["list"]
        if (jsonArray.size() == 0) error("您还没有原神角色！！")
        for (obj in jsonArray) {
            val jsonNode = sign(miHoYoEntity, obj)
            when (jsonNode.getInteger("retcode")) {
                0, -5003 -> {
//                    val data = jsonNode["data"]
//                    val gt = data["gt"].asText()
//                    if (gt.isNotEmpty()) {
//                        val challenge = data["challenge"].asText()
//                        val rr = geeTestLogic.rr(gt, "https://webstatic.mihoyo.com/", challenge, tgId = tgId)
//                        val node = sign(miHoYoEntity, obj, rr)
//                        if (node["retcode"].asInt() !in listOf(-5003, 0)) error(jsonNode["message"].asText())
//                    }
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

    // 1 2 26 30 37 34
    suspend fun hubSign(miHoYoEntity: MiHoYoEntity) {
        val jsonNode = client.post("https://bbs-api.mihoyo.com/apihub/app/api/signIn") {
            setJsonBody("""{"gids":"2"}""")
            miHoYoEntity.fix.hubNewAppend(mapOf("gids" to "2"))
            cookieString(miHoYoEntity.hubCookie())
        }.body<JsonNode>()
        jsonNode.check()
    }

    private suspend fun sToken(miHoYoEntity: MiHoYoEntity): String {
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
    var fp: String = MyUtils.randomLetterLowerNum(13)
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