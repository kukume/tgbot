@file:Suppress("SpellCheckingInspection")

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
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.InputFile
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
    private val smZdmService: SmZdmService
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
        return InlineKeyboardMarkup(listOf(
            listOf(baiduButton, biliBiliButton),
            listOf(douYuButton, hostLocButton),
            listOf(huYaButton, kuGouButton),
            listOf(miHoYoButton, netEaseButton),
            listOf(xiaomiStepButton, leXinStepButton),
            listOf(weiboStepButton, douYinButton),
            listOf(twitterButton, pixivButton),
            listOf(buffButton, smZdmButton)
        ))
    }

    private fun returnButton(): List<InlineKeyboardButton> {
        return listOf(inlineKeyboardButton("返回", "returnLogin"))
    }

    fun returnLogin() = callback("returnLogin") {
        val chatId = query.message.chatId
        val messageId = query.message.messageId
        val editMessageText = EditMessageText.builder().chatId(chatId).messageId(messageId).text("请选择登录选项")
            .replyMarkup(loginKeyboardMarkup()).build()
        bot.execute(editMessageText)
    }

    fun login() = ability("login", "登录") {
        val markup = loginKeyboardMarkup()
        val sendMessage = SendMessage()
        sendMessage.replyMarkup = markup
        sendMessage.chatId = chatId().toString()
        sendMessage.text = "请选择登录选项"
        execute(sendMessage)
    }

    fun baiduLogin() = callback("baiduLogin") {
        val qrcode = baiduLogic.getQrcode()
        OkHttpKtUtils.getByteStream(qrcode.image).use { iis ->
            val photo = SendPhoto(query.message.chatId.toString(), InputFile(iis, "百度登录二维码.jpg"))
                .apply { caption = "请使用百度app扫码登陆，百度网盘等均可" }
            bot.execute(photo)
        }
        val baiduEntity = baiduService.findByTgId(query.from.id) ?: BaiduEntity().apply {
            tgId = query.from.id
        }
        var i = 0
        while (true) {
            if (++i > 10) error("百度二维码已超时")
            delay(3000)
            try {
                val result = baiduLogic.checkQrcode(qrcode)
                if (result.success()) {
                    val newEntity = result.data()
                    baiduEntity.cookie = newEntity.cookie
                    baiduService.save(baiduEntity)
                    val sendMessage = SendMessage(query.message.chatId.toString(), "绑定百度成功")
                    bot.execute(sendMessage)
                }
            } catch (ignore: Exception) {}
        }
    }

    fun biliBiliLogin() = callback("biliBiliLogin") {
        val qrCodeUrl = BiliBiliLogic.loginByQr1()
        val photoMessage = OkHttpKtUtils.getByteStream("https://api.kukuqaq.com/qrcode?text=${qrCodeUrl.toUrlEncode()}").use { iim ->
            val photo = SendPhoto(query.message.chatId.toString(), InputFile(iim, "哔哩哔哩登录二维码.jpg"))
                .apply { caption = "请使用哔哩哔哩app扫码登陆" }
            bot.execute(photo)
        }
        while (true) {
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
                    val message = SendMessage().also { age ->
                        age.chatId = query.message.chatId.toString()
                        age.text = "绑定哔哩哔哩成功"
                    }
                    val titleMessage = bot.execute(message)
                    delay(1000 * 10)
                    bot.execute(DeleteMessage(query.message.chatId.toString(), photoMessage.messageId))
                    bot.execute(DeleteMessage(query.message.chatId.toString(), titleMessage.messageId))
                    break
                }
                else -> {
                    val message = SendMessage().also { age ->
                        age.chatId = query.message.chatId.toString()
                        age.text = result.message
                    }
                    val titleMessage = bot.execute(message)
                    delay(1000 * 10)
                    bot.execute(DeleteMessage(query.message.chatId.toString(), photoMessage.messageId))
                    bot.execute(DeleteMessage(query.message.chatId.toString(), titleMessage.messageId))
                    break
                }
            }
        }
    }

    fun douYuLogin() = callback("douYuLogin") {
        val qrcode = douYuLogic.getQrcode()
        val imageUrl = qrcode.url
        client.get("https://api.kukuqaq.com/qrcode?text=${imageUrl.toUrlEncode()}").body<InputStream>().use { iim ->
            val photo = SendPhoto(query.message.chatId.toString(), InputFile(iim,
                "斗鱼登录二维码.jpg")).apply { caption = "请使用斗鱼app扫码登录" }
            bot.execute(photo)
        }
        while (true) {
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
                    val sendMessage = SendMessage(query.message.chatId.toString(), "绑定斗鱼成功")
                    bot.execute(sendMessage)
                    break
                }
                else -> {
                    val sendMessage = SendMessage(query.message.chatId.toString(), result.message)
                    bot.execute(sendMessage)
                    break
                }
            }
        }
    }

    fun hostLocLogin() = callback("hostLocLogin") {
        val chatId = query.message.chatId
        val userid = query.from.id
        val accountSendMessage = SendMessage(chatId.toString(), "请发送账号")
        bot.execute(accountSendMessage)
        val account = query.waitNextMessage().text
        val passwordSendMessage = SendMessage(chatId.toString(), "请发送密码")
        bot.execute(passwordSendMessage)
        val password = query.waitNextMessage().text
        val cookie = HostLocLogic.login(account, password)
        val hostLocEntity = hostLocService.findByTgId(userid) ?: HostLocEntity().apply { tgId = userid }
        hostLocEntity.cookie = cookie
        hostLocService.save(hostLocEntity)
        val sendMessage = SendMessage(chatId.toString(), "绑定HostLoc成功")
        bot.execute(sendMessage)
    }

    fun huYaLogin() = callback("huYaLogin") {
        val chatId = query.message.chatId
        val userid = query.from.id
        val qrcode = huYaLogic.getQrcode()
        OkHttpKtUtils.getByteStream(qrcode.url).use { iim ->
            val photo = SendPhoto(query.message.chatId.toString(), InputFile(iim,
                "虎牙登录二维码.jpg")).apply { caption = "请使用虎牙App扫码登录" }
            bot.execute(photo)
        }
        while (true) {
            delay(3000)
            val result = huYaLogic.checkQrcode(qrcode)
            when (result.code) {
                0 -> continue
                200 -> {
                    val newEntity = result.data()
                    val huYaEntity = huYaService.findByTgId(userid) ?: HuYaEntity().also { entity ->
                        entity.tgId = userid
                    }
                    huYaEntity.cookie = newEntity.cookie
                    huYaService.save(huYaEntity)
                    val sendMessage = SendMessage(chatId.toString(), "绑定虎牙成功")
                    bot.execute(sendMessage)
                    break
                }
                else -> {
                    val sendMessage = SendMessage(chatId.toString(), result.message)
                    bot.execute(sendMessage)
                    break
                }
            }
        }
    }

    fun kuGouLogin() = callback("kuGouLogin") {
        val chatId = query.message.chatId
        val userid = query.from.id
        bot.execute(SendMessage(chatId.toString(), "请发送手机号"))
        val phone = query.waitNextMessage().text.toLongOrNull() ?: return@callback kotlin.run {
            bot.execute(SendMessage(chatId.toString(), "发送的手机号有误"))
        }
        val kuGouEntity = kuGouService.findByTgId(userid) ?: KuGouEntity().apply {
            mid = kuGouLogic.mid()
            tgId = userid
        }
        val mid = kuGouEntity.mid
        val result = kuGouLogic.sendMobileCode(phone.toString(), mid)
        val message = if (result.success()) {
            bot.execute(SendMessage(chatId.toString(), "请发送短信验证码"))
            val code = query.waitNextMessage(1000 * 60 * 2).text
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
        bot.execute(SendMessage(chatId.toString(), message))
    }

    fun miHoYoLogin() = callback("miHoYoLogin") {
        val chatId = query.message.chatId
        val userid = query.from.id
        bot.execute(SendMessage(chatId.toString(), "请发送米哈游的cookie"))
        val cookie = query.waitNextMessage().text
        val newEntity = miHoYoService.findByTgId(userid) ?: MiHoYoEntity().apply {
            tgId = userid
        }
        newEntity.cookie = cookie
        miHoYoService.save(newEntity)
        bot.execute(SendMessage(chatId.toString(), "绑定米哈游成功"))
    }



    fun CallbackSubscriber.netEase() {
        "netEaseLogin" {
            val qrcodeButton = inlineKeyboardButton("扫码登录", "netEaseQrcodeLogin")
            val passwordButton = inlineKeyboardButton("手机密码登录", "netEasePasswordLogin")
            val sendMessage = EditMessageText.builder().text("网易云登录").chatId(chatId).messageId(message.messageId)
                .replyMarkup(InlineKeyboardMarkup(listOf(listOf(qrcodeButton), listOf(passwordButton), returnButton())))
                .build()
            bot.execute(sendMessage)
        }
        "netEaseQrcodeLogin" {
            val chatId = query.message.chatId
            val userid = query.from.id
            val key = NetEaseLogic.qrcode()
            val url = "http://music.163.com/login?codekey=$key"
            val newUrl =
                "https://api.kukuqaq.com/qrcode?text=${url.toUrlEncode()}"
            OkHttpKtUtils.getByteStream(newUrl).use { iis ->
                val photo = SendPhoto(query.message.chatId.toString(), InputFile(iis,
                    "网易云音乐登录二维码.jpg")).apply { caption = "请使用网易云音乐App扫码登录" }
                bot.execute(photo)
            }
            var scan = true
            while (true) {
                delay(3000)
                val result = NetEaseLogic.checkQrcode(key)
                when (result.code) {
                    200 -> {
                        val netEaseEntity = result.data()
                        val newEntity = netEaseService.findByTgId(userid) ?: NetEaseEntity().apply {
                            tgId = userid
                        }
                        newEntity.csrf = netEaseEntity.csrf
                        newEntity.musicU = netEaseEntity.musicU
                        netEaseService.save(newEntity)
                        bot.execute(SendMessage(chatId.toString(), "绑定网易云音乐成功"))
                        break
                    }
                    500 -> {
                        bot.execute(SendMessage(chatId.toString(), result.message))
                        break
                    }
                    1 -> {
                        if (scan) {
                            bot.execute(SendMessage(chatId.toString(), result.message))
                            scan = false
                        }
                    }
                }
            }
        }
        "netEasePasswordLogin" {
            val sendPhoneMessaeg = bot.execute(SendMessage.builder().text("请发送手机号").chatId(chatId).build())
            val phoneMessage = query.waitNextMessage()
            val phone = phoneMessage.text
            if (phone.length != 11) error("手机号码格式不正确")
            val sendPasswordMessage = bot.execute(SendMessage.builder().text("请发送密码").chatId(chatId).build())
            val passwordMessage = query.waitNextMessage()
            val password = passwordMessage.text
            val result = NetEaseLogic.login(phone, password)
            sendPhoneMessaeg.delete()
            phoneMessage.delete()
            sendPasswordMessage.delete()
            passwordMessage.delete()
            if (result.success()) {
                val newEntity = result.data()
                val entity = netEaseService.findByTgId(tgId) ?: NetEaseEntity().also { it.tgId = tgId }
                entity.csrf = newEntity.csrf
                entity.musicU = newEntity.musicU
                netEaseService.save(entity)
                bot.execute(SendMessage.builder().chatId(chatId).text("绑定网易云音乐成功").build())
            } else bot.execute(SendMessage.builder().chatId(chatId).text(result.message).build())
        }
    }

    fun xiaomiStepLogin() = callback("xiaomiStepLogin") {
        val chatId = query.message.chatId
        val userid = query.from.id
        bot.execute(SendMessage(chatId.toString(), "请发送手机号"))
        val phone = query.waitNextMessage().text
        bot.execute(SendMessage(chatId.toString(), "请发送密码"))
        val password = query.waitNextMessage().text
        val result = XiaomiStepLogic.login(phone, password)
        val message = if (result.success()) {
            val newEntity = result.data()
            val stepEntity = stepService.findByTgId(userid) ?: StepEntity().apply {
                tgId = userid
            }
            stepEntity.miLoginToken = newEntity.miLoginToken
            stepService.save(stepEntity)
            "绑定小米运动成功"
        } else result.message
        bot.execute(SendMessage(chatId.toString(), message))
    }

    fun leXinStepLogin() = callback("leXinStepLogin") {
        val chatId = query.message.chatId
        bot.execute(SendMessage(chatId.toString(), "请发送手机号"))
        val phone = query.waitNextMessage().text
        bot.execute(SendMessage(chatId.toString(), "请发送密码"))
        val password = query.waitNextMessage().text
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
        bot.execute(SendMessage(chatId.toString(), message))
    }

    fun weiboLogin() = callback("weiboLogin") {
        val chatId = query.message.chatId
        val userid = query.from.id
        bot.execute(SendMessage.builder().text("请发送账号").chatId(chatId).build())
        val account = query.waitNextMessage().text
        bot.execute(SendMessage.builder().text("请发送密码").chatId(chatId).build())
        val password = query.waitNextMessage().text
        val weiboLoginVerify = WeiboLogic.login(account, password)
        WeiboLogic.loginByPrivateMsg1(weiboLoginVerify)
        bot.execute(SendMessage.builder().text("微博需要私信验证，请打开微博app或者网页查看*微博安全中心*发送的验证码").parseMode("Markdown").chatId(chatId).build())
        val code = query.waitNextMessage(1000 * 60 * 2).text
        val newEntity = WeiboLogic.loginByPrivateMsg2(weiboLoginVerify, code)
        val weiboEntity = weiboService.findByTgId(userid) ?: WeiboEntity().also { entity -> entity.tgId = userid }
        weiboEntity.cookie = newEntity.cookie
        weiboService.save(weiboEntity)
        bot.execute(SendMessage(chatId.toString(), "绑定微博成功"))
    }

    fun douYinLogin() = callback("douYinLogin") {
        val chatId = query.message.chatId
        val userid = query.from.id
        val qrcode = DouYinLogic.qrcode()
        qrcode.baseImage.base64Decode().inputStream().use { iis ->
            val photo = SendPhoto(chatId.toString(), InputFile(iis,
            "抖音登录二维码.jpg")).apply { caption = "请使用抖音App扫码登录" }
            bot.execute(photo)
        }
        while (true) {
            val result = DouYinLogic.checkQrcode(qrcode)
            if (result.code == 200) {
                val newDouYinEntity = result.data()
                val douYinEntity = douYinService.findByTgId(userid) ?: DouYinEntity().also { entity -> entity.tgId = userid }
                douYinEntity.cookie = newDouYinEntity.cookie
                douYinEntity.userid = newDouYinEntity.userid
                douYinEntity.secUserid = newDouYinEntity.secUserid
                douYinService.save(douYinEntity)
                val sendMessage = SendMessage(chatId.toString(), "绑定抖音成功")
                bot.execute(sendMessage)
                break
            } else if (result.code == 500) {
                error(result.message)
            }
            delay(2000)
        }
    }

    fun twitterLogin() = callback {
        query("twitterLogin") {
            val chatId = query.message.chatId
            val messageId = query.message.messageId
            val loginButton = inlineKeyboardButton("模拟登录", "twitterLoginByUsername")
            val cookieButton = inlineKeyboardButton("cookie登录", "twitterCookieLogin")
            val markup = InlineKeyboardMarkup(listOf(
                listOf(loginButton),
                listOf(cookieButton),
                returnButton()
            ))
            val editMessageText = EditMessageText.builder().text("请选择twitter登录方式").chatId(chatId)
                .messageId(messageId).replyMarkup(markup).build()
            bot.execute(editMessageText)
        }
        query("twitterLoginByUsername") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            bot.execute(SendMessage(chatId.toString(), "请发送twitter的用户名"))
            val usernameMessage = query.waitNextMessage()
            val username = usernameMessage.text
            bot.execute(SendMessage(chatId.toString(), "请发送twitter的密码"))
            val passwordMessage = query.waitNextMessage()
            val password = passwordMessage.text
            try {
                val twitterEntity = TwitterLogic.login(username, password)
                val queryEntity = twitterService.findByTgId(tgId) ?: TwitterEntity().also { en -> en.tgId = tgId }
                queryEntity.cookie = twitterEntity.cookie
                queryEntity.csrf = twitterEntity.csrf
                queryEntity.tId = twitterEntity.tId
                queryEntity.tRestId = twitterEntity.tRestId
                twitterService.save(queryEntity)
                bot.execute(SendMessage(chatId.toString(), "绑定twitter成功"))
            } finally {
                val deleteMessage = DeleteMessage(chatId.toString(), usernameMessage.messageId)
                bot.execute(deleteMessage)
                val deleteMsg = DeleteMessage(chatId.toString(), passwordMessage.messageId)
                bot.execute(deleteMsg)
            }

        }
        query("twitterCookieLogin") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            bot.execute(SendMessage(chatId.toString(), "请发送twitter的cookie"))
            val nextMessage = query.waitNextMessage()
            val cookie = nextMessage.text
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
            val deleteMessage = DeleteMessage(chatId.toString(), nextMessage.messageId)
            bot.execute(deleteMessage)
            bot.execute(SendMessage(chatId.toString(), "绑定twitter成功"))
        }
    }

    fun pixivLogin() = callback {
        query("pixivLogin") {
            val chatId = query.message.chatId
            val messageId = query.message.messageId
            val loginButton = inlineKeyboardButton("使用微博app扫码登陆", "pixivLoginByWeibo")
            val cookieButton = inlineKeyboardButton("cookie登录", "pixivCookieLogin")
            val markup = InlineKeyboardMarkup(listOf(
                listOf(loginButton),
                listOf(cookieButton),
                returnButton()
            ))
            val editMessageText = EditMessageText.builder().text("请选择pixiv登录方式").chatId(chatId)
                .messageId(messageId).replyMarkup(markup).build()
            bot.execute(editMessageText)
        }
        query("pixivLoginByWeibo") {
            error("没写")
        }
        query("pixivCookieLogin") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            bot.execute(SendMessage(chatId.toString(), "请发送pixiv的cookie"))
            val nextMessage = query.waitNextMessage()
            val cookie = nextMessage.text
            PixivLogic.followImage(PixivEntity().also { ii -> ii.cookie = cookie })
            val pixivEntity = pixivService.findByTgId(tgId) ?: PixivEntity().also { ii -> ii.tgId = tgId }
            pixivEntity.cookie = cookie
            pixivService.save(pixivEntity)
            val deleteMessage = DeleteMessage(chatId.toString(), nextMessage.messageId)
            bot.execute(deleteMessage)
            bot.execute(SendMessage(chatId.toString(), "绑定pixiv成功"))
        }
    }

    fun buffLogin() = callback {
        query("buffLogin") {
            val chatId = query.message.chatId
            val messageId = query.message.messageId
            val loginButton = inlineKeyboardButton("使用手机验证码登陆", "buffLoginByPhoneCode")
            val cookieButton = inlineKeyboardButton("cookie登录", "buffLoginByCookie")
            val markup = InlineKeyboardMarkup(listOf(
                listOf(loginButton),
                listOf(cookieButton),
                returnButton()
            ))
            val editMessageText = EditMessageText.builder().text("请选择网易buff登录方式").chatId(chatId)
                .messageId(messageId).replyMarkup(markup).build()
            bot.execute(editMessageText)
        }
        query("buffLoginByPhoneCode") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            bot.execute(SendMessage.builder().text("请发送手机号").chatId(chatId).build())
            val phone = query.waitNextMessage().text
            var s = false
            for (i in 0..2) {
                s = kotlin.runCatching {
                    BuffLogic.login1(phone)
                    true
                }.getOrDefault(false)
                delay(1000)
            }
            if (!s) error("验证码识别失败，请重试")
            bot.execute(SendMessage.builder().text("请发送验证码").chatId(chatId).build())
            val code = query.waitNextMessage().text
            val buffEntity = BuffLogic.login2(phone, code)
            val saveEntity = buffService.findByTgId(tgId) ?: BuffEntity().also { entity -> entity.tgId = tgId }
            saveEntity.csrf = buffEntity.csrf
            saveEntity.cookie = buffEntity.cookie
            buffService.save(saveEntity)
            bot.execute(SendMessage.builder().text("绑定网易buff成功").chatId(chatId).build())
        }
        query("buffLoginByCookie") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            bot.execute(SendMessage(chatId.toString(), "请发送网易buff的cookie"))
            val nextMessage = query.waitNextMessage()
            val cookie = nextMessage.text
            BuffLogic.search(BuffEntity().also { en -> en.cookie = cookie }, "m9刺刀")
            val buffEntity = buffService.findByTgId(tgId) ?: BuffEntity().also { ii -> ii.tgId = tgId }
            buffEntity.cookie = cookie
            buffService.save(buffEntity)
            val deleteMessage = DeleteMessage(chatId.toString(), nextMessage.messageId)
            bot.execute(deleteMessage)
            bot.execute(SendMessage(chatId.toString(), "绑定pixiv成功"))
        }
    }

    fun CallbackSubscriber.smZdm() {
        "smZdmLogin" {
            val loginButton = inlineKeyboardButton("使用手机验证码登陆", "smZdmLoginByPhoneCode")
            val wechatQrcodeButton = inlineKeyboardButton("使用微信扫码登陆", "smZdmWechatLoginByPhoneCode")
            val appQrcodeButton = inlineKeyboardButton("使用App扫码登陆", "smZdmAppQrcodeLoginByPhoneCode")
            val cookieButton = inlineKeyboardButton("cookie登录", "smZdmLoginByCookie")
            val markup = InlineKeyboardMarkup(listOf(
                listOf(loginButton),
                listOf(wechatQrcodeButton),
                listOf(appQrcodeButton),
                listOf(cookieButton),
                returnButton()
            ))
            val editMessageText = EditMessageText.builder().text("请选择什么值得买登录方式").chatId(chatId)
                .messageId(message.messageId).replyMarkup(markup).build()
            bot.execute(editMessageText)
        }
        "smZdmLoginByPhoneCode" {
            val sendPhoneMessage = bot.execute(SendMessage.builder().text("请发送手机号码").chatId(chatId).build())
            val phoneMessage = query.waitNextMessage()
            val phone = phoneMessage.text
            SmZdmLogic.login1(phone)
            val captchaMessage = bot.execute(SendMessage.builder().text("请发送验证码").chatId(chatId).build())
            val codeMessage = query.waitNextMessage()
            val newEntity = SmZdmLogic.login2(phone, codeMessage.text)
            val smZdmEntity = smZdmService.findByTgId(tgId) ?: SmZdmEntity().also { it.tgId = tgId }
            smZdmEntity.cookie = newEntity.cookie
            smZdmService.save(smZdmEntity)
            sendPhoneMessage.delete()
            phoneMessage.delete()
            captchaMessage.delete()
            codeMessage.delete()
            bot.execute(SendMessage.builder().text("绑定什么值得买成功").chatId(chatId).build())
        }
        "smZdmLoginByCookie" {
            val sendCookieMessage = bot.execute(SendMessage.builder().text("请发送cookie").chatId(chatId).build())
            val cookieMessage = query.waitNextMessage()
            val text = cookieMessage.text
            SmZdmLogic.appSign(SmZdmEntity().also { it.cookie = text })
            val smZdmEntity = smZdmService.findByTgId(tgId) ?: SmZdmEntity().also { it.tgId = tgId }
            smZdmEntity.cookie = text
            smZdmService.save(smZdmEntity)
            sendCookieMessage.delete()
            cookieMessage.delete()
            bot.execute(SendMessage.builder().text("绑定什么值得买成功").chatId(chatId).build())
        }
        "smZdmWechatLoginByPhoneCode" {
            val wechatQrcode = SmZdmLogic.wechatQrcode1()
            val photoMessage = client.get(wechatQrcode.url).body<InputStream>().use {
                val sendPhoto = SendPhoto(chatId.toString(), InputFile(it, "smzdmWechat.jpg")).also { sp ->
                        sp.caption = "请先在网页成功使用微信扫码成功登录一次，使用微信扫码登录，如未关注公众号，扫码关注公众号后再扫一次"
                }
                bot.execute(sendPhoto)
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
                        bot.execute(SendMessage.builder().text("绑定什么值得买成功").chatId(chatId).build())
                        fail = false
                        break
                    }
                } catch (ignore: Exception) {
                }
            }
            photoMessage.delete()
            if (fail)
                bot.execute(SendMessage.builder().text("什么值得买二维码已过期").chatId(chatId).build())
        }
        "smZdmAppQrcodeLoginByPhoneCode" {
            val appQrcode = SmZdmLogic.appQrcode1()
            val url = appQrcode.url
            val photoMessage = client.get("https://api.kukuqaq.com/qrcode?text=${url.toUrlEncode()}").body<InputStream>().use {
                val sendPhoto = SendPhoto(chatId.toString(), InputFile(it, "smzdmApp.jpg")).also { sp ->
                    sp.caption = "请使用什么值得买App扫码登陆"
                }
                bot.execute(sendPhoto)
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
                    bot.execute(SendMessage.builder().text("绑定什么值得买成功").chatId(chatId).build())
                    fail = false
                    break
                }
            }
            photoMessage.delete()
            if (fail)
                bot.execute(SendMessage.builder().text("什么值得买二维码已过期").chatId(chatId).build())
        }
    }

}
