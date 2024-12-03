@file:Suppress("SpellCheckingInspection", "unused")

package me.kuku.telegram.extension

import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.request.SendPhoto
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import me.kuku.telegram.context.*
import me.kuku.telegram.entity.*
import me.kuku.telegram.exception.QrcodeScanException
import me.kuku.telegram.logic.*
import me.kuku.telegram.utils.*
import org.springframework.stereotype.Service

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
    private val stepService: StepService,
    private val weiboService: WeiboService,
    private val miHoYoService: MiHoYoService, private val miHoYoLogic: MiHoYoLogic,
    private val smZdmService: SmZdmService, private val smZdmLogic: SmZdmLogic,
    private val leiShenService: LeiShenService,
    private val nodeSeekService: NodeSeekService,
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
        val stepButton = InlineKeyboardButton("刷步数").callbackData("stepLogin")
        val weiboStepButton = InlineKeyboardButton("微博").callbackData("weiboLogin")
        val smZdmButton = inlineKeyboardButton("什么值得买", "smZdmLogin")
        val leiShenButton = inlineKeyboardButton("雷神加速器", "leiShenLogin")
        val nodeSeekButton = inlineKeyboardButton("NodeSeek", "nodeSeekLogin")
        val eCloud = inlineKeyboardButton("天翼云盘", "eCloudLogin")
        val linuxDo = inlineKeyboardButton("LinuxDo", "linuxDoLogin")
        return InlineKeyboardMarkup(
            arrayOf(baiduButton, biliBiliButton),
            arrayOf(douYuButton, hostLocButton),
            arrayOf(huYaButton, kuGouButton),
            arrayOf(miHoYoButton),
            arrayOf(stepButton, weiboStepButton),
            arrayOf(smZdmButton),
            arrayOf(leiShenButton, nodeSeekButton),
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
            client.get(qrcode.image).body<ByteArray>().let {
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
                    val newEntity = baiduLogic.checkQrcode(qrcode)
                    baiduEntity.cookie = newEntity.cookie
                    baiduService.save(baiduEntity)
                    editMessageText("绑定百度成功")
                } catch (ignore: QrcodeScanException) {}
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
                val newEntity = try {
                    BiliBiliLogic.loginByQr2(qrcode)
                } catch (e: QrcodeScanException) {
                    continue
                }
                val biliBiliEntity = biliBiliService.findByTgId(query.from().id()) ?: BiliBiliEntity().init()
                biliBiliEntity.cookie = newEntity.cookie
                biliBiliEntity.userid = newEntity.userid
                biliBiliEntity.token = newEntity.token
                biliBiliService.save(biliBiliEntity)
                editMessageText("绑定哔哩哔哩成功")
                break
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
                val newEntity = try {
                    douYuLogic.checkQrcode(qrcode)
                } catch (e: QrcodeScanException) {
                    continue
                }
                val douYuEntity = douYuService.findByTgId(query.from().id()) ?: DouYuEntity().init()
                douYuEntity.cookie = newEntity.cookie
                douYuService.save(douYuEntity)
                editMessageText("绑定斗鱼成功")
                break
            }
            photoMessage?.delete()
        }
    }

    fun TelegramSubscribe.hostLocLogin() {
        callback("hostLocLogin") {
            editMessageText("请选择HostLoc登录方式", InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("使用账号密码登录", "hostLocPasswordLogin")),
                arrayOf(inlineKeyboardButton("使用cookie登录", "hostLocCookieLogin"))
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
        callback("hostLocCookieLogin") {
            editMessageText("请发送HostLoc的cookie")
            val cookie = nextMessage().text()
            HostLocLogic.singleSign(cookie)
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
            client.get(qrcode.url).body<ByteArray>().let {
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
                val newEntity = try {
                    huYaLogic.checkQrcode(qrcode)
                } catch (e: QrcodeScanException) {
                    continue
                }
                val huYaEntity = huYaService.findByTgId(tgId) ?: HuYaEntity().init()
                huYaEntity.cookie = newEntity.cookie
                huYaService.save(huYaEntity)
                editMessageText("绑定虎牙成功")
                break
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
            kuGouLogic.sendMobileCode(phone.toString(), mid)
            editMessageText("请发送酷狗短信验证码")
            val code = nextMessage(1000 * 60 * 2).text()
            val newKuGouEntity = kuGouLogic.verifyCode(phone.toString(), code, mid)
            kuGouEntity.kuGoo = newKuGouEntity.kuGoo
            kuGouEntity.token = newKuGouEntity.token
            kuGouEntity.userid = newKuGouEntity.userid
            kuGouService.save(kuGouEntity)
            editMessageText("绑定酷狗成功")
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
            val ticket = RegexUtils.extract(cookie, "login_ticket", ";") ?: ""
            val accountId = RegexUtils.extract(cookie, " account_id", ";") ?: ""
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
                    val miHoYoEntity = try {
                        miHoYoLogic.qrcodeLogin2(qrcode)
                    } catch (e: QrcodeScanException) {
                        continue
                    }
                    val newEntity = miHoYoService.findByTgId(tgId) ?: MiHoYoEntity().init()
                    newEntity.fix = miHoYoEntity.fix
                    newEntity.aid = miHoYoEntity.aid
                    newEntity.mid = miHoYoEntity.mid
                    newEntity.cookie = miHoYoEntity.cookie
                    miHoYoService.save(newEntity)
                    editMessageText("绑定米哈游成功")
                    break
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
            val newEntity = XiaomiStepLogic.login(phone, password)
            val stepEntity = stepService.findByTgId(tgId) ?: StepEntity().init()
            stepEntity.miLoginToken = newEntity.miLoginToken
            stepService.save(stepEntity)
            editMessageText("绑定小米运动成功")
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
            val newStepEntity = LeXinStepLogic.login(phone, password)
            val stepEntity = stepService.findByTgId(query.from().id()) ?: StepEntity().init()
            stepEntity.leXinCookie = newStepEntity.leXinCookie
            stepEntity.leXinUserid = newStepEntity.leXinUserid
            stepEntity.leXinAccessToken = newStepEntity.leXinAccessToken
            stepService.save(stepEntity)
            editMessageText("绑定乐心运动成功")
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
            editMessageText("请选择什么值得买登录方式\n注意：因为什么值得买部分有极验验证码，所以你可能需要在/config中设配置2captcha密钥", markup)
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
                    val newEntity = smZdmLogic.wechatQrcode2(wechatQrcode)
                    val smZdmEntity = smZdmService.findByTgId(tgId) ?: SmZdmEntity().init()
                    smZdmEntity.cookie = newEntity.cookie
                    smZdmService.save(smZdmEntity)
                    editMessageText("绑定什么值得买成功")
                    fail = false
                    break
                } catch (ignore: QrcodeScanException) {
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
                val newEntity = try {
                    smZdmLogic.appQrcode2(appQrcode)
                } catch (e: QrcodeScanException) {
                    continue
                }
                val smZdmEntity = smZdmService.findByTgId(tgId) ?: SmZdmEntity().init()
                smZdmEntity.cookie = newEntity.cookie
                smZdmService.save(smZdmEntity)
                editMessageText("绑定什么值得买成功")
                fail = false
                break
            }
            photoMessage?.delete()
            if (fail)
                editMessageText("什么值得买二维码已过期")
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
            editMessageText("请选择NodeSeek的登陆方式", InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("使用cookie登陆", "nodeSeekCookieLogin"))
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
