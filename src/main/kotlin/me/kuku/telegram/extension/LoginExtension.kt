@file:Suppress("SpellCheckingInspection", "unused")

package me.kuku.telegram.extension

import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendPhoto
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import me.kuku.telegram.context.*
import me.kuku.telegram.entity.*
import me.kuku.telegram.exception.QrcodeScanException
import me.kuku.telegram.logic.*
import me.kuku.telegram.utils.*
import me.kuku.utils.*
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class LoginExtension(
    private val configService: ConfigService,
    private val twoCaptchaLogic: TwoCaptchaLogic,
    private val biliBiliService: BiliBiliService,
    private val baiduLogic: BaiduLogic, private val baiduService: BaiduService,
    private val douYuLogic: DouYuLogic, private val douYuService: DouYuService,
    private val hostLocService: HostLocService,
    private val huYaLogic: HuYaLogic, private val huYaService: HuYaService,
    private val kuGouService: KuGouService, private val kuGouLogic: KuGouLogic,
    private val netEaseService: NetEaseService,
    private val stepService: StepService,
    private val weiboService: WeiboService,
    private val miHoYoService: MiHoYoService, private val miHoYoLogic: MiHoYoLogic,
    private val twitterService: TwitterService,
    private val pixivService: PixivService,
    private val smZdmService: SmZdmService, private val smZdmLogic: SmZdmLogic,
    private val aliDriveLogic: AliDriveLogic, private val aliDriveService: AliDriveService,
    private val leiShenService: LeiShenService,
    private val nodeSeekService: NodeSeekService,
    private val glaDosService: GlaDosService,
    private val iqyService: IqyService,
    private val eCloudService: ECloudService, private val eCloudLogic: ECloudLogic,
    private val linuxDoService: LinuxDoService
) {

    private fun loginKeyboardMarkup(): InlineKeyboardMarkup {
        val baiduButton = InlineKeyboardButton("百度").callbackData("baiduLogin")
        val biliBiliButton = InlineKeyboardButton("哔哩哔哩").callbackData("biliBiliLogin")
        val douYuButton = InlineKeyboardButton("斗鱼").callbackData("douYuLogin")
        val hostLocButton = InlineKeyboardButton("HostLoc").callbackData("hostLocLogin")
        val huYaButton = InlineKeyboardButton("虎牙").callbackData("huYaLogin")
        val kuGouButton = InlineKeyboardButton("酷狗").callbackData("kuGouLogin")
        val miHoYoButton = InlineKeyboardButton("米哈游").callbackData("miHoYoLogin")
        val netEaseButton = InlineKeyboardButton("网易云音乐").callbackData("netEaseLogin")
        val stepButton = InlineKeyboardButton("刷步数").callbackData("stepLogin")
        val weiboStepButton = InlineKeyboardButton("微博").callbackData("weiboLogin")
        val twitterButton = InlineKeyboardButton("twitter").callbackData("twitterLogin")
        val pixivButton = InlineKeyboardButton("pixiv").callbackData("pixivLogin")
        val smZdmButton = inlineKeyboardButton("什么值得买", "smZdmLogin")
        val aliDriveButton = inlineKeyboardButton("阿里云盘", "aliDriveLogin")
        val leiShenButton = inlineKeyboardButton("雷神加速器", "leiShenLogin")
        val nodeSeekButton = inlineKeyboardButton("NodeSeek", "nodeSeekLogin")
        val gloDos = inlineKeyboardButton("GloDos", "gloDosLogin")
        val iqy = inlineKeyboardButton("爱奇艺", "iqyLogin")
        val eCloud = inlineKeyboardButton("天翼云盘", "eCloudLogin")
        val linuxDo = inlineKeyboardButton("LinuxDo", "linuxDoLogin")
        return InlineKeyboardMarkup(
            arrayOf(baiduButton, biliBiliButton),
            arrayOf(douYuButton, hostLocButton),
            arrayOf(huYaButton, kuGouButton),
            arrayOf(miHoYoButton, netEaseButton),
            arrayOf(stepButton, weiboStepButton),
            arrayOf(twitterButton, pixivButton),
            arrayOf(smZdmButton, aliDriveButton),
            arrayOf(leiShenButton, nodeSeekButton),
            arrayOf(gloDos, iqy),
            arrayOf(eCloud, linuxDo)
        )
    }

    fun AbilitySubscriber.login() {
        sub("login") {
            val markup = loginKeyboardMarkup()
            sendMessage("请选择登录选项", markup)
        }
    }

    fun TelegramSubscribe.baiduLogin() {
        callback("baiduLogin") {
            editMessageText("请选择百度登录方式", InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("使用百度系app扫码登录", "baiduQrcodeLogin"))
            ))
        }
        callback("baiduQrcodeLogin") {
            val qrcode = baiduLogic.getQrcode()
            var photoMessage: Message?
            OkHttpKtUtils.getBytes(qrcode.image).let {
                val photo = SendPhoto(chatId, it)
                photoMessage = bot.asyncExecute(photo).message()
                editMessageText("请使用百度app扫描以下二维码登陆，百度网盘等均可", returnButton = false)
            }
            val baiduEntity = baiduService.findByTgId(query.from().id()) ?: BaiduEntity().init()
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
            editMessageText("请选择哔哩哔哩登录方式", InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("使用哔哩哔哩app扫码登录", "biliBiliQrcodeLogin"))
            ))
        }
        callback("biliBiliQrcodeLogin") {
            val qrcode = BiliBiliLogic.loginByQr1()
            var photoMessage: Message?
            qrcode(qrcode.url).let {
                val photo = SendPhoto(chatId, it)
                photoMessage = bot.asyncExecute(photo).message()
                editMessageText("请使用哔哩哔哩app扫描以下二维码登陆", returnButton = false)
            }
            var i = 0
            while (true) {
                if (++i > 10) {
                    editMessageText("哔哩哔哩二维码已超时")
                    break
                }
                delay(3000)
                val result = BiliBiliLogic.loginByQr2(qrcode)
                when (result.code) {
                    0 -> continue
                    200 -> {
                        val newEntity = result.data()
                        val biliBiliEntity = biliBiliService.findByTgId(query.from().id()) ?: BiliBiliEntity().init()
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
            val inlineKeyboardMarkup = InlineKeyboardMarkup(
                arrayOf(qrLogin)
            )
            editMessageText("斗鱼登录", inlineKeyboardMarkup)
        }
        callback("douYuQr") {
            val qrcode = douYuLogic.getQrcode()
            val imageUrl = qrcode.url
            var photoMessage: Message?
            qrcode(imageUrl).let {
                val photo = SendPhoto(chatId, it)
                photoMessage = bot.asyncExecute(photo).message()
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
                        val douYuEntity = douYuService.findByTgId(query.from().id()) ?: DouYuEntity().init()
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
    }

    fun TelegramSubscribe.hostLocLogin() {
        callback("hostLocLogin") {
            editMessageText("请选择HostLoc登录方式", InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("使用账号密码登录", "hostLocPasswordLogin"))
            ))
        }
        callback("hostLocPasswordLogin") {
            editMessageText("请发送HostLoc账号")
            val accountMessage = nextMessage()
            val account = accountMessage.text()
            editMessageText("请发送HostLoc密码")
            val passwordMessage = nextMessage()
            val password = passwordMessage.text()
            val cookie = HostLocLogic.login(account, password)
            val hostLocEntity = hostLocService.findByTgId(tgId) ?: HostLocEntity().init()
            hostLocEntity.cookie = cookie
            hostLocService.save(hostLocEntity)
            editMessageText("绑定HostLoc成功")
        }
    }

    fun TelegramSubscribe.huYaLogin() {
        callback("huYaLogin") {
            editMessageText("请选择虎牙登录方式", InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("使用虎牙app扫码登录", "huyaQrcodeLogin"))
            ))
        }
        callback("huyaQrcodeLogin") {
            val qrcode = huYaLogic.getQrcode()
            val photoMessage: Message?
            OkHttpKtUtils.getBytes(qrcode.url).let {
                val photo = SendPhoto(chatId, it)
                photoMessage = bot.asyncExecute(photo).message()
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
                        val huYaEntity = huYaService.findByTgId(tgId) ?: HuYaEntity().init()
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
            editMessageText("请选择酷狗登录方式", InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("使用手机验证码登录", "kuGouPhoneCaptchaLogin"))
            ))
        }
        callback("kuGouPhoneCaptchaLogin") {
            editMessageText("请发送酷狗登录的手机号")
            val phone = nextMessage().text()
            val kuGouEntity = kuGouService.findByTgId(tgId) ?: KuGouEntity().init<KuGouEntity>().also {
                it.mid = kuGouLogic.mid()
            }
            val mid = kuGouEntity.mid
            val result = kuGouLogic.sendMobileCode(phone.toString(), mid)
            val message = if (result.success()) {
                editMessageText("请发送酷狗短信验证码")
                val code = nextMessage(1000 * 60 * 2).text()
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

    @Suppress("DuplicatedCode")
    fun TelegramSubscribe.miHoYoLogin(){
        callback("miHoYoLogin") {
            editMessageText("请选择登录米哈游的方式\n注意：密码登陆大概率需要在/config配置rrcor的key", InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("使用cookie登录", "miHoYoCookieLogin")),
                arrayOf(inlineKeyboardButton("使用米游社app扫码登陆", "miHoYoQrcodeLogin")),
                arrayOf(inlineKeyboardButton("使用app账号密码登录", "miHoYoAppPasswordLogin")),
                arrayOf(inlineKeyboardButton("使用web账号密码登录", "miHoYoWebPasswordLogin"))
            ))
        }
        callback("miHoYoCookieLogin") {
            editMessageText("请发送米哈游的cookie")
            val cookie = nextMessage().text()
            val ticket = OkUtils.cookie(cookie, "login_ticket") ?: ""
            val accountId = OkUtils.cookie(cookie, "account_id") ?: ""
            val newEntity = miHoYoService.findByTgId(tgId) ?: MiHoYoEntity().init()
            newEntity.cookie = cookie
            newEntity.ticket = ticket
            newEntity.aid = accountId
            miHoYoService.save(newEntity)
            editMessageText("绑定米哈游成功")
        }
        callback("miHoYoQrcodeLogin") {
            val qrcode = miHoYoLogic.qrcodeLogin1()
            var photoMessage: Message?
            qrcode(qrcode.url).let {
                val photo = SendPhoto(chatId, it)
                photoMessage = bot.asyncExecute(photo).message()
                editMessageText("请使用米游社扫描下面二维码登录", returnButton = false)
            }
            var i = 0
            try {
                while (true) {
                    if (i++ > 20) {
                        editMessageText("米游社二维码已过期")
                        break
                    }
                    delay(3000)
                    val result = miHoYoLogic.qrcodeLogin2(qrcode)
                    when (result.code) {
                        200 -> {
                            val miHoYoEntity = result.data()
                            val newEntity = miHoYoService.findByTgId(tgId) ?: MiHoYoEntity().init()
                            newEntity.fix = miHoYoEntity.fix
                            newEntity.aid = miHoYoEntity.aid
                            newEntity.mid = miHoYoEntity.mid
                            newEntity.cookie = miHoYoEntity.cookie
                            miHoYoService.save(newEntity)
                            editMessageText("绑定米哈游成功")
                            break
                        }
                    }
                }
            } finally {
                photoMessage?.delete()
            }
        }
        callback("miHoYoAppPasswordLogin") {
            editMessageText("请发送账号")
            val account = nextMessage().text()
            editMessageText("请发送密码")
            val password = nextMessage().text()
            val entity = miHoYoLogic.login(account, password, tgId)
            val newEntity = miHoYoService.findByTgId(tgId) ?: MiHoYoEntity().init()
            newEntity.aid = entity.aid
            newEntity.mid = entity.mid
            newEntity.token = entity.token
            newEntity.ticket = entity.ticket
            newEntity.fix = entity.fix
            newEntity.sToken = entity.sToken
            miHoYoService.save(newEntity)
            editMessageText("绑定米哈游成功")
        }
        callback("miHoYoWebPasswordLogin") {
            editMessageText("请发送账号")
            val account = nextMessage().text()
            editMessageText("请发送密码")
            val password = nextMessage().text()
            val entity = miHoYoLogic.webLogin(account, password, tgId)
            val newEntity = miHoYoService.findByTgId(tgId) ?: MiHoYoEntity().init()
            newEntity.aid = entity.aid
            newEntity.token = entity.token
            newEntity.ticket = entity.ticket
            newEntity.fix = entity.fix
            newEntity.cookie = entity.cookie
            newEntity.sToken = entity.sToken
            miHoYoService.save(newEntity)
            editMessageText("绑定米哈游成功")
        }
    }


    fun TelegramSubscribe.netEase() {
        callback("netEaseLogin") {
            val qrcodeButton = inlineKeyboardButton("扫码登录", "netEaseQrcodeLogin")
            val passwordButton = inlineKeyboardButton("手机密码登录", "netEasePasswordLogin")
            editMessageText("网易云登录", InlineKeyboardMarkup(arrayOf(qrcodeButton), arrayOf(passwordButton)))
        }
        callback("netEaseQrcodeLogin") {
            val key = NetEaseLogic.qrcode()
            val url = "http://music.163.com/login?codekey=$key"
            var photoMessage: Message?
            qrcode(url).let {
                val photo = SendPhoto(chatId, it)
                photoMessage = bot.asyncExecute(photo).message()
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
                        val newEntity = netEaseService.findByTgId(tgId) ?: NetEaseEntity().init()
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
            val phone = nextMessage().text()
            editMessageText("请发送网易云音乐登录的密码")
            val password = nextMessage().text()
            val result = NetEaseLogic.login(phone, password)
            if (result.success()) {
                val entity = netEaseService.findByTgId(tgId) ?: NetEaseEntity().init()
                val newEntity = result.data()
                entity.csrf = newEntity.csrf
                entity.musicU = newEntity.musicU
                netEaseService.save(entity)
                editMessageText("绑定网易云音乐成功")
            } else editMessageText("绑定网易云音乐失败，${result.message}")
        }
    }

    fun TelegramSubscribe.xiaomiStepLogin() {
        callback("stepLogin") {
            editMessageText("""
                请选择刷步数登录类别，其中
                小米运动：是小米运动app的账号，不是小米账号
                乐心运动：需要绑定乐心手环才可以刷步数
            """.trimIndent(), InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("小米运动", "xiaomiStepLogin")),
                arrayOf(inlineKeyboardButton("乐心运动", "leXinStepLogin")),
            ))
        }
        callback("xiaomiStepLogin") {
            editMessageText("请选择小米运动登录方式", InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("使用账号密码登录", "xiaomiStepPasswordLogin"))
            ))
        }
        callback("xiaomiStepPasswordLogin") {
            editMessageText("请发送小米运动手机号")
            val phone = nextMessage().text()
            editMessageText("请发送小米运动密码")
            val password = nextMessage().text()
            val result = XiaomiStepLogic.login(phone, password)
            if (result.success()) {
                val newEntity = result.data()
                val stepEntity = stepService.findByTgId(tgId) ?: StepEntity().init()
                stepEntity.miLoginToken = newEntity.miLoginToken
                stepService.save(stepEntity)
                editMessageText("绑定小米运动成功")
            } else editMessageText("绑定小米运动失败，${result.message}")
        }
        callback("leXinStepLogin") {
            editMessageText("请选择乐心运动登录方式", InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("使用账号密码登录", "leXinStepPasswordLogin"))
            ))
        }
        callback("leXinStepPasswordLogin") {
            editMessageText("请发送乐心运动手机号")
            val phone = nextMessage().text()
            editMessageText("请发送乐心运动密码")
            val password = nextMessage().text()
            val result = LeXinStepLogic.login(phone, password)
            val message = if (result.success()) {
                val newStepEntity = result.data()
                val stepEntity = stepService.findByTgId(query.from().id()) ?: StepEntity().init()
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
            editMessageText("请选择微博登录方式", InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("使用微博app扫码登陆", "weiboQrcodeLogin"))
            ))
        }
        callback("weiboQrcodeLogin") {
            val qrcode = WeiboLogic.login1()
            val photoMessage: Message?
            client.get(qrcode.image).body<ByteArray>().let {
                val sendPhoto = SendPhoto(chatId, it)
                photoMessage = bot.asyncExecute(sendPhoto).message()
                editMessageText("使用微博app扫码登陆", returnButton = false)
            }
            var i = 0
            var fail = true
            while (true) {
                if (i++ > 20) break
                delay(3000)
                try {
                    val newEntity = WeiboLogic.login2(qrcode)
                    val weiboEntity = weiboService.findByTgId(tgId) ?: WeiboEntity().init()
                    weiboEntity.cookie = newEntity.cookie
                    weiboService.save(weiboEntity)
                    editMessageText("绑定微博成功")
                    fail = false
                    break
                } catch (e: QrcodeScanException) {
                    continue
                }
            }
            photoMessage?.delete()
            if (fail) {
                editMessageText("微博二维码已过期")
            }
        }
    }

    fun TelegramSubscribe.twitterLogin()  {
        callback("twitterLogin") {
            val loginButton = inlineKeyboardButton("模拟登录", "twitterLoginByUsername")
            val cookieButton = inlineKeyboardButton("cookie登录", "twitterCookieLogin")
            val markup = InlineKeyboardMarkup(
                arrayOf(loginButton),
                arrayOf(cookieButton)
            )
            editMessageText("请选择twitter登录方式", markup)
        }
        callback("twitterLoginByUsername") {
            editMessageText("请发送twitter的用户名")
            val username = nextMessage().text()
            editMessageText("请发送twitter的密码")
            val password = nextMessage().text()
            val twitterEntity = TwitterLogic.login(username, password)
            val queryEntity = twitterService.findByTgId(tgId) ?: TwitterEntity().init()
            queryEntity.cookie = twitterEntity.cookie
            queryEntity.csrf = twitterEntity.csrf
            queryEntity.tId = twitterEntity.tId
            queryEntity.tRestId = twitterEntity.tRestId
            twitterService.save(queryEntity)
            editMessageText("绑定twitter成功")
        }
        callback("twitterCookieLogin") {
            editMessageText("请发送twitter的cookie")
            val cookie = nextMessage().text()
            val ct0 = OkUtils.cookie(cookie, "ct0") ?: error("cookie中必须包含ct0")
            val entity = TwitterEntity().also { entity ->
                entity.cookie = cookie
                entity.csrf = ct0
            }
            TwitterLogic.friendTweet(entity)
            val queryEntity = twitterService.findByTgId(tgId) ?: TwitterEntity().init()
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
            val markup = InlineKeyboardMarkup(
                arrayOf(loginButton),
                arrayOf(cookieButton)
            )
            editMessageText("请选择pixiv登录方式", markup)
        }
        callback("pixivLoginByWeibo") {
            answerCallbackQuery("没写")
        }
        callback("pixivCookieLogin") {
            editMessageText("请发送pixiv的cookie")
            val cookie = nextMessage().text()
            PixivLogic.followImage(PixivEntity().also { ii -> ii.cookie = cookie })
            val pixivEntity = pixivService.findByTgId(tgId) ?: PixivEntity().init()
            pixivEntity.cookie = cookie
            pixivService.save(pixivEntity)
            editMessageText("绑定pixiv成功")
        }
    }

    fun TelegramSubscribe.smZdm() {
        callback("smZdmLogin") {
            val loginButton = inlineKeyboardButton("使用手机验证码登陆", "smZdmLoginByPhoneCode")
            val wechatQrcodeButton = inlineKeyboardButton("使用微信扫码登陆", "smZdmWechatLoginByPhoneCode")
            val appQrcodeButton = inlineKeyboardButton("使用App扫码登陆", "smZdmAppQrcodeLoginByPhoneCode")
            val cookieButton = inlineKeyboardButton("cookie登录", "smZdmLoginByCookie")
            val markup = InlineKeyboardMarkup(
                arrayOf(loginButton),
                arrayOf(wechatQrcodeButton),
                arrayOf(appQrcodeButton),
                arrayOf(cookieButton)
            )
            editMessageText("请选择什么值得买登录方式\n注意：因为什么值得买部分有极验验证码，所以你可能需要在/config中设配置rrocr密钥", markup)
        }
        callback("smZdmLoginByPhoneCode") {
            editMessageText("请发送什么值得买的手机号码")
            val phone = nextMessage().text()
            smZdmLogic.login1(phone)
            editMessageText("请发送什么值得买的验证码")
            val code = nextMessage().text()
            val newEntity = smZdmLogic.login2(phone, code)
            val smZdmEntity = smZdmService.findByTgId(tgId) ?: SmZdmEntity().init()
            smZdmEntity.cookie = newEntity.cookie
            smZdmService.save(smZdmEntity)
            editMessageText("绑定什么值得买成功")
        }
        callback("smZdmLoginByCookie") {
            editMessageText("请发送什么值得买的cookie")
            val text = nextMessage().text()
            smZdmLogic.appSign(SmZdmEntity().also { it.cookie = text })
            val smZdmEntity = smZdmService.findByTgId(tgId) ?: SmZdmEntity().init()
            smZdmEntity.cookie = text
            smZdmService.save(smZdmEntity)
            editMessageText("绑定什么值得买成功")
        }
        callback("smZdmWechatLoginByPhoneCode") {
            val wechatQrcode = smZdmLogic.wechatQrcode1()
            var photoMessage: Message?
            client.get(wechatQrcode.url).body<ByteArray>().let {
                val sendPhoto = SendPhoto(chatId, it)
                photoMessage = bot.asyncExecute(sendPhoto).message()
                editMessageText("请先在网页成功使用微信扫码成功登录一次，使用微信扫码登录，如未关注公众号，扫码关注公众号后再扫一次", returnButton = false)
            }
            var i = 0
            var fail = true
            while (true) {
                if (++i >= 20) break
                try {
                    delay(3000)
                    val result = smZdmLogic.wechatQrcode2(wechatQrcode)
                    if (result.code == 200) {
                        val smZdmEntity = smZdmService.findByTgId(tgId) ?: SmZdmEntity().init()
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
            val appQrcode = smZdmLogic.appQrcode1()
            val url = appQrcode.url
            var photoMessage: Message?
            qrcode(url).let {
                val sendPhoto = SendPhoto(chatId, it)
                photoMessage = bot.asyncExecute(sendPhoto).message()
                editMessageText("请使用什么值得买App扫码登陆", returnButton = false)
            }
            var i = 0
            var fail = true
            while (true) {
                if (++i >= 20) break
                delay(3000)
                val result = smZdmLogic.appQrcode2(appQrcode)
                if (result.code == 200) {
                    val newEntity = result.data()
                    val smZdmEntity = smZdmService.findByTgId(tgId) ?: SmZdmEntity().init()
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

    fun TelegramSubscribe.aliDrive() {
        callback("aliDriveLogin") {
            editMessageText("""
                请选择阿里云盘登录方式
                登陆成功之后账号可能会用于完成加入共享相册的任务
            """.trimIndent(), InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("使用阿里云盘app扫码登录", "aliDriveQrcodeLogin")),
                arrayOf(inlineKeyboardButton("使用阿里云盘RefreshToken登录", "aliDriveTokenLogin"))
            ))
        }
        callback("aliDriveQrcodeLogin") {
            val qrcode = aliDriveLogic.login1()
            var photoMessage: Message?
            qrcode(qrcode.qrcodeUrl).let {
                val sendPhoto = SendPhoto(chatId, it)
                photoMessage = bot.asyncExecute(sendPhoto).message()
                editMessageText("请使用阿里云盘app扫码登陆", returnButton = false)
            }
            var i = 0
            while (true) {
                if (++i > 20) {
                    editMessageText("阿里云盘登陆二维码已过期")
                    break
                }
                delay(3000)
                val commonResult = aliDriveLogic.login2(qrcode)
                if (commonResult.success()) {
                    val data = commonResult.data()
                    val refreshToken = data.refreshToken
                    val aliDriveEntity = aliDriveService.findByTgId(tgId) ?: AliDriveEntity().init()
                    aliDriveEntity.refreshToken = refreshToken
                    if (aliDriveEntity.deviceId.isEmpty()) aliDriveEntity.deviceId = UUID.randomUUID().toString()
                    aliDriveService.save(aliDriveEntity)
                    editMessageText("绑定阿里云盘成功")
                    break
                } else if (commonResult.code != 0) {
                    editMessageText(commonResult.message)
                    break
                }
            }
            photoMessage?.delete()
        }
        callback("aliDriveTokenLogin") {
            editMessageText("请发送阿里云盘的RefreshToken")
            val refreshToken = nextMessage().text()
            val aliDriveEntity = aliDriveService.findByTgId(tgId) ?: AliDriveEntity().init()
            aliDriveEntity.refreshToken = refreshToken
            if (aliDriveEntity.deviceId.isEmpty()) aliDriveEntity.deviceId = UUID.randomUUID().toString()
            aliDriveService.save(aliDriveEntity)
            editMessageText("绑定阿里云盘成功")
        }
    }

    fun TelegramSubscribe.leiShenLogin() {
        callback("leiShenLogin") {
            editMessageText("请选择雷神加速器登录方式\n使用账号密码登录会记录账号密码以自动更新cookie", InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("使用账号密码登录", "leiShenPasswordLogin"))
            ))
        }
        callback("leiShenPasswordLogin") {
            editMessageText("请发送手机号")
            val phone = nextMessage().text()
            editMessageText("请发送密码")
            val password = nextMessage().text()
            val leiShenEntity = LeiShenLogic.login(phone, password.md5())
            leiShenService.findByTgId(tgId)?.let {
                leiShenEntity.id = it.id
            }
            leiShenEntity.init<LeiShenEntity>()
            leiShenService.save(leiShenEntity)
            editMessageText("绑定雷神加速器成功")
        }
    }

    fun TelegramSubscribe.nodeSeekLogin() {
        callback("nodeSeekLogin") {
            editMessageText("请选择NodeSeek的登陆方式\n注意：使用账号密码登录，可能使用无头浏览器无法通过recaptchav3，通过recaptchav2需要前往/config配置2captcha密钥，建议抓取cookie登录", InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("使用cookie登陆", "nodeSeekCookieLogin")),
                arrayOf(inlineKeyboardButton("使用账号密码登陆", "nodeSeekPasswordLogin")),
            ))
        }
        callback("nodeSeekCookieLogin") {
            editMessageText("请发送cookie，请在浏览器抓包该网页的所有cookie")
            val cookie = nextMessage(errMessage = "您发送的cookie有误，需包含session=和cf_clearance等，请重新发送") {
                text().contains("session=") && text().contains("cf_clearance=")
            }.text()
            val entity = nodeSeekService.findByTgId(tgId) ?: NodeSeekEntity().init()
            entity.cookie = cookie
            nodeSeekService.save(entity)
            editMessageText("绑定NodeSeek成功")
        }
        callback("nodeSeekPasswordLogin") {
            editMessageText("请发送NodeSeek账号")
            val username = nextMessage().text()
            editMessageText("请发送NodeSeek密码")
            val password = nextMessage().text()
            var cookie: String? = null
            kotlin.runCatching {
                cookie = NodeSeekLogic.login(username, password)
            }.onFailure {
                val errMessage = it.message ?: "未知错误"
                if (errMessage.contains("通过recaptchaV3失败")) {
                    editMessageText("通过recaptchaV3失败，正在打码中")
                    val key = configService.findByTgId(tgId)?.twoCaptchaKey()
                    val token = twoCaptchaLogic.recaptchaV2(key, "6LfoOGcjAAAAAMh4fkiqTP48yS5Ey_P61wmfakV3", "https://www.nodeseek.com/signIn.html")
                    cookie = NodeSeekLogic.login(username, password, token)
                } else return@callback editMessageText(errMessage)
            }
            val entity = nodeSeekService.findByTgId(tgId) ?: NodeSeekEntity().init()
            cookie?.let {  entity.cookie = it }
            nodeSeekService.save(entity)
            editMessageText("绑定NodeSeek成功")
        }
    }

    fun TelegramSubscribe.gloDosSign() {
        callback("gloDosLogin") {
            editMessageText("请选择Glodos的登陆方式", InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("使用cookie登陆", "glaDosCookieLogin")),
                arrayOf(inlineKeyboardButton("使用邮箱验证码登录", "glaDosPasswordLogin")),
            ))
        }
        callback("glaDosCookieLogin") {
            editMessageText("请发送Glados的cookie")
            val text = nextMessage().text()
            val entity = glaDosService.findByTgId(tgId) ?: GlaDosEntity().init()
            entity.cookie = text
            glaDosService.save(entity)
            editMessageText("绑定Glados成功")
        }
        callback("glaDosPasswordLogin") {
            editMessageText("请发送Glados的登录邮箱")
            val email = nextMessage().text()
            GlaDosLogic.sendCode(email)
            editMessageText("请发送Glados登录邮箱${email}的验证码")
            val code = nextMessage().text()
            val cookie = GlaDosLogic.verifyCode(email, code)
            val entity = glaDosService.findByTgId(tgId) ?: GlaDosEntity().init()
            entity.cookie = cookie
            glaDosService.save(entity)
            editMessageText("绑定Glados成功")
        }
    }

    fun TelegramSubscribe.iqyLogin() {
        callback("iqyLogin") {
            editMessageText("请选择爱奇艺的登陆方式", InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("扫码登陆", "iqyQrcodeLogin")),
            ))
        }
        callback("iqyQrcodeLogin") {
            val qrcode = IqyLogic.login1()
            var photoMessage: Message?
            qrcode.imageUrl.let {
                val sendPhoto = SendPhoto(chatId, it)
                photoMessage = bot.asyncExecute(sendPhoto).message()
                editMessageText("请使用爱奇艺App扫码登陆", returnButton = false)
            }
            var i = 0
            while (true) {
                if (++i > 20) {
                    editMessageText("爱奇艺登陆二维码已过期")
                    break
                }
                delay(3000)
                val newIqyEntity = try {
                    IqyLogic.login2(qrcode)
                } catch (_: QrcodeScanException) {
                    continue
                }
                val iqyEntity = iqyService.findByTgId(tgId) ?: IqyEntity().init()
                iqyEntity.authCookie = newIqyEntity.authCookie
                iqyEntity.userid = newIqyEntity.userid
                iqyEntity.platform = newIqyEntity.platform
                iqyEntity.deviceId = newIqyEntity.deviceId
                iqyEntity.qyId = newIqyEntity.qyId
                iqyEntity.cookie = newIqyEntity.cookie
                iqyEntity.p00001 = newIqyEntity.p00001
                iqyService.save(iqyEntity)
                editMessageText("绑定爱奇艺成功")
                break
            }
            photoMessage?.delete()
        }
    }

    fun TelegramSubscribe.eCloudLogin() {
        callback("eCloudLogin") {
            editMessageText("请选择天翼云盘的登陆方式", InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("密码登录", "eCloudPasswordLogin")),
            ))
        }
        callback("eCloudPasswordLogin") {
            editMessageText("请发送天翼云盘账号")
            val username = nextMessage().text()
            editMessageText("请发送天翼云盘密码")
            val password = nextMessage().text()
            val newEntity = eCloudLogic.login(username, password)
            val entity = eCloudService.findByTgId(tgId) ?: ECloudEntity().init()
            entity.cookie = newEntity.cookie
            entity.eCookie = newEntity.eCookie
            eCloudService.save(entity)
            editMessageText("绑定天翼云盘成功")
        }
    }

    fun TelegramSubscribe.linuxDoLogin() {
        callback("linuxDoLogin") {
            editMessageText("""
                请选择LinuxDo的登陆方式
                cookie登陆，请新开一个无痕浏览器抓包获取cookie
            """.trimIndent(), InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("cookie登陆", "linuxDoCookieLogin")),
                arrayOf(inlineKeyboardButton("密码登陆", "linuxDoPasswordLogin"))
            ))
        }
        callback("linuxDoCookieLogin") {
            editMessageText("请发送LinuxDo的cookie")
            val text = nextMessage().text()
            LinuxDoLogic.check(text)
            val linuxDoEntity = linuxDoService.findByTgId(tgId) ?: LinuxDoEntity().init()
            linuxDoEntity.cookie = text
            linuxDoService.save(linuxDoEntity)
            editMessageText("绑定LinuxDo成功")
        }
        callback("linuxDoPasswordLogin") {
            editMessageText("请发送LinuxDo的用户名")
            val username = nextMessage().text()
            editMessageText("请发送LinuxDo的密码")
            val password = nextMessage().text()
            val cookie = LinuxDoLogic.login(username, password)
            val entity = linuxDoService.findByTgId(tgId) ?: LinuxDoEntity().init()
            entity.cookie = cookie
            linuxDoService.save(entity)
            editMessageText("绑定LinuxDo成功")
        }
    }

}
