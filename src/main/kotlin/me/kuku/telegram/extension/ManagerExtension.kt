@file:Suppress("SpellCheckingInspection")

package me.kuku.telegram.extension

import me.kuku.telegram.entity.*
import me.kuku.telegram.utils.*
import org.springframework.stereotype.Service
import org.telegram.abilitybots.api.util.AbilityExtension
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Service
class ManagerExtension(
    private val baiduService: BaiduService,
    private val biliBiliService: BiliBiliService,
    private val douYuService: DouYuService,
    private val hostLocService: HostLocService,
    private val huYaService: HuYaService,
    private val kuGouService: KuGouService,
    private val miHoYoService: MiHoYoService,
    private val netEaseService: NetEaseService,
    private val stepService: StepService,
    private val weiboService: WeiboService,
    private val twitterService: TwitterService,
    private val pixivService: PixivService,
    private val douYinService: DouYinService,
    private val smZdmService: SmZdmService,
    private val aliDriverService: AliDriverService,
    private val leiShenService: LeiShenService
): AbilityExtension {

    private fun managerKeyboardMarkup(): InlineKeyboardMarkup {
        val baiduButton = InlineKeyboardButton("百度").also { it.callbackData = "baiduManager" }
        val biliBiliButton = InlineKeyboardButton("哔哩哔哩").also { it.callbackData = "biliBiliManager" }
        val douYuButton = InlineKeyboardButton("斗鱼").also { it.callbackData = "douYuManager" }
        val hostLocButton = InlineKeyboardButton("HostLoc").also { it.callbackData = "hostLocManager" }
        val huYaButton = InlineKeyboardButton("虎牙").also { it.callbackData = "huYaManager" }
        val kuGouButton = InlineKeyboardButton("酷狗").also { it.callbackData = "kuGouManager" }
        val miHoYoButton = InlineKeyboardButton("米忽悠").also { it.callbackData = "miHoYoManager" }
        val netEaseButton = InlineKeyboardButton("网易云音乐").also { it.callbackData = "netEaseManager" }
        val xiaomiStepButton = InlineKeyboardButton("刷步数").also { it.callbackData = "stepManager" }
        val weiboButton = InlineKeyboardButton("微博").also { it.callbackData = "weiboManager" }
        val twitterButton = InlineKeyboardButton("twitter").also { it.callbackData = "twitterManager" }
        val pixivButton = InlineKeyboardButton("pixiv").also { it.callbackData = "pixivManager" }
        val douYinButton = InlineKeyboardButton("抖音").also { it.callbackData = "douYinManager" }
        val smZdmButton = InlineKeyboardButton("什么值得买").also { it.callbackData = "smZdmManager" }
        val aliDriver = inlineKeyboardButton("阿里云盘", "aliDriverManager")
        val leiShen = inlineKeyboardButton("雷神加速器", "leiShenManager")
        return InlineKeyboardMarkup(listOf(
            listOf(baiduButton, biliBiliButton),
            listOf(douYuButton, hostLocButton),
            listOf(huYaButton, kuGouButton),
            listOf(miHoYoButton, netEaseButton),
            listOf(xiaomiStepButton, weiboButton),
            listOf(twitterButton, pixivButton),
            listOf(douYinButton, smZdmButton),
            listOf(aliDriver, leiShen)
        ))
    }

    fun AbilitySubscriber.manager() {
        sub("manager", "管理") {
            val markup = managerKeyboardMarkup()
            sendMessage("请选择管理选项", markup)
        }
    }

    fun TelegramSubscribe.baiduManager() {
        before { set(baiduService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定百度账号")) }
        callback("baiduManager") {}
        callback("baiduSignOpen") { firstArg<BaiduEntity>().sign = Status.ON }
        callback("baiduSignClose") { firstArg<BaiduEntity>().sign = Status.OFF }
        after {
            val baiduEntity = firstArg<BaiduEntity>()
            baiduService.save(baiduEntity)
            val signOpenButton = inlineKeyboardButton("自动签到（开）", "baiduSignOpen")
            val signCloseButton = inlineKeyboardButton("自动签到（关）", "baiduSignClose")
            val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
                listOf(signOpenButton, signCloseButton)
            ))
            editMessageText("""
                百度自动签到管理，当前状态：
                自动签到：${baiduEntity.sign.str()}
            """.trimIndent(), inlineKeyboardMarkup, top = true)
        }
    }

    fun TelegramSubscribe.biliBiliManager() {
        before { set(biliBiliService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定哔哩哔哩账号")) }
        callback("biliBiliManager") {}
        callback("biliBiliPushOpen") { firstArg<BiliBiliEntity>().push = Status.ON }
        callback("biliBiliPushClose") { firstArg<BiliBiliEntity>().push = Status.OFF }
        callback("biliBiliSignOpen") { firstArg<BiliBiliEntity>().sign = Status.ON }
        callback("biliBiliSignClose") { firstArg<BiliBiliEntity>().sign = Status.OFF }
        callback("biliBiliLiveOpen") { firstArg<BiliBiliEntity>().live = Status.ON }
        callback("biliBiliLiveClose") { firstArg<BiliBiliEntity>().live = Status.OFF }
        after {
            val biliBiliEntity = firstArg<BiliBiliEntity>()
            val pushOpenButton = InlineKeyboardButton("动态推送（开）").apply { callbackData = "biliBiliPushOpen" }
            val pushCloseButton = InlineKeyboardButton("动态推送（关）").apply { callbackData = "biliBiliPushClose" }
            val signOpenButton = InlineKeyboardButton("自动签到（开）").apply { callbackData = "biliBiliSignOpen" }
            val signCloseButton = InlineKeyboardButton("自动签到（关）").apply { callbackData = "biliBiliSignClose" }
            val liveOpenButton = InlineKeyboardButton("开播提醒（开）").apply { callbackData = "biliBiliLiveOpen" }
            val liveCloseButton = InlineKeyboardButton("开播提醒（关）").apply { callbackData = "biliBiliLiveClose" }
            val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
                listOf(pushOpenButton, pushCloseButton),
                listOf(signOpenButton, signCloseButton),
                listOf(liveOpenButton, liveCloseButton)
            ))
            editMessageText("""
                哔哩哔哩自动签到管理，当前状态：
                动态推送：${biliBiliEntity.push.str()}
                自动签到：${biliBiliEntity.sign.str()}
                开播提醒：${biliBiliEntity.live.str()}
            """.trimIndent(), inlineKeyboardMarkup, top = true)
        }
    }

    fun TelegramSubscribe.douYuManager() {
        before { set(douYuService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定斗鱼账号")) }
        callback("douYuManager") {}
        callback("douYuLiveOpen") { firstArg<DouYuEntity>().live = Status.ON }
        callback("douYuLiveClose") { firstArg<DouYuEntity>().live = Status.OFF }
        callback("douYuFishOpen") { firstArg<DouYuEntity>().fishGroup = Status.ON }
        callback("douYuFishClose") { firstArg<DouYuEntity>().fishGroup = Status.OFF }
        callback("douYuFishPushOpen") { firstArg<DouYuEntity>().push = Status.ON }
        callback("douYuFishPushClose") { firstArg<DouYuEntity>().push = Status.OFF }
        callback("douYuAppSignOpen") { firstArg<DouYuEntity>().appSign = Status.ON }
        callback("douYuAppSignClose") { firstArg<DouYuEntity>().appSign = Status.OFF }
        callback("douYuTitleChangeOpen") { firstArg<DouYuEntity>().titleChange = Status.ON }
        callback("douYuTitleChangeClose") { firstArg<DouYuEntity>().titleChange = Status.OFF }
        after {
            val douYuEntity = firstArg<DouYuEntity>()
            douYuService.save(douYuEntity)
            val liveOpenButton = InlineKeyboardButton("开播提醒（开）").apply { callbackData = "douYuLiveOpen" }
            val liveCloseButton = InlineKeyboardButton("开播提醒（关）").apply { callbackData = "douYuLiveClose" }
            val fishOpenButton = InlineKeyboardButton("鱼吧签到（开）").apply { callbackData = "douYuFishOpen" }
            val fishCloseButton = InlineKeyboardButton("鱼吧签到（关）").apply { callbackData = "douYuFishClose" }
            val fishPushOpenButton = InlineKeyboardButton("鱼吧推送（开）").apply { callbackData = "douYuFishPushOpen" }
            val fishPushCloseButton = InlineKeyboardButton("鱼吧推送（关）").apply { callbackData = "douYuFishPushClose" }
            val appSignOpenButton = inlineKeyboardButton("app签到（开）", "douYuAppSignOpen")
            val appSignCloseButton = inlineKeyboardButton("app签到（关）", "douYuAppSignClose")
            val titleChangeOpenButton = inlineKeyboardButton("直播标题更新推送（开）", "douYuTitleChangeOpen")
            val titleChangeCloseButton = inlineKeyboardButton("直播标题更新推送（关）", "douYuTitleChangeClose")
            val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
                listOf(liveOpenButton, liveCloseButton),
                listOf(fishOpenButton, fishCloseButton),
                listOf(fishPushOpenButton, fishPushCloseButton),
                listOf(appSignOpenButton, appSignCloseButton),
                listOf(titleChangeOpenButton, titleChangeCloseButton)
            ))
            editMessageText("""
                斗鱼自动签到管理，当前状态：
                开播提醒：${douYuEntity.live.str()}
                鱼吧签到：${douYuEntity.fishGroup.str()}
                鱼吧推送：${douYuEntity.push.str()}
                app签到：${douYuEntity.appSign.str()}
                直播标题更新推送：${douYuEntity.titleChange.str()}
            """.trimIndent(), inlineKeyboardMarkup, top = true)
        }
    }

    fun TelegramSubscribe.huYaManager() {
        before { set(huYaService.findByTgId(query.from.id) ?: errorAnswerCallbackQuery("未绑定虎牙账号")) }
        callback("huYaManager") {}
        callback("huYaLiveOpen") { firstArg<HuYaEntity>().live = Status.ON }
        callback("huYaLiveClose") { firstArg<HuYaEntity>().live = Status.OFF }
        after {
            val huYaEntity = firstArg<HuYaEntity>()
            val liveOpenButton = InlineKeyboardButton("开播提醒（开）").apply { callbackData = "huYaLiveOpen" }
            val liveCloseButton = InlineKeyboardButton("开播提醒（关）").apply { callbackData = "huYaLiveClose" }
            val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
                listOf(liveOpenButton, liveCloseButton)
            ))
            editMessageText("""
                虎牙自动签到管理，当前状态：
                开播提醒：${huYaEntity.live.str()}
            """.trimIndent(), inlineKeyboardMarkup, top = true)
        }
    }

    fun TelegramSubscribe.hostLocManager() {
        before { set(hostLocService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定HostLoc账号")) }
        callback("hostLocManager") {}
        callback("hostLocPushOpen") { firstArg<HostLocEntity>().push = Status.ON }
        callback("hostLocPushClose") { firstArg<HostLocEntity>().push = Status.OFF }
        callback("hostLocSignOpen") { firstArg<HostLocEntity>().sign = Status.ON }
        callback("hostLocSignClose") { firstArg<HostLocEntity>().sign = Status.OFF }
        after {
            val hostLocEntity = firstArg<HostLocEntity>()
            val pushOpenButton = InlineKeyboardButton("动态推送（开）").apply { callbackData = "hostLocPushOpen" }
            val pushCloseButton = InlineKeyboardButton("动态推送（关）").apply { callbackData = "hostLocPushClose" }
            val signOpenButton = InlineKeyboardButton("自动签到（开）").apply { callbackData = "hostLocSignOpen" }
            val signCloseButton = InlineKeyboardButton("自动签到（关）").apply { callbackData = "hostLocSignClose" }
            val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
                listOf(pushOpenButton, pushCloseButton),
                listOf(signOpenButton, signCloseButton)
            ))
            editMessageText("""
                HostLoc自动签到管理，当前状态：
                动态推送：${hostLocEntity.push.str()}
                自动签到：${hostLocEntity.sign.str()}
            """.trimIndent(), inlineKeyboardMarkup, top = true)
        }
    }

    fun TelegramSubscribe.kuGouManager() {
        before { set(kuGouService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定酷狗账号")) }
        callback("kuGouManager") {}
        callback("kuGouSignOpen") { firstArg<KuGouEntity>().sign = Status.ON }
        callback("kuGouSignClose") { firstArg<KuGouEntity>().sign = Status.OFF }
        after {
            val kuGouEntity = firstArg<KuGouEntity>()
            kuGouService.save(kuGouEntity)
            val signOpenButton = InlineKeyboardButton("自动签到（开）").apply { callbackData = "kuGouSignOpen" }
            val signCloseButton = InlineKeyboardButton("自动签到（关）").apply { callbackData = "kuGouSignClose" }
            val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
                listOf(signOpenButton, signCloseButton)
            ))
            editMessageText("""
                酷狗自动签到管理，当前状态：
                自动签到：${kuGouEntity.sign.str()}
            """.trimIndent(), inlineKeyboardMarkup, top = true)
        }
    }

    fun TelegramSubscribe.miHoYoManager() {
        before { set(miHoYoService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定米哈游账号")) }
        callback("miHoYoManager") {}
        callback("miHoYoSignOpen") { firstArg<MiHoYoEntity>().sign = Status.ON }
        callback("miHoYoSignClose") { firstArg<MiHoYoEntity>().sign = Status.OFF }
        after {
            val miHoYoEntity = firstArg<MiHoYoEntity>()
            miHoYoService.save(miHoYoEntity)
            val signOpenButton = InlineKeyboardButton("自动签到（开）").apply { callbackData = "miHoYoSignOpen" }
            val signCloseButton = InlineKeyboardButton("自动签到（关）").apply { callbackData = "miHoYoSignClose" }
            val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
                listOf(signOpenButton, signCloseButton)
            ))
            editMessageText("""
                米哈游（原神）签到管理，当前状态：
                自动签到：${miHoYoEntity.sign.str()}
            """.trimIndent(), inlineKeyboardMarkup, top = true)
        }
    }

    fun TelegramSubscribe.netEaseManager() {
        before { set(netEaseService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定网易云音乐账号")) }
        callback("netEaseManager") {}
        callback("netEaseSignOpen") { firstArg<NetEaseEntity>().sign = Status.ON }
        callback("netEaseSignClose") { firstArg<NetEaseEntity>().sign = Status.OFF }
        callback("netEaseMusicianSignOpen") { firstArg<NetEaseEntity>().musicianSign = Status.ON }
        callback("netEaseMusicianSignClose") { firstArg<NetEaseEntity>().musicianSign = Status.OFF }
        after {
            val netEaseEntity = firstArg<NetEaseEntity>()
            netEaseService.save(netEaseEntity)
            val signOpenButton = InlineKeyboardButton("自动签到（开）").apply { callbackData = "netEaseSignOpen" }
            val signCloseButton = InlineKeyboardButton("自动签到（关）").apply { callbackData = "netEaseSignClose" }
            val musicianSignOpenButton = InlineKeyboardButton("音乐人自动签到（开）").apply { callbackData = "netEaseMusicianSignOpen" }
            val musicianSignCloseButton = InlineKeyboardButton("音乐人自动签到（关）").apply { callbackData = "netEaseMusicianSignClose" }
            val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
                listOf(signOpenButton, signCloseButton),
                listOf(musicianSignOpenButton, musicianSignCloseButton)
            ))
            editMessageText("""
                网易云签到管理，当前状态：
                自动签到：${netEaseEntity.sign.str()}
                音乐人自动签到：${netEaseEntity.musicianSign.str()}
            """.trimIndent(), inlineKeyboardMarkup, top = true)
        }
    }

    fun TelegramSubscribe.stepManager() {
        before { set(stepService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定刷步数的账号")) }
        callback("stepManager") {}
        callback("modifyStep") {
            val stepEntity = firstArg<StepEntity>()
            editMessageText("请发送需要修改的步数")
            stepEntity.step = nextMessage().text.toIntOrNull() ?: -1
        }
        callback("stepOffsetOpen") { firstArg<StepEntity>().offset = Status.ON }
        callback("stepOffsetClose") { firstArg<StepEntity>().offset = Status.OFF }
        after {
            val stepEntity = firstArg<StepEntity>()
            stepService.save(stepEntity)
            val modifyStepButton = InlineKeyboardButton("修改步数").apply { callbackData = "modifyStep" }
            val stepOffsetOpenButton = InlineKeyboardButton("步数偏移（开）").apply { callbackData = "stepOffsetOpen" }
            val stepOffsetCloseButton = InlineKeyboardButton("步数偏移（关）").apply { callbackData = "stepOffsetClose" }
            val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
                listOf(modifyStepButton),
                listOf(stepOffsetOpenButton, stepOffsetCloseButton)
            ))
            editMessageText("""
                刷步数管理，当前状态：
                自动步数：${stepEntity.step} (小于0为关闭自动刷步数)
                步数偏移：${stepEntity.offset.str()} （开启则会在设置的自动步数范围中随机修改）
            """.trimIndent(), inlineKeyboardMarkup, top = true)
        }
    }

    fun TelegramSubscribe.weiboManager() {
        before { set(weiboService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定微博账号")) }
        callback("weiboManager") {}
        callback("weiboPushOpen") { firstArg<WeiboEntity>().push = Status.ON }
        callback("weiboPushClose") { firstArg<WeiboEntity>().push = Status.OFF }
        callback("weiboSignOpen") { firstArg<WeiboEntity>().sign = Status.ON }
        callback("weiboSignClose") { firstArg<WeiboEntity>().sign = Status.OFF }
        after {
            val weiboEntity = firstArg<WeiboEntity>()
            weiboService.save(weiboEntity)
            val pushOpenButton = InlineKeyboardButton("动态推送（开）").apply { callbackData = "weiboPushOpen" }
            val pushCloseButton = InlineKeyboardButton("动态推送（关）").apply { callbackData = "weiboPushClose" }
            val signOpenButton = InlineKeyboardButton("自动签到（开）").apply { callbackData = "weiboSignOpen" }
            val signCloseButton = InlineKeyboardButton("自动签到（关）").apply { callbackData = "weiboSignClose" }
            val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
                listOf(pushOpenButton, pushCloseButton),
                listOf(signOpenButton, signCloseButton)
            ))
            editMessageText("""
                微博自动签到管理，当前状态：
                动态推送：${weiboEntity.push.str()}
                自动签到：${weiboEntity.sign.str()}
            """.trimIndent(), inlineKeyboardMarkup, top = true)
        }
    }

    fun TelegramSubscribe.twitterManager() {
        before { set(twitterService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定Twitter账号")) }
        callback("twitterManager") {}
        callback("twitterPushOpen") { firstArg<TwitterEntity>().push = Status.ON }
        callback("twitterPushClose") { firstArg<TwitterEntity>().push = Status.OFF }
        after {
            val twitterEntity = firstArg<TwitterEntity>()
            twitterService.save(twitterEntity)
            val pushOpenButton = InlineKeyboardButton("推文推送（开）").apply { callbackData = "twitterPushOpen" }
            val pushCloseButton = InlineKeyboardButton("推文推送（关）").apply { callbackData = "twitterPushClose" }
            val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
                listOf(pushOpenButton, pushCloseButton)
            ))
            editMessageText("""
                推特管理，当前状态：
                推文推送：${twitterEntity.push.str()}
            """.trimIndent(), inlineKeyboardMarkup, top = true)
        }
    }

    fun TelegramSubscribe.pixivManager() {
        before { set(pixivService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定pixiv")) }
        callback("pixivManager") {}
        callback("pixivPushOpen") { firstArg<PixivEntity>().push = Status.ON }
        callback("pixivPushClose") { firstArg<PixivEntity>().push = Status.OFF }
        after {
            val pixivEntity = firstArg<PixivEntity>()
            pixivService.save(pixivEntity)
            val pushOpenButton = InlineKeyboardButton("插画推送（开）").apply { callbackData = "pixivPushOpen" }
            val pushCloseButton = InlineKeyboardButton("插画推送（关）").apply { callbackData = "pixivPushClose" }
            val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
                listOf(pushOpenButton, pushCloseButton)
            ))
            editMessageText("""
                pixiv管理，当前状态：
                插画推送：${pixivEntity.push.str()}
            """.trimIndent(), inlineKeyboardMarkup, top = true)
        }
    }

    fun TelegramSubscribe.douYinManager() {
        before { set(douYinService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定抖音账号")) }
        callback("douYinManager") {}
        callback("douYinPushOpen") { firstArg<DouYinEntity>().push = Status.ON }
        callback("douYinPushClose") { firstArg<DouYinEntity>().push = Status.OFF }
        after {
            val douYinEntity = firstArg<DouYinEntity>()
            douYinService.save(douYinEntity)
            val pushOpenButton = inlineKeyboardButton("视频推送（开）", "douYinPushOpen")
            val pushCloseButton = inlineKeyboardButton("视频推送（关）", "douYinPushClose")
            val markup = InlineKeyboardMarkup(listOf(listOf(pushOpenButton, pushCloseButton)))
            editMessageText("""
                抖音管理，当前状态：
                视频推送：${douYinEntity.push.str()}
            """.trimIndent(), markup, top = true)
        }
    }

    fun TelegramSubscribe.smZdmManager() {
        before { set(smZdmService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定什么值得买账号")) }
        callback("smZdmManager") {}
        callback("smZdmSignOpen") { firstArg<SmZdmEntity>().sign = Status.ON }
        callback("smZdmSignClose") { firstArg<SmZdmEntity>().sign = Status.OFF }
        after {
            val smZdmEntity = firstArg<SmZdmEntity>()
            smZdmService.save(smZdmEntity)
            val signOpenButton = inlineKeyboardButton("自动签到（开）", "smZdmSignOpen")
            val signCloseButton = inlineKeyboardButton("自动签到（关）", "smZdmSignClose")
            val markup = InlineKeyboardMarkup(listOf(listOf(signOpenButton, signCloseButton)))
            editMessageText("""
                什么值得买管理，当前状态：
                签到：${smZdmEntity.sign.str()}
            """.trimIndent(), markup, top = true)
        }
    }

    fun TelegramSubscribe.aliDriveManager() {
        before { set(aliDriverService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定阿里云盘账号")) }
        callback("aliDriverManager") {}
        callback("aliDriverSignOpen") { firstArg<AliDriverEntity>().sign = Status.ON }
        callback("aliDriverSignClose") { firstArg<AliDriverEntity>().sign = Status.OFF }
        after {
            val aliDriverEntity = firstArg<AliDriverEntity>()
            aliDriverService.save(aliDriverEntity)
            val signOpenButton = inlineKeyboardButton("自动签到（开）", "aliDriverSignOpen")
            val signCloseButton = inlineKeyboardButton("自动签到（关）", "aliDriverSignClose")
            val markup = InlineKeyboardMarkup(listOf(listOf(signOpenButton, signCloseButton)))
            editMessageText("""
                阿里云盘，当前状态：
                签到：${aliDriverEntity.sign.str()}
            """.trimIndent(), markup, top = true)
        }
    }

    fun TelegramSubscribe.leiShenManager() {
        before { set(leiShenService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定雷神加速器账号")) }
        callback("leiShenManager") {}
        callback("leiShenSignOpen") { firstArg<LeiShenEntity>().status = Status.ON }
        callback("leiShenSignClose") { firstArg<LeiShenEntity>().status = Status.OFF }
        after {
            val leiShenEntity: LeiShenEntity = firstArg()
            leiShenService.save(leiShenEntity)
            val signOpenButton = inlineKeyboardButton("自动签到（开）", "leiShenSignOpen")
            val signCloseButton = inlineKeyboardButton("自动签到（关）", "leiShenSignClose")
            val markup = InlineKeyboardMarkup(listOf(listOf(signOpenButton, signCloseButton)))
            editMessageText("""
                雷神加速器，当前状态：
                未暂停时间提醒：${leiShenEntity.status.str()}
            """.trimIndent(), markup, top = true)
        }
    }

}
