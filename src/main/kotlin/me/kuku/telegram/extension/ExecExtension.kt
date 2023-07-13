package me.kuku.telegram.extension

import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import kotlinx.coroutines.delay
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.*
import me.kuku.telegram.utils.*
import org.springframework.stereotype.Service

@Service
class ExecExtension(
    private val configService: ConfigService,
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
    private val smZdmLogic: SmZdmLogic,
    private val aliDriverService: AliDriverService,
    private val nodeSeekService: NodeSeekService,
    private val glaDosService: GlaDosService
) {

    private fun execKeyboardMarkup(): InlineKeyboardMarkup {
        val baiduButton = InlineKeyboardButton("百度").callbackData("baiduExec")
        val biliBiliButton = InlineKeyboardButton("哔哩哔哩").callbackData("biliBiliExec")
        val hostLocButton = InlineKeyboardButton("HostLoc").callbackData("hostLocExec")
        val kuGouButton = InlineKeyboardButton("酷狗").callbackData("kuGouExec")
        val miHoYoButton = InlineKeyboardButton("米忽悠").callbackData("miHoYoExec")
        val netEaseButton = InlineKeyboardButton("网易云音乐").callbackData("netEaseExec")
        val stepButton = InlineKeyboardButton("刷步数").callbackData("stepExec")
        val weiboButton = InlineKeyboardButton("微博").callbackData("weiboExec")
        val douYuButton = InlineKeyboardButton("斗鱼").callbackData("douYuExec")
        val smZdmButton = InlineKeyboardButton("什么值得买").callbackData("smZdmExec")
        val aliDriver = inlineKeyboardButton("阿里云盘", "aliDriverExec")
        val nodeSeek = inlineKeyboardButton("NodeSeek", "nodeSeekExec")
        val glaDos = inlineKeyboardButton("GlaDos", "glaDosExec")
        return InlineKeyboardMarkup(
            arrayOf(baiduButton, biliBiliButton),
            arrayOf(hostLocButton, kuGouButton),
            arrayOf(miHoYoButton, netEaseButton),
            arrayOf(stepButton, weiboButton),
            arrayOf(douYuButton, smZdmButton),
            arrayOf(aliDriver, nodeSeek),
            arrayOf(glaDos)
        )
    }

    fun AbilitySubscriber.exec() {
        sub("exec") {
            val markup = execKeyboardMarkup()
            sendMessage("请选择手动执行选项", markup)
        }
    }

    fun TelegramSubscribe.baiduExec() {
        before { set(baiduService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定百度账号")) }
        callback("baiduExec") {
            val tieBaSignButton = InlineKeyboardButton("贴吧签到").callbackData("tieBaSign")
            val ybbSignButton = InlineKeyboardButton("游帮帮加速器签到").callbackData("ybbSign")
            val ybbWatchAdButton = InlineKeyboardButton("游帮帮加速器看广告").callbackData("ybbWatchAd")
            val markup = InlineKeyboardMarkup(arrayOf(tieBaSignButton), arrayOf(ybbSignButton), arrayOf(ybbWatchAdButton))
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
            val biliBiliSignButton = InlineKeyboardButton("签到").callbackData("biliBiliSign")
            val markup = InlineKeyboardMarkup(arrayOf(biliBiliSignButton))
            editMessageText("哔哩哔哩\n如出现请求失败为cookie失效，请重新登陆哔哩哔哩", markup)
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
            val hostLocSignButton = InlineKeyboardButton("签到").callbackData("hostLocSign")
            val markup = InlineKeyboardMarkup(arrayOf(hostLocSignButton))
            editMessageText("HostLoc", markup)
        }
        callback("hostLocSign") {
            val hostLocEntity = firstArg<HostLocEntity>()
            for (i in 0 until 12) {
                editMessageText("正在进行第${i}次访问HostLoc用户主页")
                HostLocLogic.singleSign(hostLocEntity.cookie)
            }
        }
    }

    fun TelegramSubscribe.kuGouExec() {
        before { set(kuGouService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定酷狗账号")) }
        callback("kuGouExec") {
            val kuGouMusicianSignButton = InlineKeyboardButton("音乐人").callbackData("kuGouMusicianSign")
            val kuGouListenButton = InlineKeyboardButton("概念版听歌得vip").callbackData("kuGouListen")
            val markup = InlineKeyboardMarkup(arrayOf(kuGouMusicianSignButton), arrayOf(kuGouListenButton))
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
            val genShinSignButton = InlineKeyboardButton("原神签到").callbackData("genShinSign")
            val markup = InlineKeyboardMarkup(arrayOf(genShinSignButton))
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
            val netEaseSignButton = InlineKeyboardButton("签到").callbackData("netEaseSign")
            val netEaseMusicianSignButton = InlineKeyboardButton("音乐人签到").callbackData("netEaseMusicianSign")
            val netEaseMusicianMyComment = InlineKeyboardButton("发布主创说").callbackData("netEaseMusicianMyComment")
            val markup = InlineKeyboardMarkup(
                arrayOf(netEaseSignButton),
                arrayOf(netEaseMusicianSignButton),
                arrayOf(netEaseMusicianMyComment)
            )
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
                delay(3000)
                NetEaseLogic.myMusicComment(netEaseEntity)
                "网易云音乐人签到成功"
            }
            editMessageText(message)
        }
        callback("netEaseMusicianMyComment") {
            NetEaseLogic.myMusicComment(firstArg())
            editMessageText("发布主创说成功")
        }
    }

    fun TelegramSubscribe.stepExec() {
        before { set(stepService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定任何刷步数账号")) }
        callback("stepExec") {
            val stepEntity = firstArg<StepEntity>()
            val list = mutableListOf<Array<InlineKeyboardButton>>()
            if (stepEntity.leXinCookie.isNotEmpty()) {
                list.add(arrayOf(InlineKeyboardButton("乐心运动刷步数").callbackData("leXinStepExec")))
            }
            if (stepEntity.miLoginToken.isNotEmpty()) {
                list.add(arrayOf(InlineKeyboardButton("小米运动刷步数").callbackData("xiaomiStepExec")))
            }
            val markup = InlineKeyboardMarkup(*list.toTypedArray())
            editMessageText("刷步数", markup)
        }
        callback("leXinStepExec") {
            editMessageText("请发送乐心运动下需要刷的步数")
            val step = nextMessage().text().toIntOrNull() ?: error("步数不为数字")
            val res = LeXinStepLogic.modifyStepCount(firstArg(), step)
            editMessageText(res.message)
        }
        callback("xiaomiStepExec") {
            editMessageText("请发送小米运动下需要刷的步数")
            val step = nextMessage().text().toIntOrNull() ?: error("步数不为数字")
            val res = XiaomiStepLogic.modifyStepCount(firstArg(), step)
            editMessageText(res.message)
        }
    }

    fun TelegramSubscribe.weiboExec() {
        before { set(weiboService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定微博账号")) }
        callback("weiboExec") {
            val superTalkSignButton = InlineKeyboardButton("超话签到").callbackData("superTalkSign")
            val markup = InlineKeyboardMarkup(arrayOf(superTalkSignButton))
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
            val inlineKeyboardMarkup = InlineKeyboardMarkup(arrayOf(fishGroupSignButton), arrayOf(appSignButton))
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
            val inlineKeyboardMarkup = InlineKeyboardMarkup(arrayOf(signButton))
            editMessageText("什么值得买", inlineKeyboardMarkup)
        }
        callback("smZdmSign") {
            val smZdmEntity = firstArg<SmZdmEntity>()
            smZdmLogic.webSign(smZdmEntity, configService.findByTgId(tgId)?.rrOcrKey())
            smZdmLogic.appSign(smZdmEntity)
            editMessageText("什么值得买app与网页签到成功")
        }
    }

    fun TelegramSubscribe.aliDriver() {
        before { set(aliDriverService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定阿里云盘账号")) }
        callback("aliDriverExec") {
            val signButton = inlineKeyboardButton("签到", "aliDriverSign")
            val receive = inlineKeyboardButton("领取", "aliDriverReceive")
            val inlineKeyboardMarkup = InlineKeyboardMarkup(arrayOf(signButton), arrayOf(receive))
            editMessageText("阿里云盘", inlineKeyboardMarkup)
        }
        callback("aliDriverSign") {
            val entity: AliDriverEntity = firstArg()
            val res = AliDriverLogic.sign(entity)
            editMessageText(res.customMessage)
        }
        callback("aliDriverReceive") {
            editMessageText("请发送领取哪天的奖励")
            val day = nextMessage().text().toIntOrNull() ?: error("错误，不为数字")
            val result = AliDriverLogic.receive(firstArg(), day)
            editMessageText(result)
        }
    }

    fun TelegramSubscribe.nodeSeekExec() {
        before { set(nodeSeekService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定NodeSeek账号")) }
        callback("nodeSeekExec") {
            editMessageText("NodeSeek", InlineKeyboardMarkup(
                arrayOf(
                    inlineKeyboardButton("签到-随机鸡腿", "nodeSeekSign-1"),
                    inlineKeyboardButton("签到-固定鸡腿", "nodeSeekSign-2")
                )
            ))
        }
        callbackStartsWith("nodeSeekSign-") {
            val random = query.data().split("-")[1].toInt() == 1
            val num = NodeSeekLogic.sign(firstArg(), random)
            editMessageText("NodeSeek签到成功，获得鸡腿${num}个")
        }
    }

    fun TelegramSubscribe.glaDosExec() {
        before { set(glaDosService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定GlaDos账号")) }
        callback("glaDosExec") {
            editMessageText("GlaDos", InlineKeyboardMarkup(
                arrayOf(
                    inlineKeyboardButton("签到", "glaDosSign"),
                )
            ))
        }
        callback("glaDosSign") {
            val message = GlaDosLogic.sign(firstArg())
            editMessageText(message)
        }
    }


}
