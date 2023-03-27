@file:Suppress("SpellCheckingInspection", "unused")

package me.kuku.telegram.extension

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.*
import me.kuku.telegram.utils.*
import me.kuku.utils.*
import org.springframework.stereotype.Service
import org.telegram.abilitybots.api.util.AbilityExtension
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.io.InputStream

@Service
class LoginExtension(
    private val biliBiliService: BiliBiliService,
    private val baiduLogic: BaiduLogic,
    private val baiduService: BaiduService,
    private val douYuLogic: DouYuLogic,
    private val douYuService: DouYuService,
    private val hostLocService: HostLocService,
    private val huYaLogic: HuYaLogic,
    private val huYaService: HuYaService,
    private val kuGouService: KuGouService,
    private val kuGouLogic: KuGouLogic,
    private val netEaseService: NetEaseService,
    private val stepService: StepService,
    private val weiboService: WeiboService,
    private val miHoYoService: MiHoYoService,
    private val douYinService: DouYinService,
    private val twitterService: TwitterService,
    private val pixivService: PixivService,
    private val buffService: BuffService,
    private val smZdmService: SmZdmService,
    private val aliDriverService: AliDriverService,
    private val leiShenService: LeiShenService
): AbilityExtension {

    private fun loginKeyboardMarkup(): InlineKeyboardMarkup {
        val baiduButton = InlineKeyboardButton("百度").also { it.callbackData = "baiduLogin" }
        val biliBiliButton = InlineKeyboardButton("哔哩哔哩").also { it.callbackData = "biliBiliLogin" }
        val douYuButton = InlineKeyboardButton("斗鱼").also { it.callbackData = "douYuLogin" }
        val hostLocButton = InlineKeyboardButton("HostLoc").also { it.callbackData = "hostLocLogin" }
        val huYaButton = InlineKeyboardButton("虎牙").also { it.callbackData = "huYaLogin" }
        val kuGouButton = InlineKeyboardButton("酷狗").also { it.callbackData = "kuGouLogin" }
        val miHoYoButton = InlineKeyboardButton("米忽悠").also { it.callbackData = "miHoYoLogin" }
        val netEaseButton = InlineKeyboardButton("网易云音乐").also { it.callbackData = "netEaseLogin" }
        val xiaomiStepButton = InlineKeyboardButton("小米运动").also { it.callbackData = "xiaomiStepLogin" }
        val leXinStepButton = InlineKeyboardButton("乐心运动").also { it.callbackData = "leXinStepLogin" }
        val weiboStepButton = InlineKeyboardButton("微博").also { it.callbackData = "weiboLogin" }
        val douYinButton = InlineKeyboardButton("抖音").also { it.callbackData = "douYinLogin" }
        val twitterButton = InlineKeyboardButton("twitter").also { it.callbackData = "twitterLogin" }
        val pixivButton = InlineKeyboardButton("pixiv").also { it.callbackData = "pixivLogin" }
        val buffButton = InlineKeyboardButton("网易Buff").also { it.callbackData = "buffLogin" }
        val smZdmButton = inlineKeyboardButton("什么值得买", "smZdmLogin")
        val aliDriverButton = inlineKeyboardButton("阿里云盘", "aliDriverLogin")
        val leiShenButton = inlineKeyboardButton("雷神加速器", "leiShenLogin")
        return InlineKeyboardMarkup(listOf(
            listOf(baiduButton, biliBiliButton),
            listOf(douYuButton, hostLocButton),
            listOf(huYaButton, kuGouButton),
            listOf(miHoYoButton, netEaseButton),
            listOf(xiaomiStepButton, leXinStepButton),
            listOf(weiboStepButton, douYinButton),
            listOf(twitterButton, pixivButton),
            listOf(buffButton, smZdmButton),
            listOf(aliDriverButton, leiShenButton)
        ))
    }

    fun AbilitySubscriber.login() {
        sub("login", "登录") {
            val markup = loginKeyboardMarkup()
            sendMessage("请选择登录选项", markup)
        }
    }

    fun TelegramSubscribe.baiduLogin() {
        callback("baiduLogin") {
            val qrcode = baiduLogic.getQrcode()
            var photoMessage: Message?
            OkHttpKtUtils.getByteStream(qrcode.image).use { iis ->
                val photo = SendPhoto(query.message.chatId.toString(), InputFile(iis, "百度登录二维码.jpg"))
                photoMessage = bot.execute(photo)
                editMessageText("请使用百度app扫描以下二维码登陆，百度网盘等均可", returnButton = false)
            }
            val baiduEntity = baiduService.findByTgId(query.from.id) ?: BaiduEntity().apply {
                tgId = query.from.id
            }
            var i = 0
            while (true) {
                if (++i > 20) {
                    photoMessage?.delete()
                    error("百度二维码已超时")
                }
                delay(3000)
                try {
                    val result = baiduLogic.checkQrcode(qrcode)
                    if (result.success()) {
                        val newEntity = result.data()
                        baiduEntity.cookie = newEntity.cookie
                        baiduService.save(baiduEntity)
                        editMessageText("绑定百度成功")
                    }
                } catch (ignore: Exception) {}
            }
        }
    }

    fun TelegramSubscribe.biliBiliLogin() {
        callback("biliBiliLogin") {
            val qrCodeUrl = BiliBiliLogic.loginByQr1()
            var photoMessage: Message?
            OkHttpKtUtils.getByteStream("https://api.kukuqaq.com/qrcode?text=${qrCodeUrl.toUrlEncode()}").use { iim ->
                val photo = SendPhoto(query.message.chatId.toString(), InputFile(iim, "哔哩哔哩登录二维码.jpg"))
                photoMessage = bot.execute(photo)
                editMessageText("请使用哔哩哔哩app扫描以下二维码登陆", returnButton = false)
            }
            var i = 0
            while (true) {
                if (++i > 10) {
                    editMessageText("哔哩哔哩二维码已超时")
                    break
                }
                delay(3000)
                val result = BiliBiliLogic.loginByQr2(qrCodeUrl)
                when (result.code) {
                    0 -> continue
                    200 -> {
                        val newEntity = result.data()
                        val biliBiliEntity = biliBiliService.findByTgId(query.from.id) ?: BiliBiliEntity().also { entity ->
                            entity.tgId = query.from.id
                        }
                        biliBiliEntity.cookie = newEntity.cookie
                        biliBiliEntity.userid = newEntity.userid
                        biliBiliEntity.token = newEntity.token
                        biliBiliService.save(biliBiliEntity)
                        editMessageText("绑定哔哩哔哩成功")
                        break
                    }
                    else -> {
                        editMessageText("哔哩哔哩登陆失败，${result.message}")
                        break
                    }
                }
            }
            photoMessage?.delete()
        }
    }

    fun TelegramSubscribe.douYu() {
        callback("douYuLogin") {
            val qrLogin = inlineKeyboardButton("扫码登录", "douYuQr")
            val douYuAppCookie = inlineKeyboardButton("斗鱼app的cookie", "douYuAppCookie")
            val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
                listOf(qrLogin),
                listOf(douYuAppCookie)
            ))
            editMessageText("斗鱼登录", inlineKeyboardMarkup)
        }
        callback("douYuQr") {
            val qrcode = douYuLogic.getQrcode()
            val imageUrl = qrcode.url
            var photoMessage: Message?
            client.get("https://api.kukuqaq.com/qrcode?text=${imageUrl.toUrlEncode()}").body<InputStream>().use { iim ->
                val photo = SendPhoto(query.message.chatId.toString(), InputFile(iim, "斗鱼登录二维码.jpg"))
                photoMessage = bot.execute(photo)
                editMessageText("请使用斗鱼app扫码二维码登录", returnButton = false)
            }
            var i = 0
            while (true) {
                if (i++ > 20) {
                    editMessageText("斗鱼登录二维码已失效")
                    break
                }
                delay(3000)
                val result = douYuLogic.checkQrcode(qrcode)
                when (result.code) {
                    0 -> continue
                    200 -> {
                        val newEntity = result.data()
                        val douYuEntity = douYuService.findByTgId(query.from.id) ?: DouYuEntity().apply {
                            tgId = query.from.id
                        }
                        douYuEntity.cookie = newEntity.cookie
                        douYuService.save(douYuEntity)
                        editMessageText("绑定斗鱼成功")
                        break
                    }
                    else -> {
                        editMessageText("绑定斗鱼失败，${result.message}")
                        break
                    }
                }
            }
            photoMessage?.delete()
        }
        callback("douYuAppCookie") {
            error("旧方法获取的cookie会失效，等待更新中")
        }
    }

    fun TelegramSubscribe.hostLocLogin() {
        callback("hostLocLogin") {
            editMessageText("请发送HostLoc账号")
            val accountMessage = nextMessage()
            val account = accountMessage.text
            editMessageText("请发送HostLoc密码")
            val passwordMessage = nextMessage()
            val password = passwordMessage.text
            val cookie = HostLocLogic.login(account, password)
            val hostLocEntity = hostLocService.findByTgId(tgId) ?: HostLocEntity().also { it.tgId = tgId }
            hostLocEntity.cookie = cookie
            hostLocService.save(hostLocEntity)
            editMessageText("绑定HostLoc成功")
        }
    }

    fun TelegramSubscribe.huYaLogin() {
        callback("huYaLogin") {
            val qrcode = huYaLogic.getQrcode()
            val photoMessage: Message?
            OkHttpKtUtils.getByteStream(qrcode.url).use { iim ->
                val photo = SendPhoto(query.message.chatId.toString(), InputFile(iim, "虎牙登录二维码.jpg"))
                photoMessage = bot.execute(photo)
                editMessageText("请使用虎牙App扫描二维码登录", returnButton = false)
            }
            var i = 0
            while (true) {
                if (i++ > 20) {
                    editMessageText("虎牙登录二维码已过期")
                    break
                }
                delay(3000)
                val result = huYaLogic.checkQrcode(qrcode)
                when (result.code) {
                    0 -> continue
                    200 -> {
                        val newEntity = result.data()
                        val huYaEntity = huYaService.findByTgId(tgId) ?: HuYaEntity().also { entity ->
                            entity.tgId = tgId
                        }
                        huYaEntity.cookie = newEntity.cookie
                        huYaService.save(huYaEntity)
                        editMessageText("绑定虎牙成功")
                        break
                    }
                    else -> {
                        editMessageText("绑定虎牙失败，${result.message}")
                        break
                    }
                }
            }
            photoMessage?.delete()
        }
    }

    fun TelegramSubscribe.kuGouLogin() {
        callback("kuGouLogin") {
            editMessageText("请发送酷狗登录的手机号")
            val phone = nextMessage().text
            val kuGouEntity = kuGouService.findByTgId(tgId) ?: KuGouEntity().also {
                it.mid = kuGouLogic.mid()
                it.tgId = tgId
            }
            val mid = kuGouEntity.mid
            val result = kuGouLogic.sendMobileCode(phone.toString(), mid)
            val message = if (result.success()) {
                editMessageText("请发送酷狗短信验证码")
                val code = nextMessage(1000 * 60 * 2).text
                val verifyResult = kuGouLogic.verifyCode(phone.toString(), code, mid)
                if (verifyResult.success()) {
                    val newKuGouEntity = verifyResult.data()
                    kuGouEntity.kuGoo = newKuGouEntity.kuGoo
                    kuGouEntity.token = newKuGouEntity.token
                    kuGouEntity.userid = newKuGouEntity.userid
                    kuGouService.save(kuGouEntity)
                    "绑定成功"
                } else verifyResult.message
            } else result.message
            editMessageText(message)
        }
    }

    fun TelegramSubscribe.miHoYoLogin(){
        callback("miHoYoLogin") {
            editMessageText("请发送米哈游的cookie")
            val cookie = nextMessage().text
            val newEntity = miHoYoService.findByTgId(tgId) ?: MiHoYoEntity().also {
                it.tgId = tgId
            }
            newEntity.cookie = cookie
            miHoYoService.save(newEntity)
            editMessageText("绑定米哈游成功")
        }
    }


    fun TelegramSubscribe.netEase() {
        callback("netEaseLogin") {
            val qrcodeButton = inlineKeyboardButton("扫码登录", "netEaseQrcodeLogin")
            val passwordButton = inlineKeyboardButton("手机密码登录", "netEasePasswordLogin")
            editMessageText("网易云登录", InlineKeyboardMarkup(listOf(listOf(qrcodeButton), listOf(passwordButton))))
        }
        callback("netEaseQrcodeLogin") {
            val key = NetEaseLogic.qrcode()
            val url = "http://music.163.com/login?codekey=$key"
            val newUrl =
                "https://api.kukuqaq.com/qrcode?text=${url.toUrlEncode()}"
            var photoMessage: Message?
            OkHttpKtUtils.getByteStream(newUrl).use { iis ->
                val photo = SendPhoto(query.message.chatId.toString(), InputFile(iis, "网易云音乐登录二维码.jpg"))
                photoMessage = bot.execute(photo)
                editMessageText("请使用网易云音乐App扫描下面二维码登录", returnButton = false)
            }
            var scan = true
            var i = 0
            while (true) {
                if (i++ > 20) {
                    editMessageText("网易云二维码已过期")
                    break
                }
                delay(3000)
                val result = NetEaseLogic.checkQrcode(key)
                when (result.code) {
                    200 -> {
                        val netEaseEntity = result.data()
                        val newEntity = netEaseService.findByTgId(tgId) ?: NetEaseEntity().also {
                            it.tgId = tgId
                        }
                        newEntity.csrf = netEaseEntity.csrf
                        newEntity.musicU = netEaseEntity.musicU
                        netEaseService.save(newEntity)
                        editMessageText("绑定网易云音乐成功")
                        break
                    }
                    500 -> {
                        editMessageText("网易云音乐登录失败，${result.message}")
                        break
                    }
                    1 -> {
                        if (scan) {
                            editMessageText(result.message)
                            scan = false
                        }
                    }
                }
            }
            photoMessage?.delete()
        }
        callback("netEasePasswordLogin") {
            editMessageText("请发送网易云音乐登录的手机号")
            val phone = nextMessage().text
            editMessageText("请发送网易云音乐登录的密码")
            val password = nextMessage().text
            val result = NetEaseLogic.login(phone, password)
            if (result.success()) {
                val entity = netEaseService.findByTgId(tgId) ?: NetEaseEntity().also { it.tgId = tgId }
                val newEntity = result.data()
                entity.csrf = newEntity.csrf
                entity.musicU = newEntity.musicU
                netEaseService.save(entity)
                editMessageText("绑定网易云音乐成功")
            } else editMessageText("绑定网易云音乐失败，${result.message}")
        }
    }

    fun TelegramSubscribe.xiaomiStepLogin() {
        callback("xiaomiStepLogin") {
            editMessageText("请发送小米运动手机号")
            val phone = nextMessage().text
            editMessageText("请发送小米运动密码")
            val password = nextMessage().text
            val result = XiaomiStepLogic.login(phone, password)
            if (result.success()) {
                val newEntity = result.data()
                val stepEntity = stepService.findByTgId(tgId) ?: StepEntity().also {
                    it.tgId = tgId
                }
                stepEntity.miLoginToken = newEntity.miLoginToken
                stepService.save(stepEntity)
                editMessageText("绑定小米运动成功")
            } else editMessageText("绑定小米运动失败，${result.message}")
        }
        callback("leXinStepLogin") {
            editMessageText("请发送乐心运动手机号")
            val phone = nextMessage().text
            editMessageText("请发送乐心运动密码")
            val password = nextMessage().text
            val result = LeXinStepLogic.login(phone, password)
            val message = if (result.success()) {
                val newStepEntity = result.data()
                val stepEntity = stepService.findByTgId(query.from.id) ?: StepEntity().apply {
                    tgId = query.from.id
                }
                stepEntity.leXinCookie = newStepEntity.leXinCookie
                stepEntity.leXinUserid = newStepEntity.leXinUserid
                stepEntity.leXinAccessToken = newStepEntity.leXinAccessToken
                stepService.save(stepEntity)
                "绑定乐心运动成功"
            } else result.message
            editMessageText(message)
        }
    }

    fun TelegramSubscribe.weiboLogin() {
        callback("weiboLogin") {
            editMessageText("请发送微博账号")
            val account = nextMessage().text
            editMessageText("请发送微博密码")
            val password = nextMessage().text
            val weiboLoginVerify = WeiboLogic.login(account, password)
            WeiboLogic.loginByPrivateMsg1(weiboLoginVerify)
            editMessageText("微博需要私信验证，请打开微博app或者网页查看*微博安全中心*发送的验证码", parseMode = "Markdown")
            val code = nextMessage(1000 * 60 * 2).text
            val newEntity = WeiboLogic.loginByPrivateMsg2(weiboLoginVerify, code)
            val weiboEntity = weiboService.findByTgId(tgId) ?: WeiboEntity().also { it.tgId = tgId }
            weiboEntity.cookie = newEntity.cookie
            weiboService.save(weiboEntity)
            editMessageText("绑定微博成功")
        }
    }

    fun TelegramSubscribe.douYinLogin() {
        callback("douYinLogin") {
            val qrcode = DouYinLogic.qrcode()
            var photoMessage: Message?
            qrcode.baseImage.base64Decode().inputStream().use { iis ->
                val photo = SendPhoto(chatId.toString(), InputFile(iis, "抖音登录二维码.jpg"))
                photoMessage = bot.execute(photo)
                editMessageText("请使用抖音App扫码登录", returnButton = false)
            }
            var i = 0
            while (true) {
                if (i++ > 20) {
                    editMessageText("抖音二维码已过期")
                    break
                }
                val result = DouYinLogic.checkQrcode(qrcode)
                if (result.code == 200) {
                    val newDouYinEntity = result.data()
                    val douYinEntity = douYinService.findByTgId(tgId) ?: DouYinEntity().also { it.tgId = tgId }
                    douYinEntity.cookie = newDouYinEntity.cookie
                    douYinEntity.userid = newDouYinEntity.userid
                    douYinEntity.secUserid = newDouYinEntity.secUserid
                    douYinService.save(douYinEntity)
                    editMessageText("绑定抖音成功")
                    break
                } else if (result.code == 500) {
                    editMessageText("绑定抖音失败，${result.message}")
                }
                delay(2000)
            }
            photoMessage?.delete()
        }
    }

    fun TelegramSubscribe.twitterLogin()  {
        callback("twitterLogin") {
            val loginButton = inlineKeyboardButton("模拟登录", "twitterLoginByUsername")
            val cookieButton = inlineKeyboardButton("cookie登录", "twitterCookieLogin")
            val markup = InlineKeyboardMarkup(listOf(
                listOf(loginButton),
                listOf(cookieButton)
            ))
            editMessageText("请选择twitter登录方式", markup)
        }
        callback("twitterLoginByUsername") {
            editMessageText("请发送twitter的用户名")
            val username = nextMessage().text
            editMessageText("请发送twitter的密码")
            val password = nextMessage().text
            val twitterEntity = TwitterLogic.login(username, password)
            val queryEntity = twitterService.findByTgId(tgId) ?: TwitterEntity().also { en -> en.tgId = tgId }
            queryEntity.cookie = twitterEntity.cookie
            queryEntity.csrf = twitterEntity.csrf
            queryEntity.tId = twitterEntity.tId
            queryEntity.tRestId = twitterEntity.tRestId
            twitterService.save(queryEntity)
            editMessageText("绑定twitter成功")
        }
        callback("twitterCookieLogin") {
            editMessageText("请发送twitter的cookie")
            val cookie = nextMessage().text
            val ct0 = OkUtils.cookie(cookie, "ct0") ?: error("cookie中必须包含ct0")
            val entity = TwitterEntity().also { entity ->
                entity.cookie = cookie
                entity.csrf = ct0
            }
            TwitterLogic.friendTweet(entity)
            val queryEntity = twitterService.findByTgId(tgId) ?: TwitterEntity().also { en -> en.tgId = tgId }
            queryEntity.cookie = cookie
            queryEntity.csrf = ct0
            twitterService.save(queryEntity)
            editMessageText("绑定twitter成功")
        }
    }

    fun TelegramSubscribe.pixivLogin() {
        callback("pixivLogin") {
            val loginButton = inlineKeyboardButton("使用微博app扫码登陆", "pixivLoginByWeibo")
            val cookieButton = inlineKeyboardButton("cookie登录", "pixivCookieLogin")
            val markup = InlineKeyboardMarkup(listOf(
                listOf(loginButton),
                listOf(cookieButton)
            ))
            editMessageText("请选择pixiv登录方式", markup)
        }
        callback("pixivLoginByWeibo") {
            error("没写")
        }
        callback("pixivCookieLogin") {
            editMessageText("请发送pixiv的cookie")
            val cookie = nextMessage().text
            PixivLogic.followImage(PixivEntity().also { ii -> ii.cookie = cookie })
            val pixivEntity = pixivService.findByTgId(tgId) ?: PixivEntity().also { ii -> ii.tgId = tgId }
            pixivEntity.cookie = cookie
            pixivService.save(pixivEntity)
            editMessageText("绑定pixiv成功")
        }
    }

    fun TelegramSubscribe.buffLogin() {
        callback("buffLogin") {
            val loginButton = inlineKeyboardButton("使用手机验证码登陆", "buffLoginByPhoneCode")
            val cookieButton = inlineKeyboardButton("cookie登录", "buffLoginByCookie")
            val markup = InlineKeyboardMarkup(listOf(
                listOf(loginButton),
                listOf(cookieButton)
            ))
            editMessageText("请选择网易buff登录方式", markup)
        }
        callback("buffLoginByPhoneCode") {
            editMessageText("请发送网易Buff的手机号")
            val phone = nextMessage().text
            var s = false
            for (i in 0..2) {
                s = kotlin.runCatching {
                    BuffLogic.login1(phone)
                    true
                }.getOrDefault(false)
                delay(1000)
            }
            if (!s) error("验证码识别失败，请重试")
            editMessageText("请发送网易buff的验证码")
            val code = nextMessage().text
            val buffEntity = BuffLogic.login2(phone, code)
            val saveEntity = buffService.findByTgId(tgId) ?: BuffEntity().also { entity -> entity.tgId = tgId }
            saveEntity.csrf = buffEntity.csrf
            saveEntity.cookie = buffEntity.cookie
            buffService.save(saveEntity)
            editMessageText("绑定网易buff成功")
        }
        callback("buffLoginByCookie") {
            editMessageText("请发送网易buff的cookie")
            val cookie = nextMessage().text
            BuffLogic.search(BuffEntity().also { en -> en.cookie = cookie }, "m9刺刀")
            val buffEntity = buffService.findByTgId(tgId) ?: BuffEntity().also { ii -> ii.tgId = tgId }
            buffEntity.cookie = cookie
            buffService.save(buffEntity)
            editMessageText("绑定pixiv成功")
        }
    }

    fun TelegramSubscribe.smZdm() {
        callback("smZdmLogin") {
            val loginButton = inlineKeyboardButton("使用手机验证码登陆", "smZdmLoginByPhoneCode")
            val wechatQrcodeButton = inlineKeyboardButton("使用微信扫码登陆", "smZdmWechatLoginByPhoneCode")
            val appQrcodeButton = inlineKeyboardButton("使用App扫码登陆", "smZdmAppQrcodeLoginByPhoneCode")
            val cookieButton = inlineKeyboardButton("cookie登录", "smZdmLoginByCookie")
            val markup = InlineKeyboardMarkup(listOf(
                listOf(loginButton),
                listOf(wechatQrcodeButton),
                listOf(appQrcodeButton),
                listOf(cookieButton)
            ))
            editMessageText("请选择什么值得买登录方式", markup)
        }
        callback("smZdmLoginByPhoneCode") {
            editMessageText("请发送什么值得买的手机号码")
            val phone = nextMessage().text
            SmZdmLogic.login1(phone)
            editMessageText("请发送什么值得买的验证码")
            val code = nextMessage().text
            val newEntity = SmZdmLogic.login2(phone, code)
            val smZdmEntity = smZdmService.findByTgId(tgId) ?: SmZdmEntity().also { it.tgId = tgId }
            smZdmEntity.cookie = newEntity.cookie
            smZdmService.save(smZdmEntity)
            editMessageText("绑定什么值得买成功")
        }
        callback("smZdmLoginByCookie") {
            editMessageText("请发送什么值得买的cookie")
            val text = nextMessage().text
            SmZdmLogic.appSign(SmZdmEntity().also { it.cookie = text })
            val smZdmEntity = smZdmService.findByTgId(tgId) ?: SmZdmEntity().also { it.tgId = tgId }
            smZdmEntity.cookie = text
            smZdmService.save(smZdmEntity)
            editMessageText("绑定什么值得买成功")
        }
        callback("smZdmWechatLoginByPhoneCode") {
            val wechatQrcode = SmZdmLogic.wechatQrcode1()
            var photoMessage: Message?
            client.get(wechatQrcode.url).body<InputStream>().use {
                val sendPhoto = SendPhoto(chatId.toString(), InputFile(it, "smzdmWechat.jpg"))
                photoMessage = bot.execute(sendPhoto)
                editMessageText("请先在网页成功使用微信扫码成功登录一次，使用微信扫码登录，如未关注公众号，扫码关注公众号后再扫一次", returnButton = false)
            }
            var i = 0
            var fail = true
            while (true) {
                if (++i >= 20) break
                try {
                    delay(3000)
                    val result = SmZdmLogic.wechatQrcode2(wechatQrcode)
                    if (result.code == 200) {
                        val smZdmEntity = smZdmService.findByTgId(tgId) ?: SmZdmEntity().also { it.tgId = tgId }
                        smZdmEntity.cookie = result.data().cookie
                        smZdmService.save(smZdmEntity)
                        editMessageText("绑定什么值得买成功")
                        fail = false
                        break
                    }
                } catch (ignore: Exception) {
                }
            }
            photoMessage?.delete()
            if (fail)
                editMessageText("什么值得买二维码已过期")
        }
        callback("smZdmAppQrcodeLoginByPhoneCode") {
            val appQrcode = SmZdmLogic.appQrcode1()
            val url = appQrcode.url
            var photoMessage: Message?
            client.get("https://api.kukuqaq.com/qrcode?text=${url.toUrlEncode()}").body<InputStream>().use {
                val sendPhoto = SendPhoto(chatId.toString(), InputFile(it, "smzdmApp.jpg"))
                photoMessage = bot.execute(sendPhoto)
                editMessageText("请使用什么值得买App扫码登陆", returnButton = false)
            }
            var i = 0
            var fail = true
            while (true) {
                if (++i >= 20) break
                delay(3000)
                val result = SmZdmLogic.appQrcode2(appQrcode)
                if (result.code == 200) {
                    val newEntity = result.data()
                    val smZdmEntity = smZdmService.findByTgId(tgId) ?: SmZdmEntity().also { it.tgId = tgId }
                    smZdmEntity.cookie = newEntity.cookie
                    smZdmService.save(smZdmEntity)
                    editMessageText("绑定什么值得买成功")
                    fail = false
                    break
                }
            }
            photoMessage?.delete()
            if (fail)
                editMessageText("什么值得买二维码已过期")
        }
    }

    fun TelegramSubscribe.aliDriver() {
        callback("aliDriverLogin") {
            val qrcode = AliDriverLogic.login1()
            var photoMessage: Message?
            client.get("https://api.kukuqaq.com/qrcode?text=${qrcode.qrcodeUrl.toUrlEncode()}").body<InputStream>().use {
                val sendPhoto = SendPhoto.builder().chatId(chatId).photo(InputFile(it, "aliQrcode.png")).build()
                photoMessage = bot.execute(sendPhoto)
                editMessageText("请使用阿里云盘app扫码登陆", returnButton = false)
            }
            var i = 0
            while (true) {
                if (++i > 20) {
                    editMessageText("阿里云盘登陆二维码已过期")
                    break
                }
                delay(3000)
                val commonResult = AliDriverLogic.login2(qrcode)
                if (commonResult.success()) {
                    val data = commonResult.data()
                    val refreshToken = data.refreshToken
                    val aliDriverEntity = aliDriverService.findByTgId(tgId) ?: AliDriverEntity().also {
                        it.tgId = tgId
                    }
                    aliDriverEntity.refreshToken = refreshToken
                    aliDriverService.save(aliDriverEntity)
                    editMessageText("绑定阿里云盘成功")
                    break
                } else if (commonResult.code != 0) {
                    editMessageText(commonResult.message)
                    break
                }
            }
            photoMessage?.delete()
        }
    }

    fun TelegramSubscribe.leiShenLogin() {
        callback("leiShenLogin") {
            editMessageText("请发送手机号")
            val phone = nextMessage().text
            editMessageText("请发送密码")
            val password = nextMessage().text
            val leiShenEntity = LeiShenLogic.login(phone, password)
            leiShenService.findByTgId(tgId)?.let {
                leiShenEntity.id = it.id
            }
            leiShenEntity.tgId = tgId
            leiShenService.save(leiShenEntity)
            editMessageText("绑定雷神加速器成功")
        }
    }

}
