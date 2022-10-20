@file:Suppress("SpellCheckingInspection")

package me.kuku.telegram.extension

import kotlinx.coroutines.delay
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.*
import me.kuku.telegram.utils.*
import me.kuku.utils.OkHttpKtUtils
import me.kuku.utils.OkUtils
import me.kuku.utils.base64Decode
import me.kuku.utils.toUrlEncode
import org.springframework.stereotype.Service
import org.telegram.abilitybots.api.util.AbilityExtension
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

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
    private val pixivService: PixivService
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
        return InlineKeyboardMarkup(listOf(
            listOf(baiduButton, biliBiliButton),
            listOf(douYuButton, hostLocButton),
            listOf(huYaButton, kuGouButton),
            listOf(miHoYoButton, netEaseButton),
            listOf(xiaomiStepButton, leXinStepButton),
            listOf(weiboStepButton, douYinButton),
            listOf(twitterButton, pixivButton)
        ))
    }

    private fun returnButton(): List<InlineKeyboardButton> {
        return listOf(inlineKeyboardButton("返回", "returnLogin"))
    }

    fun returnLogin() = callback("returnLogin") {
        val chatId = it.message.chatId
        val messageId = it.message.messageId
        val editMessageText = EditMessageText.builder().chatId(chatId).messageId(messageId).text("请选择登录选项")
            .replyMarkup(loginKeyboardMarkup()).build()
        execute(editMessageText)
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
            val photo = SendPhoto(it.message.chatId.toString(), InputFile(iis, "百度登录二维码.jpg"))
                .apply { caption = "请使用百度app扫码登陆，百度网盘等均可" }
            execute(photo)
        }
        val baiduEntity = baiduService.findByTgId(it.from.id) ?: BaiduEntity().apply {
            tgId = it.from.id
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
                    val sendMessage = SendMessage(it.message.chatId.toString(), "绑定百度成功")
                    execute(sendMessage)
                }
            } catch (ignore: Exception) {}
        }
    }

    fun biliBiliLogin() = callback("biliBiliLogin") {
        val qrCodeUrl = BiliBiliLogic.loginByQr1()
        val photoMessage = OkHttpKtUtils.getByteStream("https://api.kukuqaq.com/qrcode?text=${qrCodeUrl.toUrlEncode()}").use { iim ->
            val photo = SendPhoto(it.message.chatId.toString(), InputFile(iim, "哔哩哔哩登录二维码.jpg"))
                .apply { caption = "请使用哔哩哔哩app扫码登陆" }
            execute(photo)
        }
        while (true) {
            delay(3000)
            val result = BiliBiliLogic.loginByQr2(qrCodeUrl)
            when (result.code) {
                0 -> continue
                200 -> {
                    val newEntity = result.data()
                    val biliBiliEntity = biliBiliService.findByTgId(it.from.id) ?: BiliBiliEntity().also { entity ->
                        entity.tgId = it.from.id
                    }
                    biliBiliEntity.cookie = newEntity.cookie
                    biliBiliEntity.userid = newEntity.userid
                    biliBiliEntity.token = newEntity.token
                    biliBiliService.save(biliBiliEntity)
                    val message = SendMessage().also { age ->
                        age.chatId = it.message.chatId.toString()
                        age.text = "绑定哔哩哔哩成功"
                    }
                    val titleMessage = execute(message)
                    delay(1000 * 10)
                    execute(DeleteMessage(it.message.chatId.toString(), photoMessage.messageId))
                    execute(DeleteMessage(it.message.chatId.toString(), titleMessage.messageId))
                    break
                }
                else -> {
                    val message = SendMessage().also { age ->
                        age.chatId = it.message.chatId.toString()
                        age.text = result.message
                    }
                    val titleMessage = execute(message)
                    delay(1000 * 10)
                    execute(DeleteMessage(it.message.chatId.toString(), photoMessage.messageId))
                    execute(DeleteMessage(it.message.chatId.toString(), titleMessage.messageId))
                    break
                }
            }
        }
    }

    fun douYuLogin() = callback("douYuLogin") {
        val qrcode = douYuLogic.getQrcode()
        val imageBase = qrcode.qqLoginQrcode.imageBase
        imageBase.base64Decode().inputStream().use { iim ->
            val photo = SendPhoto(it.message.chatId.toString(), InputFile(iim,
                "斗鱼登录二维码.jpg")).apply { caption = "请使用斗鱼绑定qq，然后使用qq扫码登录" }
            execute(photo)
        }
        while (true) {
            delay(3000)
            val result = douYuLogic.checkQrcode(qrcode)
            when (result.code) {
                0 -> continue
                200 -> {
                    val newEntity = result.data()
                    val douYuEntity = douYuService.findByTgId(it.from.id) ?: DouYuEntity().apply {
                        tgId = it.from.id
                    }
                    douYuEntity.cookie = newEntity.cookie
                    douYuService.save(douYuEntity)
                    val sendMessage = SendMessage(it.message.chatId.toString(), "绑定斗鱼成功")
                    execute(sendMessage)
                    break
                }
                else -> {
                    val sendMessage = SendMessage(it.message.chatId.toString(), result.message)
                    execute(sendMessage)
                    break
                }
            }
        }
    }

    fun hostLocLogin() = callback("hostLocLogin") {
        val chatId = it.message.chatId
        val userid = it.from.id
        val accountSendMessage = SendMessage(chatId.toString(), "请发送账号")
        execute(accountSendMessage)
        val account = it.waitNextMessage().text
        val passwordSendMessage = SendMessage(chatId.toString(), "请发送密码")
        execute(passwordSendMessage)
        val password = it.waitNextMessage().text
        val cookie = HostLocLogic.login(account, password)
        val hostLocEntity = hostLocService.findByTgId(userid) ?: HostLocEntity().apply { tgId = userid }
        hostLocEntity.cookie = cookie
        hostLocService.save(hostLocEntity)
        val sendMessage = SendMessage(chatId.toString(), "绑定HostLoc成功")
        execute(sendMessage)
    }

    fun huYaLogin() = callback("huYaLogin") {
        val chatId = it.message.chatId
        val userid = it.from.id
        val qrcode = huYaLogic.getQrcode()
        OkHttpKtUtils.getByteStream(qrcode.url).use { iim ->
            val photo = SendPhoto(it.message.chatId.toString(), InputFile(iim,
                "虎牙登录二维码.jpg")).apply { caption = "请使用虎牙App扫码登录" }
            execute(photo)
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
                    execute(sendMessage)
                    break
                }
                else -> {
                    val sendMessage = SendMessage(chatId.toString(), result.message)
                    execute(sendMessage)
                    break
                }
            }
        }
    }

    fun kuGouLogin() = callback("kuGouLogin") {
        val chatId = it.message.chatId
        val userid = it.from.id
        execute(SendMessage(chatId.toString(), "请发送手机号"))
        val phone = it.waitNextMessage().text.toLongOrNull() ?: return@callback kotlin.run {
            execute(SendMessage(chatId.toString(), "发送的手机号有误"))
        }
        val kuGouEntity = kuGouService.findByTgId(userid) ?: KuGouEntity().apply {
            mid = kuGouLogic.mid()
            tgId = userid
        }
        val mid = kuGouEntity.mid
        val result = kuGouLogic.sendMobileCode(phone.toString(), mid)
        val message = if (result.success()) {
            execute(SendMessage(chatId.toString(), "请发送短信验证码"))
            val code = it.waitNextMessage(1000 * 60 * 2).text
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
        execute(SendMessage(chatId.toString(), message))
    }

    fun miHoYoLogin() = callback("miHoYoLogin") {
        val chatId = it.message.chatId
        val userid = it.from.id
        execute(SendMessage(chatId.toString(), "请发送米哈游的cookie"))
        val cookie = it.waitNextMessage().text
        val newEntity = miHoYoService.findByTgId(userid) ?: MiHoYoEntity().apply {
            tgId = userid
        }
        newEntity.cookie = cookie
        miHoYoService.save(newEntity)
        execute(SendMessage(chatId.toString(), "绑定米哈游成功"))
    }

    fun netEaseLogin() = callback("netEaseLogin") {
        val chatId = it.message.chatId
        val userid = it.from.id
        val key = NetEaseLogic.qrcode()
        val url = "http://music.163.com/login?codekey=$key"
        val newUrl =
            "https://api.kukuqaq.com/qrcode?text=${url.toUrlEncode()}"
        OkHttpKtUtils.getByteStream(newUrl).use { iis ->
            val photo = SendPhoto(it.message.chatId.toString(), InputFile(iis,
                "网易云音乐登录二维码.jpg")).apply { caption = "请使用网易云音乐App扫码登录" }
            execute(photo)
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
                    execute(SendMessage(chatId.toString(), "绑定网易云音乐成功"))
                    break
                }
                500 -> {
                    execute(SendMessage(chatId.toString(), result.message))
                    break
                }
                1 -> {
                    if (scan) {
                        execute(SendMessage(chatId.toString(), result.message))
                        scan = false
                    }
                }
            }
        }
    }

    fun xiaomiStepLogin() = callback("xiaomiStepLogin") {
        val chatId = it.message.chatId
        val userid = it.from.id
        execute(SendMessage(chatId.toString(), "请发送手机号"))
        val phone = it.waitNextMessage().text
        execute(SendMessage(chatId.toString(), "请发送密码"))
        val password = it.waitNextMessage().text
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
        execute(SendMessage(chatId.toString(), message))
    }

    fun leXinStepLogin() = callback("leXinStepLogin") {
        val chatId = it.message.chatId
        execute(SendMessage(chatId.toString(), "请发送手机号"))
        val phone = it.waitNextMessage().text
        execute(SendMessage(chatId.toString(), "请发送密码"))
        val password = it.waitNextMessage().text
        val result = LeXinStepLogic.login(phone, password)
        val message = if (result.success()) {
            val newStepEntity = result.data()
            val stepEntity = stepService.findByTgId(it.from.id) ?: StepEntity().apply {
                tgId = it.from.id
            }
            stepEntity.leXinCookie = newStepEntity.leXinCookie
            stepEntity.leXinUserid = newStepEntity.leXinUserid
            stepEntity.leXinAccessToken = newStepEntity.leXinAccessToken
            stepService.save(stepEntity)
            "绑定乐心运动成功"
        } else result.message
        execute(SendMessage(chatId.toString(), message))
    }

    fun weiboLogin() = callback("weiboLogin") {
        val chatId = it.message.chatId
        val userid = it.from.id
        execute(SendMessage.builder().text("请发送账号").chatId(chatId).build())
        val account = it.waitNextMessage().text
        execute(SendMessage.builder().text("请发送密码").chatId(chatId).build())
        val password = it.waitNextMessage().text
        val weiboLoginVerify = WeiboLogic.login(account, password)
        WeiboLogic.loginByPrivateMsg1(weiboLoginVerify)
        execute(SendMessage.builder().text("微博需要私信验证，请打开微博app或者网页查看*微博安全中心*发送的验证码").parseMode("Markdown").chatId(chatId).build())
        val code = it.waitNextMessage(1000 * 60 * 2).text
        val newEntity = WeiboLogic.loginByPrivateMsg2(weiboLoginVerify, code)
        val weiboEntity = weiboService.findByTgId(userid) ?: WeiboEntity().also { entity -> entity.tgId = userid }
        weiboEntity.cookie = newEntity.cookie
        weiboService.save(weiboEntity)
        execute(SendMessage(chatId.toString(), "绑定微博成功"))
    }

    fun douYinLogin() = callback("douYinLogin") {
        val chatId = it.message.chatId
        val userid = it.from.id
        val qrcode = DouYinLogic.qrcode()
        qrcode.baseImage.base64Decode().inputStream().use { iis ->
            val photo = SendPhoto(chatId.toString(), InputFile(iis,
            "抖音登录二维码.jpg")).apply { caption = "请使用抖音App扫码登录" }
            execute(photo)
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
                execute(sendMessage)
                break
            } else if (result.code == 500) {
                error(result.message)
            }
            delay(2000)
        }
    }

    fun twitterLogin() = callback {
        query("twitterLogin") {
            val chatId = it.message.chatId
            val messageId = it.message.messageId
            val loginButton = inlineKeyboardButton("模拟登录", "twitterLoginByUsername")
            val cookieButton = inlineKeyboardButton("cookie登录", "twitterCookieLogin")
            val markup = InlineKeyboardMarkup(listOf(
                listOf(loginButton),
                listOf(cookieButton),
                returnButton()
            ))
            val editMessageText = EditMessageText.builder().text("请选择twitter登录方式").chatId(chatId)
                .messageId(messageId).replyMarkup(markup).build()
            execute(editMessageText)
        }
        query("twitterLoginByUsername") {
            val chatId = it.message.chatId
            val tgId = it.from.id
            execute(SendMessage(chatId.toString(), "请发送twitter的用户名"))
            val usernameMessage = it.waitNextMessage()
            val username = usernameMessage.text
            execute(SendMessage(chatId.toString(), "请发送twitter的密码"))
            val passwordMessage = it.waitNextMessage()
            val password = passwordMessage.text
            try {
                val twitterEntity = TwitterLogic.login(username, password)
                val queryEntity = twitterService.findByTgId(tgId) ?: TwitterEntity().also { en -> en.tgId = tgId }
                queryEntity.cookie = twitterEntity.cookie
                queryEntity.csrf = twitterEntity.csrf
                queryEntity.tId = twitterEntity.tId
                queryEntity.tRestId = twitterEntity.tRestId
                twitterService.save(queryEntity)
                execute(SendMessage(chatId.toString(), "绑定twitter成功"))
            } finally {
                val deleteMessage = DeleteMessage(chatId.toString(), usernameMessage.messageId)
                execute(deleteMessage)
                val deleteMsg = DeleteMessage(chatId.toString(), passwordMessage.messageId)
                execute(deleteMsg)
            }

        }
        query("twitterCookieLogin") {
            val chatId = it.message.chatId
            val tgId = it.from.id
            execute(SendMessage(chatId.toString(), "请发送twitter的cookie"))
            val nextMessage = it.waitNextMessage()
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
            execute(deleteMessage)
            execute(SendMessage(chatId.toString(), "绑定twitter成功"))
        }
    }

    fun pixivLogin() = callback {
        query("pixivLogin") {
            val chatId = it.message.chatId
            val messageId = it.message.messageId
            val loginButton = inlineKeyboardButton("使用微博app扫码登陆", "pixivLoginByWeibo")
            val cookieButton = inlineKeyboardButton("cookie登录", "pixivCookieLogin")
            val markup = InlineKeyboardMarkup(listOf(
                listOf(loginButton),
                listOf(cookieButton),
                returnButton()
            ))
            val editMessageText = EditMessageText.builder().text("请选择pixiv登录方式").chatId(chatId)
                .messageId(messageId).replyMarkup(markup).build()
            execute(editMessageText)
        }
        query("pixivLoginByWeibo") {
            error("没写")
        }
        query("pixivCookieLogin") {
            val chatId = it.message.chatId
            val tgId = it.from.id
            execute(SendMessage(chatId.toString(), "请发送pixiv的cookie"))
            val nextMessage = it.waitNextMessage()
            val cookie = nextMessage.text
            PixivLogic.followImage(PixivEntity().also { ii -> ii.cookie = cookie })
            val pixivEntity = pixivService.findByTgId(tgId) ?: PixivEntity().also { ii -> ii.tgId = tgId }
            pixivEntity.cookie = cookie
            pixivService.save(pixivEntity)
            val deleteMessage = DeleteMessage(chatId.toString(), nextMessage.messageId)
            execute(deleteMessage)
            execute(SendMessage(chatId.toString(), "绑定pixiv成功"))
        }
    }

}