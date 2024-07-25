@file:Suppress("SpellCheckingInspection")

package me.kuku.telegram.extension

import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import me.kuku.telegram.context.*
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.LeiShenLogic
import me.kuku.telegram.logic.LeiShenUserInfo

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
    val smZdmButton = InlineKeyboardButton("什么值得买").callbackData("smZdmManager")
    val aliDrive = inlineKeyboardButton("阿里云盘", "aliDriveManager")
    val leiShen = inlineKeyboardButton("雷神加速器", "leiShenManager")
    val nodeSeek = inlineKeyboardButton("NodeSeek", "nodeSeekManager")
    val glaDos = inlineKeyboardButton("Glados", "glaDosManager")
    val iqy = inlineKeyboardButton("爱奇艺", "iqyManager")
    val eCloud = inlineKeyboardButton("天翼云盘", "eCloudManager")
    val linuxDo = inlineKeyboardButton("LinuxDo", "linuxDoManager")
    return InlineKeyboardMarkup(
        arrayOf(baiduButton, biliBiliButton),
        arrayOf(douYuButton, hostLocButton),
        arrayOf(huYaButton, kuGouButton),
        arrayOf(miHoYoButton, netEaseButton),
        arrayOf(xiaomiStepButton, weiboButton),
        arrayOf(twitterButton, pixivButton),
        arrayOf(smZdmButton, aliDrive),
        arrayOf(leiShen, nodeSeek),
        arrayOf(glaDos, iqy),
        arrayOf(eCloud, linuxDo)
    )
}

fun AbilitySubscriber.manager() {
    sub("manager") {
        val markup = managerKeyboardMarkup()
        sendMessage("请选择管理选项", markup)
    }
}

fun TelegramSubscribe.baiduManager() {
    before { set(BaiduService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定百度账号")) }
    callback("baiduManager") {}
    callback("baiduSignSwitch") { firstArg<BaiduEntity>().sign = firstArg<BaiduEntity>().sign.reverse() }
    after {
        val baiduEntity = firstArg<BaiduEntity>()
        BaiduService.save(baiduEntity)
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
    before { set(BiliBiliService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定哔哩哔哩账号")) }
    callback("biliBiliManager") {}
    callback("biliBiliPushSwitch") { firstArg<BiliBiliEntity>().also { it.push = it.push.reverse() } }
    callback("biliBiliSignSwitch") { firstArg<BiliBiliEntity>().also { it.sign = it.sign.reverse() } }
    callback("biliBiliLiveSwitch") { firstArg<BiliBiliEntity>().also { it.live = it.live.reverse() } }
    after {
        val biliBiliEntity = firstArg<BiliBiliEntity>()
        BiliBiliService.save(biliBiliEntity)
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
    before { set(DouYuService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定斗鱼账号")) }
    callback("douYuManager") {}
    callback("douYuLiveTurn") { firstArg<DouYuEntity>().also { it.live = !it.live } }
    callback("douYuFishTurn") { firstArg<DouYuEntity>().also { it.fishGroup = !it.fishGroup } }
    callback("douYuFishPushTurn") { firstArg<DouYuEntity>().also { it.push = !it.push } }
    callback("douYuTitleChangeTurn") { firstArg<DouYuEntity>().also { it.titleChange = !it.titleChange } }
    after {
        var i = 0
        i += 1
        val douYuEntity = firstArg<DouYuEntity>()
        DouYuService.save(douYuEntity)
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
    before { set(HuYaService.findByTgId(query.from().id()) ?: errorAnswerCallbackQuery("未绑定虎牙账号")) }
    callback("huYaManager") {}
    callback("huYaLiveTurn") { firstArg<HuYaEntity>().also { it.live = !it.live } }
    after {
        val huYaEntity = firstArg<HuYaEntity>()
        HuYaService.save(huYaEntity)
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
    before { set(HostLocService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定HostLoc账号")) }
    callback("hostLocManager") {}
    callback("hostLocPushTurn") { firstArg<HostLocEntity>().also { it.push = !it.push } }
    callback("hostLocSignTurn") { firstArg<HostLocEntity>().also { it.sign = !it.sign } }
    after {
        val hostLocEntity = firstArg<HostLocEntity>()
        HostLocService.save(hostLocEntity)
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
    before { set(KuGouService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定酷狗账号")) }
    callback("kuGouManager") {}
    callback("kuGouSignSwitch") { firstArg<KuGouEntity>().also { it.sign = !it.sign } }
    after {
        val kuGouEntity = firstArg<KuGouEntity>()
        KuGouService.save(kuGouEntity)
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
    before { set(MiHoYoService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定米哈游账号")) }
    callback("miHoYoManager") {}
    callback("miHoYoSignSwitch") { firstArg<MiHoYoEntity>().also { it.sign = !it.sign } }
    callback("mysSignSwitch") { firstArg<MiHoYoEntity>().also { it.mysSign = !it.mysSign } }
    after {
        val miHoYoEntity = firstArg<MiHoYoEntity>()
        MiHoYoService.save(miHoYoEntity)
        val signButton = InlineKeyboardButton("${miHoYoEntity.sign}原神自动签到")
            .callbackData("miHoYoSignSwitch")
        val mys = InlineKeyboardButton("${miHoYoEntity.mysSign}米游社自动签到")
            .callbackData("mysSignSwitch")
        val inlineKeyboardMarkup = InlineKeyboardMarkup(
            arrayOf(signButton),
            arrayOf(mys)
        )
        editMessageText("""
                米哈游签到管理
                注意：签到大概率需要在/config配置rrcor的key
            """.trimIndent(), inlineKeyboardMarkup, top = true)
    }
}

fun TelegramSubscribe.netEaseManager() {
    before { set(NetEaseService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定网易云音乐账号")) }
    callback("netEaseManager") {}
    callback("netEaseSignSwitch") { firstArg<NetEaseEntity>().also { it.sign = !it.sign } }
    callback("netEaseMusicianSignSwitch") { firstArg<NetEaseEntity>().also { it.musicianSign = !it.musicianSign } }
    callback("netEaseVipSignSwitch") { firstArg<NetEaseEntity>().also { it.vipSign = !it.vipSign } }
    after {
        val netEaseEntity = firstArg<NetEaseEntity>()
        NetEaseService.save(netEaseEntity)
        val signButton = InlineKeyboardButton("${netEaseEntity.sign}自动签到")
            .callbackData("netEaseSignSwitch")
        val musicianSignButton = InlineKeyboardButton("${netEaseEntity.musicianSign}音乐人自动签到")
            .callbackData("netEaseMusicianSignSwitch")
        val vipSign = inlineKeyboardButton("vip自动签到", "netEaseVipSignSwitch")
        val inlineKeyboardMarkup = InlineKeyboardMarkup(
            arrayOf(signButton),
            arrayOf(musicianSignButton),
            arrayOf(vipSign)
        )
        editMessageText("""
                网易云签到管理
            """.trimIndent(), inlineKeyboardMarkup, top = true)
    }
}

fun TelegramSubscribe.stepManager() {
    before { set(StepService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定刷步数的账号")) }
    callback("stepManager") {}
    callback("modifyStep") {
        val stepEntity = firstArg<StepEntity>()
        editMessageText("请发送需要修改的步数")
        stepEntity.step = nextMessage().text().toIntOrNull() ?: -1
    }
    callback("stepOffsetSwitch") { firstArg<StepEntity>().also { it.offset = !it.offset } }
    after {
        val stepEntity = firstArg<StepEntity>()
        StepService.save(stepEntity)
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
    before { set(WeiboService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定微博账号")) }
    callback("weiboManager") {}
    callback("weiboPushTurn") { firstArg<WeiboEntity>().also { it.push = !it.push } }
    callback("weiboSignTurn") { firstArg<WeiboEntity>().also { it.sign = !it.sign } }
    after {
        val weiboEntity = firstArg<WeiboEntity>()
        WeiboService.save(weiboEntity)
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
    before { set(TwitterService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定Twitter账号")) }
    callback("twitterManager") {}
    callback("twitterPushSwitch") { firstArg<TwitterEntity>().also { it.push = !it.push } }
    after {
        val twitterEntity = firstArg<TwitterEntity>()
        TwitterService.save(twitterEntity)
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
    before { set(PixivService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定pixiv")) }
    callback("pixivManager") {}
    callback("pixivPushSwitch") { firstArg<PixivEntity>().also { it.push = !it.push } }
    after {
        val pixivEntity = firstArg<PixivEntity>()
        PixivService.save(pixivEntity)
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

fun TelegramSubscribe.smZdmManager() {
    before { set(SmZdmService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定什么值得买账号")) }
    callback("smZdmManager") {}
    callback("smZdmSignSwitch") { firstArg<SmZdmEntity>().also { it.sign = !it.sign } }
    after {
        val smZdmEntity = firstArg<SmZdmEntity>()
        SmZdmService.save(smZdmEntity)
        val signButton = inlineKeyboardButton("${smZdmEntity.sign}自动签到",
            "smZdmSignSwitch")
        val markup = InlineKeyboardMarkup(arrayOf(signButton))
        editMessageText("""
                什么值得买管理
            """.trimIndent(), markup, top = true)
    }
}

fun TelegramSubscribe.aliDriveManager() {
    before { set(AliDriveService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定阿里云盘账号")) }
    callback("aliDriveManager") {}
    callback("aliDriveSignSwitch") { firstArg<AliDriveEntity>().also { it.sign = !it.sign } }
    callback("aliDriveReceiveSwitch") { firstArg<AliDriveEntity>().also { it.receive = !it.receive } }
    callback("aliDriveTaskSwitch") { firstArg<AliDriveEntity>().also { it.task = !it.task } }
    callback("aliDriveReceiveTaskSwitch") { firstArg<AliDriveEntity>().also { it.receiveTask = !it.receiveTask } }
    callback("aliDriveDeviceRoomTurn") { firstArg<AliDriveEntity>().also { it.deviceRoom = !it.deviceRoom } }
    callback("aliDriveCardSwitch") { firstArg<AliDriveEntity>().also { it.card = !it.card } }
    after {
        val aliDriveEntity = firstArg<AliDriveEntity>()
        AliDriveService.save(aliDriveEntity)
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
    before { set(LeiShenService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定雷神加速器账号")) }
    callback("leiShenManager") {}
    callback("leiShenSignSwitch") { firstArg<LeiShenEntity>().also { it.status = !it.status } }
    callback("leiShenPauseRecover") {
        val userInfo = LeiShenLogic.userInfo(firstArg())
        if (userInfo.pauseStatusId == 1)
            LeiShenLogic.recover(firstArg())
        else
            LeiShenLogic.pause(firstArg())
    }
    callback("leiShenPause") {
        kotlin.runCatching {
            LeiShenLogic.pause(firstArg())
            answerCallbackQuery("雷神加速器时间暂停成功", true)
        }.onFailure {
            answerCallbackQuery(it.message ?: "未知错误", true)
        }
    }
    after {
        val leiShenEntity: LeiShenEntity = firstArg()
        LeiShenService.save(leiShenEntity)
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
    before { set(NodeSeekService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定NodeSeek账号")) }
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
        NodeSeekService.save(nodeSeekEntity)
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
    before { set(GlaDosService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定GlaDos账号")) }
    callback("glaDosManager") {}
    callback("glaDosSignSwitch") { firstArg<GlaDosEntity>().also { it.sign = !it.sign } }
    after {
        val glaDosEntity: GlaDosEntity = firstArg()
        GlaDosService.save(glaDosEntity)
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
    before { IqyService.findByTgId(tgId).set("未绑定爱奇艺账号") }
    callback("iqyManager") {}
    callback("iqySignSwitch") { firstArg<IqyEntity>().also { it.sign = !it.sign } }
    after {
        val entity: IqyEntity = firstArg()
        IqyService.save(entity)
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
    before { ECloudService.findByTgId(tgId).set("未绑定天翼云盘账号") }
    callback("eCloudManager") {}
    callback("eCloudSignSwitch") { firstArg<ECloudEntity>().also { it.sign = !it.sign } }
    after {
        val entity: ECloudEntity = firstArg()
        ECloudService.save(entity)
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

fun TelegramSubscribe.linuxDoManager() {
    before { LinuxDoService.findByTgId(tgId).set("未绑定LinuxDo账号") }
    callback("linuxDoManager") {}
    callback("linuxDoSignSwitch") { firstArg<LinuxDoEntity>().also { it.sign = !it.sign } }
    after {
        val entity: LinuxDoEntity = firstArg()
        LinuxDoService.save(entity)
        val markup = InlineKeyboardMarkup(
            arrayOf(
                inlineKeyboardButton("${entity.sign}自动签到", "linuxDoSignSwitch"),
            )
        )
        editMessageText("""
                LinuxDo
            """.trimIndent(), markup, top = true)
    }
}
