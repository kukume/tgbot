package me.kuku.telegram.extension

import kotlinx.coroutines.delay
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.*
import me.kuku.telegram.utils.*
import org.springframework.stereotype.Service
import org.telegram.abilitybots.api.util.AbilityExtension
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Service
class ExecExtension(
    private val baiduService: BaiduService,
    private val baiduLogic: BaiduLogic,
    private val biliBiliService: BiliBiliService,
    private val hostLocService: HostLocService,
    private val kuGouService: KuGouService,
    private val kuGouLogic: KuGouLogic,
    private val miHoYoService: MiHoYoService,
    private val netEaseService: NetEaseService,
    private val stepService: StepService,
    private val weiboService: WeiboService,
    private val douYuService: DouYuService,
    private val douYuLogic: DouYuLogic,
    private val smZdmService: SmZdmService,
    private val aliDriverService: AliDriverService
): AbilityExtension {

    private fun execKeyboardMarkup(): InlineKeyboardMarkup {
        val baiduButton = InlineKeyboardButton("百度").also { it.callbackData = "baiduExec" }
        val biliBiliButton = InlineKeyboardButton("哔哩哔哩").also { it.callbackData = "biliBiliExec" }
        val hostLocButton = InlineKeyboardButton("HostLoc").also { it.callbackData = "hostLocExec" }
        val kuGouButton = InlineKeyboardButton("酷狗").also { it.callbackData = "kuGouExec" }
        val miHoYoButton = InlineKeyboardButton("米忽悠").also { it.callbackData = "miHoYoExec" }
        val netEaseButton = InlineKeyboardButton("网易云音乐").also { it.callbackData = "netEaseExec" }
        val stepButton = InlineKeyboardButton("刷步数").also { it.callbackData = "stepExec" }
        val weiboButton = InlineKeyboardButton("微博").also { it.callbackData = "weiboExec" }
        val douYuButton = InlineKeyboardButton("斗鱼").also { it.callbackData = "douYuExec" }
        val smZdmButton = InlineKeyboardButton("什么值得买").also { it.callbackData = "smZdmExec" }
        val aliDriver = inlineKeyboardButton("阿里云盘", "aliDriverExec")
        return InlineKeyboardMarkup(listOf(
            listOf(baiduButton, biliBiliButton),
            listOf(hostLocButton, kuGouButton),
            listOf(miHoYoButton, netEaseButton),
            listOf(stepButton, weiboButton),
            listOf(douYuButton, smZdmButton),
            listOf(aliDriver)
        ))
    }

    fun exec() = ability("exec", "手动执行") {
        val markup = execKeyboardMarkup()
        val sendMessage = SendMessage()
        sendMessage.replyMarkup = markup
        sendMessage.chatId = chatId().toString()
        sendMessage.text = "请选择手动执行选项"
        execute(sendMessage)
    }

    fun TelegramSubscribe.baiduExec() {
        before { set(baiduService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定百度账号")) }
        callback("baiduExec") {
            val tieBaSignButton = InlineKeyboardButton("贴吧签到").apply { callbackData = "tieBaSign" }
            val ybbSignButton = InlineKeyboardButton("游帮帮加速器签到").apply { callbackData = "ybbSign" }
            val ybbWatchAdButton = InlineKeyboardButton("游帮帮加速器看广告").apply { callbackData = "ybbWatchAd" }
            val markup = InlineKeyboardMarkup(listOf(listOf(tieBaSignButton), listOf(ybbSignButton), listOf(ybbWatchAdButton)))
            editMessageText("百度", markup)
        }
        callback("tieBaSign") {
            baiduLogic.tieBaSign(firstArg())
            editMessageText("贴吧签到成功")
        }
        callback("ybbSign") {
            baiduLogic.ybbSign(firstArg())
            editMessageText("游帮帮签到成功")
        }
        callback("ybbWatchAd") {
            baiduLogic.ybbWatchAd(firstArg(), "v3")
            editMessageText("游帮帮观看广告成功")
        }
    }

    fun TelegramSubscribe.biliBiliExec() {
        before { set(biliBiliService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定哔哩哔哩账号")) }
        callback("biliBiliExec") {
            val biliBiliSignButton = InlineKeyboardButton("签到").apply { callbackData = "biliBiliSign" }
            val markup = InlineKeyboardMarkup(listOf(listOf(biliBiliSignButton)))
            editMessageText("哔哩哔哩", markup)
        }
        callback("biliBiliSign") {
            val biliBiliEntity = firstArg<BiliBiliEntity>()
            val firstRank = BiliBiliLogic.ranking()[0]
            BiliBiliLogic.report(biliBiliEntity, firstRank.aid, firstRank.cid, 300)
            BiliBiliLogic.share(biliBiliEntity, firstRank.aid)
            BiliBiliLogic.liveSign(biliBiliEntity)
            editMessageText("哔哩哔哩签到成功")
        }
    }

    fun TelegramSubscribe.hostLocExec()  {
        before { set(hostLocService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定HostLoc账号")) }
        callback("hostLocExec") {
            val hostLocSignButton = InlineKeyboardButton("签到").apply { callbackData = "hostLocSign" }
            val markup = InlineKeyboardMarkup(listOf(listOf(hostLocSignButton)))
            editMessageText("HostLoc", markup)
        }
        callback("hostLocSign") {
            val hostLocEntity = firstArg<HostLocEntity>()
            editMessageText("HostLoc签到后台进行中")
            HostLocLogic.sign(hostLocEntity.cookie)
        }
    }

    fun TelegramSubscribe.kuGouExec() {
        before { set(kuGouService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定酷狗账号")) }
        callback("kuGouExec") {
            val kuGouMusicianSignButton = InlineKeyboardButton("音乐人").apply { callbackData = "kuGouMusicianSign" }
            val kuGouListenButton = InlineKeyboardButton("概念版听歌得vip").apply { callbackData = "kuGouListen" }
            val markup = InlineKeyboardMarkup(listOf(listOf(kuGouMusicianSignButton), listOf(kuGouListenButton)))
            editMessageText("酷狗", markup)
        }
        callback("kuGouMusicianSign") {
            kuGouLogic.musicianSign(firstArg())
            editMessageText("酷狗音乐人签到成功")
        }
        callback("kuGouListen") {
            kuGouLogic.listenMusic(firstArg())
            editMessageText("酷狗听歌得vip成功")
        }
    }

    fun TelegramSubscribe.miHoYoExec() {
        before { set(miHoYoService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定米哈游账号")) }
        callback("miHoYoExec") {
            val genShinSignButton = InlineKeyboardButton("原神签到").apply { callbackData = "genShinSign" }
            val markup = InlineKeyboardMarkup(listOf(listOf(genShinSignButton)))
            editMessageText("米哈游", markup)
        }
        callback("genShinSign") {
            MiHoYoLogic.sign(firstArg())
            editMessageText("原神签到成功")
        }
    }

    fun TelegramSubscribe.netEaseExec() {
        before { set(netEaseService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定网易云音乐账号")) }
        callback("netEaseExec") {
            val netEaseSignButton = InlineKeyboardButton("签到").apply { callbackData = "netEaseSign" }
            val netEaseMusicianSignButton = InlineKeyboardButton("音乐人签到").apply { callbackData = "netEaseMusicianSign" }
            val markup = InlineKeyboardMarkup(listOf(listOf(netEaseSignButton), listOf(netEaseMusicianSignButton)))
            editMessageText("网易云音乐", markup)
        }
        callback("netEaseSign") {
            val netEaseEntity = firstArg<NetEaseEntity>()
            val result = NetEaseLogic.sign(netEaseEntity)
            val message = if (result.failure()) {
                result.message
            } else {
                delay(3000)
                NetEaseLogic.listenMusic(netEaseEntity)
                "网易云音乐签到成功"
            }
            editMessageText(message)
        }
        callback("netEaseMusicianSign") {
            val netEaseEntity = firstArg<NetEaseEntity>()
            val result = NetEaseLogic.musicianSign(netEaseEntity)
            val message = if (result.failure()) {
                result.message
            } else {
                delay(3000)
                NetEaseLogic.publish(netEaseEntity)
                delay(3000)
                NetEaseLogic.publishMLog(netEaseEntity)
                "网易云音乐人签到成功"
            }
            editMessageText(message)
        }
    }

    fun TelegramSubscribe.stepExec() {
        before { set(stepService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定任何刷步数账号")) }
        callback("stepExec") {
            val stepEntity = firstArg<StepEntity>()
            val list = mutableListOf<List<InlineKeyboardButton>>()
            if (stepEntity.leXinCookie.isNotEmpty()) {
                list.add(listOf(InlineKeyboardButton("乐心运动刷步数").apply { callbackData = "leXinStepExec" }))
            }
            if (stepEntity.miLoginToken.isNotEmpty()) {
                list.add(listOf(InlineKeyboardButton("小米运动刷步数").apply { callbackData = "xiaomiStepExec" }))
            }
            val markup = InlineKeyboardMarkup(list)
            editMessageText("刷步数", markup)
        }
        callback("leXinStepExec") {
            editMessageText("请发送乐心运动下需要刷的步数")
            val step = nextMessage().text.toIntOrNull() ?: error("步数不为数字")
            val res = LeXinStepLogic.modifyStepCount(firstArg(), step)
            editMessageText(res.message)
        }
        callback("xiaomiStepExec") {
            editMessageText("请发送小米运动下需要刷的步数")
            val step = nextMessage().text.toIntOrNull() ?: error("步数不为数字")
            val res = XiaomiStepLogic.modifyStepCount(firstArg(), step)
            editMessageText(res.message)
        }
    }

    fun TelegramSubscribe.weiboExec() {
        before { set(weiboService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定微博账号")) }
        callback("weiboExec") {
            val superTalkSignButton = InlineKeyboardButton("超话签到").apply { callbackData = "superTalkSign" }
            val markup = InlineKeyboardMarkup(listOf(listOf(superTalkSignButton)))
            editMessageText("微博", markup)
        }
        callback("superTalkSign") {
            val result = WeiboLogic.superTalkSign(firstArg())
            editMessageText(result.message)
        }
    }

    fun TelegramSubscribe.douYuExec() {
        before { set(douYuService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定斗鱼账号")) }
        callback("douYuExec") {
            val fishGroupSignButton = inlineKeyboardButton("鱼吧签到", "fishGroupSign")
            val appSignButton = inlineKeyboardButton("app签到", "douYuAppSign")
            val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(listOf(fishGroupSignButton), listOf(appSignButton)))
            editMessageText("斗鱼", inlineKeyboardMarkup)
        }
        callback("fishGroupSign") {
            val douYuEntity = firstArg<DouYuEntity>()
            if (douYuEntity.cookie.isEmpty()) error("未扫码登录斗鱼，无法执行鱼吧签到")
            douYuLogic.fishGroup(douYuEntity)
            editMessageText("斗鱼鱼吧签到成功")
        }
        callback("douYuAppSign") {
            val douYuEntity = firstArg<DouYuEntity>()
            if (douYuEntity.appCookie.isEmpty()) error("未使用cookie登录斗鱼，无法执行app签到")
            douYuLogic.appSign(douYuEntity)
            editMessageText("斗鱼app签到成功")
        }

    }

    fun TelegramSubscribe.smZdm() {
        before { set(smZdmService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定什么值得买账号")) }
        callback("smZdmExec") {
            val signButton = inlineKeyboardButton("签到", "smZdmSign")
            val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(listOf(signButton)))
            editMessageText("什么值得买", inlineKeyboardMarkup)
        }
        callback("smZdmSign") {
            val smZdmEntity = firstArg<SmZdmEntity>()
            SmZdmLogic.webSign(smZdmEntity)
            SmZdmLogic.appSign(smZdmEntity)
            editMessageText("什么值得买app与网页签到成功")
        }
    }

    fun TelegramSubscribe.aliDriver() {
        before { set(aliDriverService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定阿里云盘账号")) }
        callback("aliDriverExec") {
            val signButton = inlineKeyboardButton("签到", "aliDriverSign")
            val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(listOf(signButton)))
            editMessageText("阿里云盘", inlineKeyboardMarkup)
        }
        callback("aliDriverSign") {
            val entity: AliDriverEntity = firstArg()
            val res = AliDriverLogic.sign(entity)
            editMessageText(res)
        }
    }


}
