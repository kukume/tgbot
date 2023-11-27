@file:Suppress("SpellCheckingInspection")

package me.kuku.telegram.extension

import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import me.kuku.telegram.context.*
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.LeiShenLogic
import me.kuku.telegram.logic.LeiShenUserInfo
import org.springframework.stereotype.Service

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
    private val aliDriveService: AliDriveService,
    private val leiShenService: LeiShenService,
    private val nodeSeekService: NodeSeekService,
    private val glaDosService: GlaDosService,
    private val iqyService: IqyService,
    private val eCloudService: ECloudService
) {

    private fun managerKeyboardMarkup(): InlineKeyboardMarkup {
        val baiduButton = InlineKeyboardButton("百度").callbackData("baiduManager")
        val biliBiliButton = InlineKeyboardButton("哔哩哔哩").callbackData("biliBiliManager")
        val douYuButton = InlineKeyboardButton("斗鱼").callbackData("douYuManager")
        val hostLocButton = InlineKeyboardButton("HostLoc").callbackData("hostLocManager")
        val huYaButton = InlineKeyboardButton("虎牙").callbackData("huYaManager")
        val kuGouButton = InlineKeyboardButton("酷狗").callbackData("kuGouManager")
        val miHoYoButton = InlineKeyboardButton("米哈游").callbackData("miHoYoManager")
        val netEaseButton = InlineKeyboardButton("网易云音乐").callbackData("netEaseManager")
        val xiaomiStepButton = InlineKeyboardButton("刷步数").callbackData("stepManager")
        val weiboButton = InlineKeyboardButton("微博").callbackData("weiboManager")
        val twitterButton = InlineKeyboardButton("twitter").callbackData("twitterManager")
        val pixivButton = InlineKeyboardButton("pixiv").callbackData("pixivManager")
        val douYinButton = InlineKeyboardButton("抖音").callbackData("douYinManager")
        val smZdmButton = InlineKeyboardButton("什么值得买").callbackData("smZdmManager")
        val aliDrive = inlineKeyboardButton("阿里云盘", "aliDriveManager")
        val leiShen = inlineKeyboardButton("雷神加速器", "leiShenManager")
        val nodeSeek = inlineKeyboardButton("NodeSeek", "nodeSeekManager")
        val glaDos = inlineKeyboardButton("Glados", "glaDosManager")
        val iqy = inlineKeyboardButton("爱奇艺", "iqyManager")
        val eCloud = inlineKeyboardButton("天翼云盘", "eCloudManager")
        return InlineKeyboardMarkup(
            arrayOf(baiduButton, biliBiliButton),
            arrayOf(douYuButton, hostLocButton),
            arrayOf(huYaButton, kuGouButton),
            arrayOf(miHoYoButton, netEaseButton),
            arrayOf(xiaomiStepButton, weiboButton),
            arrayOf(twitterButton, pixivButton),
            arrayOf(douYinButton, smZdmButton),
            arrayOf(aliDrive, leiShen),
            arrayOf(nodeSeek, glaDos),
            arrayOf(iqy, eCloud)
        )
    }

    fun AbilitySubscriber.manager() {
        sub("manager") {
            val markup = managerKeyboardMarkup()
            sendMessage("请选择管理选项", markup)
        }
    }

    fun TelegramSubscribe.baiduManager() {
        before { set(baiduService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定百度账号")) }
        callback("baiduManager") {}
        callback("baiduSignSwitch") { firstArg<BaiduEntity>().sign = firstArg<BaiduEntity>().sign.reverse() }
        after {
            val baiduEntity = firstArg<BaiduEntity>()
            baiduService.save(baiduEntity)
            val signOpenButton = inlineKeyboardButton("${baiduEntity.sign.str()}自动签到", "baiduSignSwitch")
            val inlineKeyboardMarkup = InlineKeyboardMarkup(
                arrayOf(signOpenButton)
            )
            editMessageText("""
                百度自动签到管理
            """.trimIndent(), inlineKeyboardMarkup, top = true)
        }
    }

    fun TelegramSubscribe.biliBiliManager() {
        before { set(biliBiliService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定哔哩哔哩账号")) }
        callback("biliBiliManager") {}
        callback("biliBiliPushSwitch") { firstArg<BiliBiliEntity>().also { it.push = it.push.reverse() } }
        callback("biliBiliSignSwitch") { firstArg<BiliBiliEntity>().also { it.sign = it.sign.reverse() } }
        callback("biliBiliLiveSwitch") { firstArg<BiliBiliEntity>().also { it.live = it.live.reverse() } }
        after {
            val biliBiliEntity = firstArg<BiliBiliEntity>()
            biliBiliService.save(biliBiliEntity)
            val pushButton = InlineKeyboardButton("${biliBiliEntity.push.str()}动态推送")
                .callbackData("biliBiliPushSwitch")
            val signButton = InlineKeyboardButton("${biliBiliEntity.sign.str()}自动签到")
                .callbackData("biliBiliSignSwitch")
            val liveButton = InlineKeyboardButton("${biliBiliEntity.live.str()}开播提醒")
                .callbackData("biliBiliLiveSwitch")
            val inlineKeyboardMarkup = InlineKeyboardMarkup(
                arrayOf(pushButton),
                arrayOf(signButton),
                arrayOf(liveButton)
            )
            editMessageText("""
                哔哩哔哩自动签到管理
            """.trimIndent(), inlineKeyboardMarkup, top = true)
        }
    }

    fun TelegramSubscribe.douYuManager() {
        before { set(douYuService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定斗鱼账号")) }
        callback("douYuManager") {}
        callback("douYuLiveTurn") { firstArg<DouYuEntity>().also { it.live = !it.live } }
        callback("douYuFishTurn") { firstArg<DouYuEntity>().also { it.fishGroup = !it.fishGroup } }
        callback("douYuFishPushTurn") { firstArg<DouYuEntity>().also { it.push = !it.push } }
        callback("douYuTitleChangeTurn") { firstArg<DouYuEntity>().also { it.titleChange = !it.titleChange } }
        after {
            var i = 0
            i += 1
            val douYuEntity = firstArg<DouYuEntity>()
            douYuService.save(douYuEntity)
            val liveButton = InlineKeyboardButton("${douYuEntity.live.str()}开播提醒")
                .callbackData("douYuLiveTurn")
            val fishButton = InlineKeyboardButton("${douYuEntity.fishGroup.str()}鱼吧签到")
                .callbackData("douYuFishTurn")
            val fishPushButton = InlineKeyboardButton("${douYuEntity.push.str()}鱼吧推送")
                .callbackData("douYuFishPushTurn")
            val titleChangeButton = inlineKeyboardButton("${douYuEntity.titleChange.str()}直播标题更新推送",
                "douYuTitleChangeTurn")
            val inlineKeyboardMarkup = InlineKeyboardMarkup(
                arrayOf(liveButton),
                arrayOf(fishButton),
                arrayOf(fishPushButton),
                arrayOf(titleChangeButton)
            )
            editMessageText("""
                斗鱼自动签到管理
            """.trimIndent(), inlineKeyboardMarkup, top = true)
        }
    }

    fun TelegramSubscribe.huYaManager() {
        before { set(huYaService.findByTgId(query.from().id()) ?: errorAnswerCallbackQuery("未绑定虎牙账号")) }
        callback("huYaManager") {}
        callback("huYaLiveTurn") { firstArg<HuYaEntity>().also { it.live = !it.live } }
        after {
            val huYaEntity = firstArg<HuYaEntity>()
            huYaService.save(huYaEntity)
            val liveButton = InlineKeyboardButton("${huYaEntity.live.str()}开播提醒")
                .callbackData("huYaLiveTurn")
            val inlineKeyboardMarkup = InlineKeyboardMarkup(
                arrayOf(liveButton)
            )
            editMessageText("""
                虎牙自动签到管理
            """.trimIndent(), inlineKeyboardMarkup, top = true)
        }
    }

    fun TelegramSubscribe.hostLocManager() {
        before { set(hostLocService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定HostLoc账号")) }
        callback("hostLocManager") {}
        callback("hostLocPushTurn") { firstArg<HostLocEntity>().also { it.push = !it.push } }
        callback("hostLocSignTurn") { firstArg<HostLocEntity>().also { it.sign = !it.sign } }
        after {
            val hostLocEntity = firstArg<HostLocEntity>()
            hostLocService.save(hostLocEntity)
            val pushButton = InlineKeyboardButton("${hostLocEntity.push}动态推送")
                .callbackData("hostLocPushTurn")
            val signButton = InlineKeyboardButton("${hostLocEntity.sign}自动签到")
                .callbackData("hostLocSignTurn")
            val inlineKeyboardMarkup = InlineKeyboardMarkup(
                arrayOf(pushButton),
                arrayOf(signButton)
            )
            editMessageText("""
                HostLoc自动签到管理
            """.trimIndent(), inlineKeyboardMarkup, top = true)
        }
    }

    fun TelegramSubscribe.kuGouManager() {
        before { set(kuGouService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定酷狗账号")) }
        callback("kuGouManager") {}
        callback("kuGouSignSwitch") { firstArg<KuGouEntity>().also { it.sign = !it.sign } }
        after {
            val kuGouEntity = firstArg<KuGouEntity>()
            kuGouService.save(kuGouEntity)
            val signButton = InlineKeyboardButton("${kuGouEntity.sign}自动签到")
                .callbackData("kuGouSignSwitch")
            val inlineKeyboardMarkup = InlineKeyboardMarkup(
                arrayOf(signButton)
            )
            editMessageText("""
                酷狗自动签到管理
            """.trimIndent(), inlineKeyboardMarkup, top = true)
        }
    }

    fun TelegramSubscribe.miHoYoManager() {
        before { set(miHoYoService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定米哈游账号")) }
        callback("miHoYoManager") {}
        callback("miHoYoSignSwitch") { firstArg<MiHoYoEntity>().also { it.sign = !it.sign } }
        after {
            val miHoYoEntity = firstArg<MiHoYoEntity>()
            miHoYoService.save(miHoYoEntity)
            val signButton = InlineKeyboardButton("${miHoYoEntity.sign}自动签到")
                .callbackData("miHoYoSignSwitch")
            val inlineKeyboardMarkup = InlineKeyboardMarkup(
                arrayOf(signButton)
            )
            editMessageText("""
                米哈游（原神）签到管理
            """.trimIndent(), inlineKeyboardMarkup, top = true)
        }
    }

    fun TelegramSubscribe.netEaseManager() {
        before { set(netEaseService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定网易云音乐账号")) }
        callback("netEaseManager") {}
        callback("netEaseSignSwitch") { firstArg<NetEaseEntity>().also { it.sign = !it.sign } }
        callback("netEaseMusicianSignSwitch") { firstArg<NetEaseEntity>().also { it.musicianSign = !it.musicianSign } }
        after {
            val netEaseEntity = firstArg<NetEaseEntity>()
            netEaseService.save(netEaseEntity)
            val signButton = InlineKeyboardButton("${netEaseEntity.sign}自动签到")
                .callbackData("netEaseSignSwitch")
            val musicianSignButton = InlineKeyboardButton("${netEaseEntity.musicianSign}音乐人自动签到")
                .callbackData("netEaseMusicianSignSwitch")
            val inlineKeyboardMarkup = InlineKeyboardMarkup(
                arrayOf(signButton),
                arrayOf(musicianSignButton)
            )
            editMessageText("""
                网易云签到管理
            """.trimIndent(), inlineKeyboardMarkup, top = true)
        }
    }

    fun TelegramSubscribe.stepManager() {
        before { set(stepService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定刷步数的账号")) }
        callback("stepManager") {}
        callback("modifyStep") {
            val stepEntity = firstArg<StepEntity>()
            editMessageText("请发送需要修改的步数")
            stepEntity.step = nextMessage().text().toIntOrNull() ?: -1
        }
        callback("stepOffsetSwitch") { firstArg<StepEntity>().also { it.offset = !it.offset } }
        after {
            val stepEntity = firstArg<StepEntity>()
            stepService.save(stepEntity)
            val modifyStepButton = InlineKeyboardButton("修改步数").callbackData("modifyStep")
            val stepOffsetButton = InlineKeyboardButton("${stepEntity.offset}步数偏移")
                .callbackData("stepOffsetSwitch")
            val inlineKeyboardMarkup = InlineKeyboardMarkup(
                arrayOf(modifyStepButton),
                arrayOf(stepOffsetButton)
            )
            editMessageText("""
                刷步数管理，当前状态：
                自动步数：${stepEntity.step} (小于0为关闭自动刷步数)
                步数偏移（开启则会在设置的自动步数范围中随机修改）
            """.trimIndent(), inlineKeyboardMarkup, top = true)
        }
    }

    fun TelegramSubscribe.weiboManager() {
        before { set(weiboService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定微博账号")) }
        callback("weiboManager") {}
        callback("weiboPushTurn") { firstArg<WeiboEntity>().also { it.push = !it.push } }
        callback("weiboSignTurn") { firstArg<WeiboEntity>().also { it.sign = !it.sign } }
        after {
            val weiboEntity = firstArg<WeiboEntity>()
            weiboService.save(weiboEntity)
            val pushButton = InlineKeyboardButton("${weiboEntity.push}动态推送")
                .callbackData("weiboPushTurn")
            val signButton = InlineKeyboardButton("${weiboEntity.sign}自动签到")
                .callbackData("weiboSignTurn")
            val inlineKeyboardMarkup = InlineKeyboardMarkup(
                arrayOf(pushButton),
                arrayOf(signButton)
            )
            editMessageText("""
                微博自动签到管理
            """.trimIndent(), inlineKeyboardMarkup, top = true)
        }
    }

    fun TelegramSubscribe.twitterManager() {
        before { set(twitterService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定Twitter账号")) }
        callback("twitterManager") {}
        callback("twitterPushSwitch") { firstArg<TwitterEntity>().also { it.push = !it.push } }
        after {
            val twitterEntity = firstArg<TwitterEntity>()
            twitterService.save(twitterEntity)
            val pushButton = InlineKeyboardButton("${twitterEntity.push}推文推送")
                .callbackData("twitterPushSwitch")
            val inlineKeyboardMarkup = InlineKeyboardMarkup(
                arrayOf(pushButton)
            )
            editMessageText("""
                推特管理
            """.trimIndent(), inlineKeyboardMarkup, top = true)
        }
    }

    fun TelegramSubscribe.pixivManager() {
        before { set(pixivService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定pixiv")) }
        callback("pixivManager") {}
        callback("pixivPushSwitch") { firstArg<PixivEntity>().also { it.push = !it.push } }
        after {
            val pixivEntity = firstArg<PixivEntity>()
            pixivService.save(pixivEntity)
            val pushButton = InlineKeyboardButton("${pixivEntity.push}插画推送")
                .callbackData("pixivPushSwitch")
            val inlineKeyboardMarkup = InlineKeyboardMarkup(
                arrayOf(pushButton)
            )
            editMessageText("""
                pixiv管理
            """.trimIndent(), inlineKeyboardMarkup, top = true)
        }
    }

    fun TelegramSubscribe.douYinManager() {
        before { set(douYinService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定抖音账号")) }
        callback("douYinManager") {}
        callback("douYinPushSwitch") { firstArg<DouYinEntity>().also { it.push = !it.push } }
        after {
            val douYinEntity = firstArg<DouYinEntity>()
            douYinService.save(douYinEntity)
            val pushButton = inlineKeyboardButton("${douYinEntity.push}视频推送",
                "douYinPushSwitch")
            val markup = InlineKeyboardMarkup(arrayOf(pushButton))
            editMessageText("""
                抖音管理
            """.trimIndent(), markup, top = true)
        }
    }

    fun TelegramSubscribe.smZdmManager() {
        before { set(smZdmService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定什么值得买账号")) }
        callback("smZdmManager") {}
        callback("smZdmSignSwitch") { firstArg<SmZdmEntity>().also { it.sign = !it.sign } }
        after {
            val smZdmEntity = firstArg<SmZdmEntity>()
            smZdmService.save(smZdmEntity)
            val signButton = inlineKeyboardButton("${smZdmEntity.sign}自动签到",
                "smZdmSignSwitch")
            val markup = InlineKeyboardMarkup(arrayOf(signButton))
            editMessageText("""
                什么值得买管理
            """.trimIndent(), markup, top = true)
        }
    }

    fun TelegramSubscribe.aliDriveManager() {
        before { set(aliDriveService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定阿里云盘账号")) }
        callback("aliDriveManager") {}
        callback("aliDriveSignSwitch") { firstArg<AliDriveEntity>().also { it.sign = !it.sign } }
        callback("aliDriveReceiveSwitch") { firstArg<AliDriveEntity>().also { it.receive = !it.receive } }
        callback("aliDriveTaskSwitch") { firstArg<AliDriveEntity>().also { it.task = !it.task } }
        callback("aliDriveReceiveTaskSwitch") { firstArg<AliDriveEntity>().also { it.receiveTask = !it.receiveTask } }
        callback("aliDriveDeviceRoomTurn") { firstArg<AliDriveEntity>().also { it.deviceRoom = !it.deviceRoom } }
        callback("aliDriveCardSwitch") { firstArg<AliDriveEntity>().also { it.card = !it.card } }
        after {
            val aliDriveEntity = firstArg<AliDriveEntity>()
            aliDriveService.save(aliDriveEntity)
            val signButton = inlineKeyboardButton("${aliDriveEntity.sign}自动签到",
                "aliDriveSignSwitch")
            val receiveButton = inlineKeyboardButton("${aliDriveEntity.receive}自动领取",
                "aliDriveReceiveSwitch")
            val task = inlineKeyboardButton("${aliDriveEntity.task}完成任务",
                "aliDriveTaskSwitch")
            val receiveTask = inlineKeyboardButton("${aliDriveEntity.receiveTask}领取任务奖励",
                "aliDriveReceiveTaskSwitch")
            val deviceRoom = InlineKeyboardButton("${aliDriveEntity.deviceRoom}时光设备间")
                .callbackData("aliDriveDeviceRoomTurn")
            val card = inlineKeyboardButton("${aliDriveEntity.card}领取补签卡",
                "aliDriveCardSwitch")
            val markup = InlineKeyboardMarkup(arrayOf(signButton), arrayOf(receiveButton),
                arrayOf(task), arrayOf(receiveTask), arrayOf(deviceRoom), arrayOf(card)
            )
            editMessageText("""
                阿里云盘，如自动签到为关，自动领取不生效
                完成任务会在你的云盘上上传图片、视频、新建文件夹等，介意勿用
                完成任务如出现device offline错误，请找到阿里云盘的登录设备管理，下线一些设备即可
            """.trimIndent(), markup, top = true)
        }
    }

    fun TelegramSubscribe.leiShenManager() {
        before { set(leiShenService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定雷神加速器账号")) }
        callback("leiShenManager") {}
        callback("leiShenSignSwitch") { firstArg<LeiShenEntity>().also { it.status = !it.status } }
        callback("leiShenPauseRecover") {
            val userInfo = LeiShenLogic.userInfo(firstArg())
            if (userInfo.pauseStatusId == 1)
                LeiShenLogic.recover(firstArg())
            else
                LeiShenLogic.pause(firstArg())
        }
        after {
            val leiShenEntity: LeiShenEntity = firstArg()
            leiShenService.save(leiShenEntity)
            val signButton = inlineKeyboardButton("${leiShenEntity.status}未暂停时间提醒",
                "leiShenSignSwitch")
            val split = inlineKeyboardButton("以下是手动暂停与恢复时间按钮", "not")
            val info = try {
                secondArg<LeiShenUserInfo>()
            } catch (e: Exception) { LeiShenLogic.userInfo(leiShenEntity) }
            val infoStr = if (info.pauseStatusId == 1) "暂停" else "恢复"
            val pause = inlineKeyboardButton("（$infoStr）暂停/恢复时间", "leiShenPauseRecover")
            val markup = InlineKeyboardMarkup(
                arrayOf(signButton),
                arrayOf(split),
                arrayOf(pause)
            )
            editMessageText("""
                雷神加速器
            """.trimIndent(), markup, top = true)
        }
    }

    fun TelegramSubscribe.nodeSeekManager() {
        before { set(nodeSeekService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定NodeSeek账号")) }
        callback("nodeSeekManager") {}
        callback("nodeSeekSwitch") {
            val entity = firstArg<NodeSeekEntity>()
            val sign = entity.sign
            when (sign) {
                NodeSeekEntity.Sign.None -> entity.sign = NodeSeekEntity.Sign.Random
                NodeSeekEntity.Sign.Random -> entity.sign = NodeSeekEntity.Sign.Fix
                NodeSeekEntity.Sign.Fix -> entity.sign = NodeSeekEntity.Sign.None
            }
        }
        after {
            val nodeSeekEntity: NodeSeekEntity = firstArg()
            nodeSeekService.save(nodeSeekEntity)
            val markup = InlineKeyboardMarkup(
                arrayOf(
                    inlineKeyboardButton("（${nodeSeekEntity.sign.value}）自动签到", "nodeSeekSwitch")
                )
            )
            editMessageText("""
                NodeSeek
            """.trimIndent(), markup, top = true)
        }
    }

    fun TelegramSubscribe.glaDosManager() {
        before { set(glaDosService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定GlaDos账号")) }
        callback("glaDosManager") {}
        callback("glaDosSignSwitch") { firstArg<GlaDosEntity>().also { it.sign = !it.sign } }
        after {
            val glaDosEntity: GlaDosEntity = firstArg()
            glaDosService.save(glaDosEntity)
            val markup = InlineKeyboardMarkup(
                arrayOf(
                    inlineKeyboardButton("${glaDosEntity.sign}自动签到", "glaDosSignSwitch"),
                )
            )
            editMessageText("""
                GlaDos
            """.trimIndent(), markup, top = true)
        }
    }

    fun TelegramSubscribe.iqyManager() {
        before { iqyService.findByTgId(tgId).set("未绑定爱奇艺账号") }
        callback("iqyManager") {}
        callback("iqySignSwitch") { firstArg<IqyEntity>().also { it.sign = !it.sign } }
        after {
            val entity: IqyEntity = firstArg()
            iqyService.save(entity)
            val markup = InlineKeyboardMarkup(
                arrayOf(
                    inlineKeyboardButton("${entity.sign}自动签到", "iqySignSwitch"),
                )
            )
            editMessageText("""
                爱奇艺
            """.trimIndent(), markup, top = true)
        }
    }

    fun TelegramSubscribe.eCloudManager() {
        before { eCloudService.findByTgId(tgId).set("未绑定天翼云盘账号") }
        callback("eCloudManager") {}
        callback("eCloudSignSwitch") { firstArg<ECloudEntity>().also { it.sign = !it.sign } }
        after {
            val entity: ECloudEntity = firstArg()
            eCloudService.save(entity)
            val markup = InlineKeyboardMarkup(
                arrayOf(
                    inlineKeyboardButton("${entity.sign}自动签到", "eCloudSignSwitch"),
                )
            )
            editMessageText("""
                天翼云盘
            """.trimIndent(), markup, top = true)
        }
    }

}
