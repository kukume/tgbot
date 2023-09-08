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
    private fun MiHoYoFix.appAppend() {
        val fix = this@MiHoYoFix
        val ds = getDs()
        headers {
            append("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) miHoYoBBS/${ds.appVersion}")
            append("x-rpc-device_id", fix.device.replace("-", ""))
            append("x-rpc-client_type", ds.clientType)
            append("x-rpc-app_version", ds.appVersion)
            append("DS", ds.ds)
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

    suspend fun login(account: String, password: String): CommonResult<MiHoYoEntity> {
        val beforeJsonNode = OkHttpKtUtils.getJson("https://webapi.account.mihoyo.com/Api/create_mmt?scene_type=1&now=${System.currentTimeMillis()}&reason=bbs.mihoyo.com")
        val dataJsonNode = beforeJsonNode["data"]["mmt_data"]
        val challenge = dataJsonNode.getString("challenge")
        val gt = dataJsonNode.getString("gt")
        val mmtKey = dataJsonNode.getString("mmt_key")
        val rr = geeTestLogic.rr(gt, "https://bbs.mihoyo.com/ys/", challenge)
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
        if (infoDataJsonNode.getInteger("status") != 1) return CommonResult.failure(infoDataJsonNode.getString("msg"))
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
        return CommonResult.success(MiHoYoEntity().also { it.cookie = cookie })
    }

    private fun getDs(dsType: String? = null, newDS: Boolean = false, data: Map<String, Any>? = null, params: Map<String, Any>? = null): MiHoYoDs {
        var salt = "YVEIkzDFNHLeKXLxzqCA9TzxCpWwbIbk"
        var appVersion = "2.36.1"
        var clientType = "5"

        fun new(): String {
            val t = System.currentTimeMillis() / 1000
            val r = (100001..200000).random().toString()
            val b = data?.let { Jackson.toJsonString(it) } ?: ""
            val q = params?.let { urlParams -> urlEncode(urlParams) } ?: ""
            val c = "salt=$salt&t=$t&r=$r&b=$b&q=$q".md5()
            return "$t,$r,$c"
        }

        fun old(): String {
            val t = System.currentTimeMillis() / 1000
            val r = MyUtils.randomLetter(6)
            val c = "salt=$salt&t=$t&r=$r".md5()
            return "$t,$r,$c"
        }


        var ds = old()

        when (dsType) {
            "2", "android" -> {
                salt = "n0KjuIrKgLHh08LWSCYP0WXlVXaYvV64"
                appVersion = "2.36.1"
                clientType = "2"
                ds = old()
            }
            "android_new" -> {
                salt = "t0qEgfub6cvueAPgR5m9aQWWVciEer7v"
                appVersion = "2.36.1"
                clientType = "2"
                ds = new()
            }
        }

        if (newDS) {
            salt = "xV8v4Qu54lUKrEYFZkJhB8cuOh9Asafs"
            appVersion = "2.36.1"
            clientType = "5"
            ds = new()
        }

        return MiHoYoDs(appVersion, clientType, ds)
    }

    private fun urlEncode(params: Map<String, Any>): String {
        return params.map { (key, value) -> "$key=$value" }.joinToString("&")
    }

    private suspend fun sign(miHoYoEntity: MiHoYoEntity, jsonNode: JsonNode, rrOcrResult: RrOcrResult? = null): JsonNode {
        return client.post("https://api-takumi.mihoyo.com/event/bbs_sign_reward/sign") {
            setJsonBody("""
                    {"act_id":"e202009291139501","region":"${jsonNode["region"].asText()}","uid":"${jsonNode.getString("game_uid")}"}
                """.trimIndent())
            miHoYoEntity.fix.appAppend()
            headers {
                cookieString(miHoYoEntity.cookie)
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
                    val data = jsonNode["data"]
                    val gt = data["gt"].asText()
                    if (gt.isNotEmpty()) {
                        val challenge = data["challenge"].asText()
                        val rr = geeTestLogic.rr(gt, "https://webstatic.mihoyo.com/", challenge, tgId = tgId)
                        val node = sign(miHoYoEntity, obj, rr)
                        if (node["retcode"].asInt() !in listOf(-5003, 0)) error(jsonNode["message"].asText())
                    }
                }
                else -> error(jsonNode["message"].asText() ?: "未知错误")
            }
        }

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