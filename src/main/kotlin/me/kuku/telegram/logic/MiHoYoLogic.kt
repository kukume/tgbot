package me.kuku.telegram.logic

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.contains
import me.kuku.pojo.CommonResult
import me.kuku.pojo.UA
import me.kuku.telegram.entity.MiHoYoEntity
import me.kuku.utils.*
import java.util.*

object MiHoYoLogic {

    private const val version = "2.3.0"

    suspend fun login(account: String, password: String): CommonResult<MiHoYoEntity> {
        val beforeJsonNode = OkHttpKtUtils.getJson("https://webapi.account.mihoyo.com/Api/create_mmt?scene_type=1&now=${System.currentTimeMillis()}&reason=bbs.mihoyo.com")
        val dataJsonNode = beforeJsonNode["data"]["mmt_data"]
        val challenge = dataJsonNode.getString("challenge")
        val gt = dataJsonNode.getString("gt")
        val mmtKey = dataJsonNode.getString("mmt_key")
        val jsonNode = OkHttpKtUtils.postJson("https://api.kukuqaq.com/geetest",
            mapOf("challenge" to challenge, "gt" to gt, "referer" to "https://bbs.mihoyo.com/ys/"))
        if (jsonNode.contains("code")) return CommonResult.failure("验证码识别失败，请重试")
        val cha = jsonNode.getString("challenge")
        val validate = jsonNode.getString("validate")
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

    private fun ds(n: String = "h8w582wxwgqvahcdkpvdhbh2w9casgfl"): String {
        val i = System.currentTimeMillis() / 1000
        val r = MyUtils.randomLetter(6)
        val c = MD5Utils.toMD5("salt=$n&t=$i&r=$r")
        return "$i,$r,$c"
    }

    private fun headerMap(miHoYoEntity: MiHoYoEntity): Map<String, String> {
        return mapOf("DS" to ds(), "x-rpc-app_version" to version, "x-rpc-client_type" to "5",
            "x-rpc-device_id" to UUID.randomUUID().toString(), "user-agent" to "Mozilla/5.0 (Linux; Android 10; V1914A Build/QP1A.190711.020; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/88.0.4324.181 Mobile Safari/537.36 miHoYoBBS/2.5.1",
            "Referer" to "https://webstatic.mihoyo.com/bbs/event/signin-ys/index.html?bbs_auth_required=true&act_id=e202009291139501&utm_source=bbs&utm_medium=mys&utm_campaign=icon",
            "cookie" to miHoYoEntity.cookie)
    }

    suspend fun sign(miHoYoEntity: MiHoYoEntity): String {
        val ssJsonNode = OkHttpKtUtils.getJson("https://api-takumi.mihoyo.com/binding/api/getUserGameRolesByCookie?game_biz=hk4e_cn",
            OkUtils.cookie(miHoYoEntity.cookie))
        if (ssJsonNode.getInteger("retcode") != 0) error(ssJsonNode.getString("message"))
        val jsonArray = ssJsonNode["data"]["list"]
        if (jsonArray.size() == 0) error("您还没有原神角色！！")
        var jsonNode: JsonNode? = null
        for (obj in jsonArray) {
            jsonNode = OkHttpKtUtils.postJson("https://api-takumi.mihoyo.com/event/bbs_sign_reward/sign",
                OkUtils.json("{\"act_id\":\"e202009291139501\",\"region\":\"cn_gf01\",\"uid\":\"${obj.getString("game_uid")}\"}"),
                headerMap(miHoYoEntity))
        }
        return when (jsonNode?.getInteger("retcode")) {
            0 -> "签到成功！！"
            -5003 -> "今日已签到！！"
            else -> error(jsonNode?.get("message")?.asText() ?: "未知错误")
        }
    }

}
