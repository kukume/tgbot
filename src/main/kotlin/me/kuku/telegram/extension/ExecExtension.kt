package me.kuku.telegram.extension

import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import kotlinx.coroutines.delay
import me.kuku.telegram.context.*
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.*


private fun execKeyboardMarkup(): InlineKeyboardMarkup {
    val baiduButton = InlineKeyboardButton("百度").callbackData("baiduExec")
    val biliBiliButton = InlineKeyboardButton("哔哩哔哩").callbackData("biliBiliExec")
    val hostLocButton = InlineKeyboardButton("HostLoc").callbackData("hostLocExec")
    val kuGouButton = InlineKeyboardButton("酷狗").callbackData("kuGouExec")
    val miHoYoButton = InlineKeyboardButton("米哈游").callbackData("miHoYoExec")
    val netEaseButton = InlineKeyboardButton("网易云音乐").callbackData("netEaseExec")
    val stepButton = InlineKeyboardButton("刷步数").callbackData("stepExec")
    val weiboButton = InlineKeyboardButton("微博").callbackData("weiboExec")
    val douYuButton = InlineKeyboardButton("斗鱼").callbackData("douYuExec")
    val smZdmButton = InlineKeyboardButton("什么值得买").callbackData("smZdmExec")
    val aliDrive = inlineKeyboardButton("阿里云盘", "aliDriveExec")
    val nodeSeek = inlineKeyboardButton("NodeSeek", "nodeSeekExec")
    val glaDos = inlineKeyboardButton("GlaDos", "glaDosExec")
    val iqy = inlineKeyboardButton("爱奇艺", "iqyExec")
    val eCloud = inlineKeyboardButton("天翼云盘", "eCloudExec")
    return InlineKeyboardMarkup(
        arrayOf(baiduButton, biliBiliButton),
        arrayOf(hostLocButton, kuGouButton),
        arrayOf(miHoYoButton, netEaseButton),
        arrayOf(stepButton, weiboButton),
        arrayOf(douYuButton, smZdmButton),
        arrayOf(aliDrive, nodeSeek),
        arrayOf(glaDos, iqy),
        arrayOf(eCloud)
    )
}

fun AbilitySubscriber.exec() {
    sub("exec") {
        val markup = execKeyboardMarkup()
        sendMessage("请选择手动执行选项", markup)
    }
}

fun TelegramSubscribe.baiduExec() {
    before { set(BaiduService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定百度账号")) }
    callback("baiduExec") {
        val tieBaSignButton = InlineKeyboardButton("贴吧签到").callbackData("tieBaSign")
        val ybbSignButton = InlineKeyboardButton("游帮帮加速器签到").callbackData("ybbSign")
        val ybbWatchAdButton = InlineKeyboardButton("游帮帮加速器看广告").callbackData("ybbWatchAd")
        val exchangeVip = InlineKeyboardButton("游帮帮加速器兑换手游会员").callbackData("ybbExchangeVip")
        val markup = InlineKeyboardMarkup(
            arrayOf(tieBaSignButton), arrayOf(ybbSignButton), arrayOf(ybbWatchAdButton), arrayOf(exchangeVip)
        )
        editMessageText("百度", markup)
    }
    callback("tieBaSign") {
        editMessageText("由于贴吧签到时间长，后台运行中")
        kotlin.runCatching {
            BaiduLogic.tieBaSign(firstArg())
            sendMessage("#手动执行结果\n贴吧签到成功")
        }.onFailure {
            sendMessage("#手动执行结果\n贴吧签到失败，${it.message}")
        }
    }
    callback("ybbSign") {
        BaiduLogic.ybbSign(firstArg())
        editMessageText("游帮帮签到成功")
    }
    callback("ybbWatchAd") {
        BaiduLogic.ybbWatchAd(firstArg(), "v3")
        editMessageText("游帮帮观看广告成功")
    }
    callback("ybbExchangeVip") {
        BaiduLogic.ybbExchangeVip(firstArg())
        editMessageText("游帮帮加速器兑换会员成功")
    }
}

fun TelegramSubscribe.biliBiliExec() {
    before { set(BiliBiliService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定哔哩哔哩账号")) }
    callback("biliBiliExec") {
        val biliBiliSignButton = InlineKeyboardButton("签到").callbackData("biliBiliSign")
        val markup = InlineKeyboardMarkup(arrayOf(biliBiliSignButton))
        editMessageText("哔哩哔哩\n如出现账号异常为cookie失效，请重新登陆哔哩哔哩", markup)
    }
    callback("biliBiliSign") {
        val biliBiliEntity = firstArg<BiliBiliEntity>()
        val firstRank = BiliBiliLogic.ranking(biliBiliEntity)[0]
        BiliBiliLogic.watchVideo(biliBiliEntity, firstRank)
        delay(1000)
        BiliBiliLogic.share(biliBiliEntity, firstRank.aid)
        delay(1000)
        BiliBiliLogic.liveSign(biliBiliEntity)
        editMessageText("哔哩哔哩签到成功")
    }
}

fun TelegramSubscribe.hostLocExec()  {
    before { set(HostLocService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定HostLoc账号")) }
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
        editMessageText("HostLoc签到成功")
    }
}

fun TelegramSubscribe.kuGouExec() {
    before { set(KuGouService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定酷狗账号")) }
    callback("kuGouExec") {
        val kuGouMusicianSignButton = InlineKeyboardButton("音乐人").callbackData("kuGouMusicianSign")
        val kuGouListenButton = InlineKeyboardButton("概念版听歌得vip").callbackData("kuGouListen")
        val watchAd = InlineKeyboardButton("概念版看广告").callbackData("kuGouWatchAd")
        val markup = InlineKeyboardMarkup(arrayOf(kuGouMusicianSignButton), arrayOf(kuGouListenButton), arrayOf(watchAd))
        editMessageText("酷狗音乐", markup)
    }
    callback("kuGouMusicianSign") {
        KuGouLogic.musicianSign(firstArg())
        editMessageText("酷狗音乐人签到成功")
    }
    callback("kuGouListen") {
        KuGouLogic.listenMusic(firstArg())
        editMessageText("酷狗概念版听歌得vip成功")
    }
    callback("kuGouWatchAd") {
        KuGouLogic.watchAd(firstArg())
        editMessageText("酷狗概念版看广告成功")
    }
}

fun TelegramSubscribe.miHoYoExec() {
    before { set(MiHoYoService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定米哈游账号")) }
    callback("miHoYoExec") {
        val genShinSignButton = InlineKeyboardButton("原神签到").callbackData("genShinSign")
        val mysSign = InlineKeyboardButton("米游社签到").callbackData("mysSign")
        val markup = InlineKeyboardMarkup(arrayOf(genShinSignButton), arrayOf(mysSign))
        editMessageText("""
                米哈游
                注意：签到大概率需要在/config配置2captcha的key
            """.trimIndent(), markup)
    }
    callback("genShinSign") {
        MiHoYoLogic.sign(firstArg(), tgId)
        editMessageText("原神签到成功")
    }
    callback("mysSign") {
        MiHoYoLogic.mysSign(firstArg())
        editMessageText("米游社区签到成功")
    }
}

fun TelegramSubscribe.netEaseExec() {
    before { set(NetEaseService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定网易云音乐账号")) }
    callback("netEaseExec") {
        val netEaseSignButton = InlineKeyboardButton("签到").callbackData("netEaseSign")
        val sign1 = InlineKeyboardButton("音乐人签到（推荐任务）").callbackData("netEaseMusicianSign1")
        val sign2 = inlineKeyboardButton("音乐人签到（每日任务）", "netEaseMusicianSign2")
        val sign3 = inlineKeyboardButton("其他", "netEaseMusicianSign3")
        val vipSign = inlineKeyboardButton("vip签到", "netEaseVipSign")
        val markup = InlineKeyboardMarkup(
            arrayOf(netEaseSignButton),
            arrayOf(sign1),
            arrayOf(sign2),
            arrayOf(sign3),
            arrayOf(vipSign)
        )
        editMessageText("网易云音乐", markup)
    }
    callback("netEaseSign") {
        val netEaseEntity = firstArg<NetEaseEntity>()
        NetEaseLogic.sign(netEaseEntity)
        delay(3000)
        NetEaseLogic.listenMusic(netEaseEntity)
        editMessageText("网易云音乐签到成功")
    }
    callback("netEaseMusicianSign1") {
        val netEaseEntity = firstArg<NetEaseEntity>()
        NetEaseLogic.publish(netEaseEntity)
        delay(3000)
//            NetEaseLogic.myMusicComment(netEaseEntity)
        editMessageText("网易云音乐人推荐任务完成成功")
    }
    callback("netEaseMusicianSign2") {
        val netEaseEntity = firstArg<NetEaseEntity>()
        NetEaseLogic.musicianSign(netEaseEntity)
        delay(3000)
        NetEaseLogic.publishAndShareMySongAndComment(netEaseEntity)
        editMessageText("网易云音乐人每日任务完成成功")
    }
    callback("netEaseMusicianSign3") {
        val netEaseEntity = firstArg<NetEaseEntity>()
        NetEaseLogic.publishMLog(netEaseEntity)
        delay(3000)
        NetEaseLogic.publishMLog(netEaseEntity)
        editMessageText("网易云音乐执行成功")
    }
    callback("netEaseVipSign") {
        NetEaseLogic.vipSign(firstArg())
        NetEaseLogic.receiveTaskReward(firstArg())
        editMessageText("网易云音乐vip签到成功")
    }
}

fun TelegramSubscribe.stepExec() {
    before { set(StepService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定任何刷步数账号")) }
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
    before { set(WeiboService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定微博账号")) }
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
    before { set(DouYuService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定斗鱼账号")) }
    callback("douYuExec") {
        val fishGroupSignButton = inlineKeyboardButton("鱼吧签到", "fishGroupSign")
        val inlineKeyboardMarkup = InlineKeyboardMarkup(arrayOf(fishGroupSignButton))
        editMessageText("斗鱼", inlineKeyboardMarkup)
    }
    callback("fishGroupSign") {
        val douYuEntity = firstArg<DouYuEntity>()
        if (douYuEntity.cookie.isEmpty()) error("未扫码登录斗鱼，无法执行鱼吧签到")
        DouYuLogic.fishGroup(douYuEntity)
        editMessageText("斗鱼鱼吧签到成功")
    }
}

fun TelegramSubscribe.smZdm() {
    before { set(SmZdmService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定什么值得买账号")) }
    callback("smZdmExec") {
        val signButton = inlineKeyboardButton("签到", "smZdmSign")
        val inlineKeyboardMarkup = InlineKeyboardMarkup(arrayOf(signButton))
        editMessageText("什么值得买", inlineKeyboardMarkup)
    }
    callback("smZdmSign") {
        val smZdmEntity = firstArg<SmZdmEntity>()
        SmZdmLogic.webSign(smZdmEntity, tgId)
        SmZdmLogic.appSign(smZdmEntity)
        editMessageText("什么值得买app与网页签到成功")
    }
}

fun TelegramSubscribe.aliDrive() {
    before { set(AliDriveService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定阿里云盘账号")) }
    callback("aliDriveExec") {
        val signButton = inlineKeyboardButton("签到", "aliDriveSign")
        val receive = inlineKeyboardButton("领取", "aliDriveReceive")
        val task = inlineKeyboardButton("完成任务", "aliDriveTask")
        val receiveTask = inlineKeyboardButton("领取任务奖励", "aliDriveReceiveTask")
        val deviceRoom = inlineKeyboardButton("时光设备间", "aliDriveDeviceRoom")
        val card = inlineKeyboardButton("领取补签卡", "aliDriveCard")
        val inlineKeyboardMarkup = InlineKeyboardMarkup(
            arrayOf(signButton), arrayOf(receive), arrayOf(task), arrayOf(receiveTask), arrayOf(deviceRoom),
            arrayOf(card)
        )
        editMessageText("""
                阿里云盘
                完成任务会在你的云盘上上传图片、视频、新建文件夹等，介意勿用
                完成任务如出现device offline错误，请找到阿里云盘的登录设备管理，下线一些设备即可
            """.trimIndent(), inlineKeyboardMarkup)
    }
    callback("aliDriveSign") {
        val entity: AliDriveEntity = firstArg()
        val res = AliDriveLogic.sign(entity)
        editMessageText(res.customMessage)
    }
    callback("aliDriveReceive") {
        editMessageText("请发送领取哪天的奖励")
        val day = nextMessage().text().toIntOrNull() ?: error("错误，不为数字")
        val result = AliDriveLogic.receive(firstArg(), day)
        editMessageText(result)
    }
    callback("aliDriveTask") {
        editMessageText("完成阿里云盘每日任务，程序正在后台为您完成任务，任务完成时间会很长")
        kotlin.runCatching {
            AliDriveLogic.finishTask(firstArg())
            sendMessage("#手动执行结果\n阿里云盘完成任务成功")
        }.onFailure {
            sendMessage("#手动执行结果\n阿里云盘完成任务失败，失败原因：${it.message}")
        }
    }
    callback("aliDriveReceiveTask") {
        editMessageText("请发送领取哪天的奖励")
        val day = nextMessage().text().toIntOrNull() ?: error("错误，不为数字")
        AliDriveLogic.signInInfo(firstArg())
        val result = AliDriveLogic.receiveTask(firstArg(), day)
        editMessageText(result)
    }
    callback("aliDriveDeviceRoom") {
        editMessageText("完成阿里云盘时光设备间，程序正在后台为您完成任务，任务完成时间会很长")
        kotlin.runCatching {
            AliDriveLogic.finishDeviceRoom(firstArg())
            sendMessage("#手动执行结果\n阿里云盘完成时光设备间成功")
        }.onFailure {
            sendMessage("#手动执行结果\n阿里云盘完成时光设备间失败，失败原因：${it.message}")
        }
    }
    callback("aliDriveCard") {
        editMessageText("领取阿里云盘补签卡，程序正在后台为您完成任务，任务完成时间会很长")
        kotlin.runCatching {
            AliDriveLogic.finishCard(firstArg())
            delay(3000)
            AliDriveLogic.receiveCard(firstArg())
            sendMessage("#手动执行结果\n阿里云盘领取补签卡成功")
        }.onFailure {
            sendMessage("#手动执行结果\n阿里云盘领取补签卡失败，失败原因：${it.message}")
        }
    }
}

fun TelegramSubscribe.nodeSeekExec() {
    before { set(NodeSeekService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定NodeSeek账号")) }
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
        NodeSeekLogic.sign(firstArg(), random)
        editMessageText("NodeSeek签到中，稍后为您自动查询结果")
        delay(1000 * 60 * 2)
        kotlin.runCatching {
            val gain = NodeSeekLogic.querySign(firstArg())
            sendMessage("#手动执行结果\nNodeSeek签到成功，获得${gain}鸡腿")
        }.onFailure {
            sendMessage("#手动执行结果\nNodeSeek签到失败，${it.message}")
        }
    }
}

fun TelegramSubscribe.glaDosExec() {
    before { set(GlaDosService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定GlaDos账号")) }
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

fun TelegramSubscribe.iqyExec() {
    before { IqyService.findByTgId(tgId).set("未绑定爱奇艺账号") }
    callback("iqyExec") {
        editMessageText("爱奇艺", InlineKeyboardMarkup(
            arrayOf(
                inlineKeyboardButton("签到", "iqySign"),
                inlineKeyboardButton("观看任务", "iqyWatch")
            )
        ))
    }
    callback("iqySign") {
        IqyLogic.taskSign(firstArg())
        editMessageText("爱奇艺签到成功")
    }
    callback("iqyWatch") {
        IqyLogic.finishTaskWatch(firstArg())
        editMessageText("爱奇艺观看任务完成成功")
    }
}

fun TelegramSubscribe.eCloudExec() {
    before { ECloudService.findByTgId(tgId).set("未绑定天翼云盘账号") }
    callback("eCloudExec") {
        editMessageText("天翼云盘", InlineKeyboardMarkup(
            inlineKeyboardButton("签到", "eCloudSign")
        ))
    }
    callback("eCloudSign") {
        ECloudLogic.sign(firstArg())
        editMessageText("天翼云盘签到成功")
    }
}