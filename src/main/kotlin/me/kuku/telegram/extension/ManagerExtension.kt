@file:Suppress("SpellCheckingInspection")

package me.kuku.telegram.extension

import me.kuku.telegram.entity.*
import me.kuku.telegram.utils.*
import org.springframework.stereotype.Service
import org.telegram.abilitybots.api.bot.BaseAbilityBot
import org.telegram.abilitybots.api.util.AbilityExtension
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
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
    private val smZdmService: SmZdmService
): AbilityExtension {

    private fun returnButton(): InlineKeyboardButton {
        return InlineKeyboardButton("返回").also { it.callbackData = "returnManager" }
    }

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
        return InlineKeyboardMarkup(listOf(
            listOf(baiduButton, biliBiliButton),
            listOf(douYuButton, hostLocButton),
            listOf(huYaButton, kuGouButton),
            listOf(miHoYoButton, netEaseButton),
            listOf(xiaomiStepButton, weiboButton),
            listOf(twitterButton, pixivButton),
            listOf(douYinButton, smZdmButton)
        ))
    }

    fun manager() = ability("manager", "管理") {
        val markup = managerKeyboardMarkup()
        val sendMessage = SendMessage()
        sendMessage.replyMarkup = markup
        sendMessage.chatId = chatId().toString()
        sendMessage.text = "请选择管理选项"
        execute(sendMessage)
    }

    fun returnMarkup() = callback("returnManager") {
        val messageId = query.message.messageId
        val editMessageText = EditMessageText()
        editMessageText.chatId = query.message.chatId.toString()
        editMessageText.replyMarkup = managerKeyboardMarkup()
        editMessageText.messageId = messageId
        editMessageText.text = "请选择管理选项"
        bot.execute(editMessageText)
    }

    private fun editBaiduMessage(bot: BaseAbilityBot, message: Message, baiduEntity: BaiduEntity) {
        val signOpenButton = InlineKeyboardButton("自动签到（开）").apply { callbackData = "baiduSignOpen" }
        val signCloseButton = InlineKeyboardButton("自动签到（关）").apply { callbackData = "baiduSignClose" }
        val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
            listOf(signOpenButton, signCloseButton),
            listOf(returnButton())
        ))
        val editMessageText = EditMessageText.builder()
            .text(
                """
            百度自动签到管理，当前状态：
            自动签到：${baiduEntity.sign.str()}
        """.trimIndent()
            ).replyMarkup(inlineKeyboardMarkup).chatId(message.chatId).messageId(message.messageId).build()
        bot.execute(editMessageText)
    }

    fun baiduManager() = callback("baiduManager") {
        val baiduEntity = baiduService.findByTgId(query.from.id) ?: error("未绑定百度账号")
        editBaiduMessage(bot, query.message, baiduEntity)
    }

    fun baiduSignOpen() = callback("baiduSignOpen") {
        val baiduEntity = baiduService.findByTgId(query.from.id)!!
        baiduEntity.sign = Status.ON
        baiduService.save(baiduEntity)
        editBaiduMessage(bot, query.message, baiduEntity)
    }

    fun baiduSignClose() = callback("baiduSignClose") {
        val baiduEntity = baiduService.findByTgId(query.from.id)!!
        baiduEntity.sign = Status.OFF
        baiduService.save(baiduEntity)
        editBaiduMessage(bot, query.message, baiduEntity)
    }

    private fun editBiliBiliMessage(bot: BaseAbilityBot, message: Message, biliEntity: BiliBiliEntity) {
        val pushOpenButton = InlineKeyboardButton("动态推送（开）").apply { callbackData = "biliBiliPushOpen" }
        val pushCloseButton = InlineKeyboardButton("动态推送（关）").apply { callbackData = "biliBiliPushClose" }
        val signOpenButton = InlineKeyboardButton("自动签到（开）").apply { callbackData = "biliBiliSignOpen" }
        val signCloseButton = InlineKeyboardButton("自动签到（关）").apply { callbackData = "biliBiliSignClose" }
        val liveOpenButton = InlineKeyboardButton("开播提醒（开）").apply { callbackData = "biliBiliLiveOpen" }
        val liveCloseButton = InlineKeyboardButton("开播提醒（关）").apply { callbackData = "biliBiliLiveClose" }
        val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
            listOf(pushOpenButton, pushCloseButton),
            listOf(signOpenButton, signCloseButton),
            listOf(liveOpenButton, liveCloseButton),
            listOf(returnButton())
        ))
        val editMessageText = EditMessageText.builder()
            .text(
                """
            哔哩哔哩自动签到管理，当前状态：
            动态推送：${biliEntity.push.str()}
            自动签到：${biliEntity.sign.str()}
            开播提醒：${biliEntity.live.str()}
        """.trimIndent()
            ).replyMarkup(inlineKeyboardMarkup).chatId(message.chatId).messageId(message.messageId).build()
        bot.execute(editMessageText)
    }

    fun biliBiliManager() = callback {
        query("biliBiliManager") {
            val biliBiliEntity = biliBiliService.findByTgId(query.from.id) ?: error("未绑定哔哩哔哩账号")
            editBiliBiliMessage(bot, query.message, biliBiliEntity)
        }
        query("biliBiliPushOpen") {
            val biliBiliEntity = biliBiliService.findByTgId(query.from.id)!!
            biliBiliEntity.push = Status.ON
            biliBiliService.save(biliBiliEntity)
            editBiliBiliMessage(bot, query.message, biliBiliEntity)
        }
        query("biliBiliPushClose") {
            val biliBiliEntity = biliBiliService.findByTgId(query.from.id)!!
            biliBiliEntity.push = Status.OFF
            biliBiliService.save(biliBiliEntity)
            editBiliBiliMessage(bot, query.message, biliBiliEntity)
        }
        query("biliBiliSignOpen") {
            val biliBiliEntity = biliBiliService.findByTgId(query.from.id)!!
            biliBiliEntity.sign = Status.ON
            biliBiliService.save(biliBiliEntity)
            editBiliBiliMessage(bot, query.message, biliBiliEntity)
        }
        query("biliBiliSignClose") {
            val biliBiliEntity = biliBiliService.findByTgId(query.from.id)!!
            biliBiliEntity.sign = Status.OFF
            biliBiliService.save(biliBiliEntity)
            editBiliBiliMessage(bot, query.message, biliBiliEntity)
        }
        query("biliBiliLiveOpen") {
            val biliBiliEntity = biliBiliService.findByTgId(query.from.id)!!
            biliBiliEntity.live = Status.ON
            biliBiliService.save(biliBiliEntity)
            editBiliBiliMessage(bot, query.message, biliBiliEntity)
        }
        query("biliBiliLiveClose") {
            val biliBiliEntity = biliBiliService.findByTgId(query.from.id)!!
            biliBiliEntity.live = Status.OFF
            biliBiliService.save(biliBiliEntity)
            editBiliBiliMessage(bot, query.message, biliBiliEntity)
        }
    }

    private fun editDouYuMessage(bot: BaseAbilityBot, message: Message, douYuEntity: DouYuEntity) {
        val liveOpenButton = InlineKeyboardButton("开播提醒（开）").apply { callbackData = "douYuLiveOpen" }
        val liveCloseButton = InlineKeyboardButton("开播提醒（关）").apply { callbackData = "douYuLiveClose" }
        val fishOpenButton = InlineKeyboardButton("鱼吧签到（开）").apply { callbackData = "douYuFishOpen" }
        val fishCloseButton = InlineKeyboardButton("鱼吧签到（关）").apply { callbackData = "douYuFishClose" }
        val fishPushOpenButton = InlineKeyboardButton("鱼吧推送（开）").apply { callbackData = "douYuFishPushOpen" }
        val fishPushCloseButton = InlineKeyboardButton("鱼吧推送（关）").apply { callbackData = "douYuFishPushClose" }
        val appSignOpenButton = inlineKeyboardButton("app签到（开）", "douYuAppSignOpen")
        val appSignCloseButton = inlineKeyboardButton("app签到（关）", "appSignCloseButton")
        val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
            listOf(liveOpenButton, liveCloseButton),
            listOf(fishOpenButton, fishCloseButton),
            listOf(fishPushOpenButton, fishPushCloseButton),
            listOf(appSignOpenButton, appSignCloseButton),
            listOf(returnButton())
        ))
        val editMessageText = EditMessageText.builder()
            .text("""
                斗鱼自动签到管理，当前状态：
                开播提醒：${douYuEntity.live.str()}
                鱼吧签到：${douYuEntity.fishGroup.str()}
                鱼吧推送：${douYuEntity.push.str()}
                app签到：${douYuEntity.appSign.str()}
            """.trimIndent()).replyMarkup(inlineKeyboardMarkup).chatId(message.chatId).messageId(message.messageId).build()
        bot.execute(editMessageText)
    }

    fun CallbackSubscriber.douYuManager() {
        before {
            val douYuEntity = douYuService.findByTgId(tgId) ?: error("未绑定斗鱼账号")
            set(douYuEntity)
        }
        "douYuManager" {}
        "douYuLiveOpen" {
            val douYuEntity = firstArg<DouYuEntity>()
            douYuEntity.live = Status.ON
        }
        "douYuLiveClose" {
            val douYuEntity = firstArg<DouYuEntity>()
            douYuEntity.live = Status.OFF
        }
        "douYuFishOpen" {
            val douYuEntity = firstArg<DouYuEntity>()
            douYuEntity.fishGroup = Status.ON
        }
        "douYuFishClose" {
            val douYuEntity = firstArg<DouYuEntity>()
            douYuEntity.fishGroup = Status.OFF
        }
        "douYuFishPushOpen" {
            val douYuEntity = firstArg<DouYuEntity>()
            douYuEntity.push = Status.ON
        }
        "douYuFishPushClose" {
            val douYuEntity = firstArg<DouYuEntity>()
            douYuEntity.push = Status.OFF
        }
        "douYuAppSignOpen" {
            firstArg<DouYuEntity>().appSign = Status.ON
        }
        "douYuAppSignClose" {
            firstArg<DouYuEntity>().appSign = Status.OFF
        }
        after {
            val douYuEntity = firstArg<DouYuEntity>()
            douYuService.save(douYuEntity)
            editDouYuMessage(bot, message, douYuEntity)
        }
    }

    private fun editHuYaMessage(bot: BaseAbilityBot, message: Message, huYaEntity: HuYaEntity) {
        val liveOpenButton = InlineKeyboardButton("开播提醒（开）").apply { callbackData = "huYaLiveOpen" }
        val liveCloseButton = InlineKeyboardButton("开播提醒（关）").apply { callbackData = "huYaLiveClose" }
        val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
            listOf(liveOpenButton, liveCloseButton),
            listOf(returnButton())
        ))
        val editMessageText = EditMessageText.builder()
            .text(
                """
            虎牙自动签到管理，当前状态：
            开播提醒：${huYaEntity.live.str()}
        """.trimIndent()
            ).replyMarkup(inlineKeyboardMarkup).chatId(message.chatId).messageId(message.messageId).build()
        bot.execute(editMessageText)
    }

    fun huYaManager() = callback{
        query("huYaManager") {
            val huYaEntity = huYaService.findByTgId(query.from.id) ?: error("未绑定虎牙账号")
            editHuYaMessage(bot, query.message, huYaEntity)
        }
        query("huYaLiveOpen") {
            val huYaEntity = huYaService.findByTgId(query.from.id)!!
            huYaEntity.live = Status.ON
            huYaService.save(huYaEntity)
            editHuYaMessage(bot, query.message, huYaEntity)
        }
        query("huYaLiveClose") {
            val huYaEntity = huYaService.findByTgId(query.from.id)!!
            huYaEntity.live = Status.OFF
            huYaService.save(huYaEntity)
            editHuYaMessage(bot, query.message, huYaEntity)
        }
    }

    private fun editHostLocMessage(bot: BaseAbilityBot, message: Message, hostLocEntity: HostLocEntity) {
        val pushOpenButton = InlineKeyboardButton("动态推送（开）").apply { callbackData = "hostLocPushOpen" }
        val pushCloseButton = InlineKeyboardButton("动态推送（关）").apply { callbackData = "hostLocPushClose" }
        val signOpenButton = InlineKeyboardButton("自动签到（开）").apply { callbackData = "hostLocSignOpen" }
        val signCloseButton = InlineKeyboardButton("自动签到（关）").apply { callbackData = "hostLocSignClose" }
        val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
            listOf(pushOpenButton, pushCloseButton),
            listOf(signOpenButton, signCloseButton),
            listOf(returnButton())
        ))
        val editMessageText = EditMessageText.builder()
            .text(
                """
            HostLoc自动签到管理，当前状态：
            动态推送：${hostLocEntity.push.str()}
            自动签到：${hostLocEntity.sign.str()}
        """.trimIndent()
            ).replyMarkup(inlineKeyboardMarkup).chatId(message.chatId).messageId(message.messageId).build()
        bot.execute(editMessageText)
    }

    fun hostLocManager() = callback{
        query("hostLocManager") {
            val hostLocEntity = hostLocService.findByTgId(query.from.id) ?: error("未绑定HostLoc账号")
            editHostLocMessage(bot, query.message, hostLocEntity)
        }
        query("hostLocPushOpen") {
            val hostLocEntity = hostLocService.findByTgId(query.from.id)!!
            hostLocEntity.push = Status.ON
            hostLocService.save(hostLocEntity)
            editHostLocMessage(bot, query.message, hostLocEntity)
        }
        query("hostLocPushClose") {
            val hostLocEntity = hostLocService.findByTgId(query.from.id)!!
            hostLocEntity.push = Status.OFF
            hostLocService.save(hostLocEntity)
            editHostLocMessage(bot, query.message, hostLocEntity)
        }
        query("hostLocSignOpen") {
            val hostLocEntity = hostLocService.findByTgId(query.from.id)!!
            hostLocEntity.sign = Status.ON
            hostLocService.save(hostLocEntity)
            editHostLocMessage(bot, query.message, hostLocEntity)
        }
        query("hostLocSignClose") {
            val hostLocEntity = hostLocService.findByTgId(query.from.id)!!
            hostLocEntity.sign = Status.OFF
            hostLocService.save(hostLocEntity)
            editHostLocMessage(bot, query.message, hostLocEntity)
        }
    }

    private fun editKuGouMessage(bot: BaseAbilityBot, message: Message, kuGouEntity: KuGouEntity) {
        val signOpenButton = InlineKeyboardButton("自动签到（开）").apply { callbackData = "kuGouSignOpen" }
        val signCloseButton = InlineKeyboardButton("自动签到（关）").apply { callbackData = "kuGouSignClose" }
        val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
            listOf(signOpenButton, signCloseButton),
            listOf(returnButton())
        ))
        val editMessageText = EditMessageText.builder()
            .text(
                """
            酷狗自动签到管理，当前状态：
            自动签到：${kuGouEntity.sign.str()}
        """.trimIndent()
            ).replyMarkup(inlineKeyboardMarkup).chatId(message.chatId).messageId(message.messageId).build()
        bot.execute(editMessageText)
    }

    fun kuGouManager() = callback{
        query("kuGouManager") {
            val kuGouEntity = kuGouService.findByTgId(query.from.id) ?: error("未绑定酷狗账号")
            editKuGouMessage(bot, query.message, kuGouEntity)
        }
        query("kuGouSignOpen") {
            val kuGouEntity = kuGouService.findByTgId(query.from.id)!!
            kuGouEntity.sign = Status.ON
            kuGouService.save(kuGouEntity)
            editKuGouMessage(bot, query.message, kuGouEntity)
        }
        query("kuGouSignClose") {
            val kuGouEntity = kuGouService.findByTgId(query.from.id)!!
            kuGouEntity.sign = Status.OFF
            kuGouService.save(kuGouEntity)
            editKuGouMessage(bot, query.message, kuGouEntity)
        }
    }

    private fun editMessage(bot: BaseAbilityBot, message: Message, miHoYoEntity: MiHoYoEntity) {
        val signOpenButton = InlineKeyboardButton("自动签到（开）").apply { callbackData = "miHoYoSignOpen" }
        val signCloseButton = InlineKeyboardButton("自动签到（关）").apply { callbackData = "miHoYoSignClose" }
        val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
            listOf(signOpenButton, signCloseButton),
            listOf(returnButton())
        ))
        val editMessageText = EditMessageText.builder()
            .text(
                """
            米哈游（原神）签到管理，当前状态：
            自动签到：${miHoYoEntity.sign.str()}
        """.trimIndent()
            ).replyMarkup(inlineKeyboardMarkup).chatId(message.chatId).messageId(message.messageId).build()
        bot.execute(editMessageText)
    }

    fun miHoYoManager() = callback{
        query("miHoYoManager") {
            val miHoYoEntity = miHoYoService.findByTgId(query.from.id) ?: error("未绑定酷狗账号")
            editMessage(bot, query.message, miHoYoEntity)
        }
        query("miHoYoSignOpen") {
            val miHoYoEntity = miHoYoService.findByTgId(query.from.id)!!
            miHoYoEntity.sign = Status.ON
            miHoYoService.save(miHoYoEntity)
            editMessage(bot, query.message, miHoYoEntity)
        }
        query("miHoYoSignClose") {
            val miHoYoEntity = miHoYoService.findByTgId(query.from.id)!!
            miHoYoEntity.sign = Status.OFF
            miHoYoService.save(miHoYoEntity)
            editMessage(bot, query.message, miHoYoEntity)
        }
    }

    private fun editMessage(bot: BaseAbilityBot, message: Message, netEaseEntity: NetEaseEntity) {
        val signOpenButton = InlineKeyboardButton("自动签到（开）").apply { callbackData = "netEaseSignOpen" }
        val signCloseButton = InlineKeyboardButton("自动签到（关）").apply { callbackData = "netEaseSignClose" }
        val musicianSignOpenButton = InlineKeyboardButton("音乐人自动签到（开）").apply { callbackData = "netEaseMusicianSignOpen" }
        val musicianSignCloseButton = InlineKeyboardButton("音乐人自动签到（关）").apply { callbackData = "netEaseMusicianSignClose" }
        val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
            listOf(signOpenButton, signCloseButton),
            listOf(musicianSignOpenButton, musicianSignCloseButton),
            listOf(returnButton())
        ))
        val editMessageText = EditMessageText.builder()
            .text(
                """
            网易云签到管理，当前状态：
            自动签到：${netEaseEntity.sign.str()}
            音乐人自动签到：${netEaseEntity.musicianSign.str()}
        """.trimIndent()
            ).replyMarkup(inlineKeyboardMarkup).chatId(message.chatId).messageId(message.messageId).build()
        bot.execute(editMessageText)
    }

    fun netEaseManager() = callback{
        query("netEaseManager") {
            val miHoYoEntity = netEaseService.findByTgId(query.from.id) ?: error("未绑定酷狗账号")
            editMessage(bot, query.message, miHoYoEntity)
        }
        query("netEaseSignOpen") {
            val netEaseEntity = netEaseService.findByTgId(query.from.id)!!
            netEaseEntity.sign = Status.ON
            netEaseService.save(netEaseEntity)
            editMessage(bot, query.message, netEaseEntity)
        }
        query("netEaseSignClose") {
            val netEaseEntity = netEaseService.findByTgId(query.from.id)!!
            netEaseEntity.sign = Status.OFF
            netEaseService.save(netEaseEntity)
            editMessage(bot, query.message, netEaseEntity)
        }
        query("netEaseMusicianSignOpen") {
            val netEaseEntity = netEaseService.findByTgId(query.from.id)!!
            netEaseEntity.musicianSign = Status.ON
            netEaseService.save(netEaseEntity)
            editMessage(bot, query.message, netEaseEntity)
        }
        query("netEaseMusicianSignClose") {
            val netEaseEntity = netEaseService.findByTgId(query.from.id)!!
            netEaseEntity.musicianSign = Status.OFF
            netEaseService.save(netEaseEntity)
            editMessage(bot, query.message, netEaseEntity)
        }
    }

    private fun editMessage(bot: BaseAbilityBot, message: Message, stepEntity: StepEntity) {
        val modifyStepButton = InlineKeyboardButton("修改步数").apply { callbackData = "modifyStep" }
        val stepOffsetOpenButton = InlineKeyboardButton("步数偏移（开）").apply { callbackData = "stepOffsetOpen" }
        val stepOffsetCloseButton = InlineKeyboardButton("步数偏移（关）").apply { callbackData = "stepOffsetClose" }
        val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
            listOf(modifyStepButton),
            listOf(stepOffsetOpenButton, stepOffsetCloseButton),
            listOf(returnButton())
        ))
        val editMessageText = EditMessageText.builder()
            .text(
                """
            刷步数管理，当前状态：
            自动步数：${stepEntity.step} (小于0为关闭自动刷步数)
            步数偏移：${stepEntity.offset.str()} （开启则会在设置的自动步数范围中随机修改）
        """.trimIndent()
            ).replyMarkup(inlineKeyboardMarkup).chatId(message.chatId).messageId(message.messageId).build()
        bot.execute(editMessageText)
    }

    fun stepManager() = callback{
        query("stepManager") {
            val stepEntity = stepService.findByTgId(query.from.id) ?: error("未绑定刷步数的账号")
            editMessage(bot, query.message, stepEntity)
        }
        query("modifyStep") {
            val chatId = query.message.chatId
            val stepEntity = stepService.findByTgId(query.from.id)!!
            bot.execute(SendMessage(chatId.toString(), "请发送需要修改的步数"))
            val step = query.waitNextMessage().text
            stepEntity.step = step.toIntOrNull() ?: -1
            stepService.save(stepEntity)
            editMessage(bot, query.message, stepEntity)
        }
        query("stepOffsetOpen") {
            val stepEntity = stepService.findByTgId(query.from.id)!!
            stepEntity.offset = Status.ON
            stepService.save(stepEntity)
            editMessage(bot, query.message, stepEntity)
        }
        query("stepOffsetClose") {
            val stepEntity = stepService.findByTgId(query.from.id)!!
            stepEntity.offset = Status.OFF
            stepService.save(stepEntity)
            editMessage(bot, query.message, stepEntity)
        }
    }

    private fun editMessage(bot: BaseAbilityBot, message: Message, weiboEntity: WeiboEntity) {
        val pushOpenButton = InlineKeyboardButton("动态推送（开）").apply { callbackData = "weiboPushOpen" }
        val pushCloseButton = InlineKeyboardButton("动态推送（关）").apply { callbackData = "weiboPushClose" }
        val signOpenButton = InlineKeyboardButton("自动签到（开）").apply { callbackData = "weiboSignOpen" }
        val signCloseButton = InlineKeyboardButton("自动签到（关）").apply { callbackData = "weiboSignClose" }
        val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
            listOf(pushOpenButton, pushCloseButton),
            listOf(signOpenButton, signCloseButton),
            listOf(returnButton())
        ))
        val editMessageText = EditMessageText.builder()
            .text(
                """
            微博自动签到管理，当前状态：
            动态推送：${weiboEntity.push.str()}
            自动签到：${weiboEntity.sign.str()}
        """.trimIndent()
            ).replyMarkup(inlineKeyboardMarkup).chatId(message.chatId).messageId(message.messageId).build()
        bot.execute(editMessageText)
    }

    fun weiboManager() = callback{
        query("weiboManager") {
            val weiboEntity = weiboService.findByTgId(query.from.id) ?: error("未绑定HostLoc账号")
            editMessage(bot, query.message, weiboEntity)
        }
        query("weiboPushOpen") {
            val weiboEntity = weiboService.findByTgId(query.from.id)!!
            weiboEntity.push = Status.ON
            weiboService.save(weiboEntity)
            editMessage(bot, query.message, weiboEntity)
        }
        query("weiboPushClose") {
            val weiboEntity = weiboService.findByTgId(query.from.id)!!
            weiboEntity.push = Status.OFF
            weiboService.save(weiboEntity)
            editMessage(bot, query.message, weiboEntity)
        }
        query("weiboSignOpen") {
            val weiboEntity = weiboService.findByTgId(query.from.id)!!
            weiboEntity.sign = Status.ON
            weiboService.save(weiboEntity)
            editMessage(bot, query.message, weiboEntity)
        }
        query("weiboSignClose") {
            val weiboEntity = weiboService.findByTgId(query.from.id)!!
            weiboEntity.sign = Status.OFF
            weiboService.save(weiboEntity)
            editMessage(bot, query.message, weiboEntity)
        }
    }

    private fun editMessage(bot: BaseAbilityBot, message: Message, twitterEntity: TwitterEntity) {
        val pushOpenButton = InlineKeyboardButton("推文推送（开）").apply { callbackData = "twitterPushOpen" }
        val pushCloseButton = InlineKeyboardButton("推文推送（关）").apply { callbackData = "twitterPushClose" }
        val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
            listOf(pushOpenButton, pushCloseButton),
            listOf(returnButton())
        ))
        val editMessageText = EditMessageText.builder()
            .text(
                """
            推特管理，当前状态：
            推文推送：${twitterEntity.push.str()}
        """.trimIndent()
            ).replyMarkup(inlineKeyboardMarkup).chatId(message.chatId).messageId(message.messageId).build()
        bot.execute(editMessageText)
    }

    fun twitterManager() = callback {
        query("twitterManager") {
            val twitterEntity = twitterService.findByTgId(query.from.id) ?: error("未绑定twitter账号")
            editMessage(bot, query.message, twitterEntity)
        }
        query("twitterPushOpen") {
            val twitterEntity = twitterService.findByTgId(query.from.id)!!
            twitterEntity.push = Status.ON
            twitterService.save(twitterEntity)
            editMessage(bot, query.message, twitterEntity)
        }
        query("twitterPushClose") {
            val twitterEntity = twitterService.findByTgId(query.from.id)!!
            twitterEntity.push = Status.OFF
            twitterService.save(twitterEntity)
            editMessage(bot, query.message, twitterEntity)
        }
    }

    private fun editMessage(bot: BaseAbilityBot, message: Message, pixivEntity: PixivEntity) {
        val pushOpenButton = InlineKeyboardButton("插画推送（开）").apply { callbackData = "pixivPushOpen" }
        val pushCloseButton = InlineKeyboardButton("插画推送（关）").apply { callbackData = "pixivPushClose" }
        val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
            listOf(pushOpenButton, pushCloseButton),
            listOf(returnButton())
        ))
        val editMessageText = EditMessageText.builder()
            .text(
                """
            pixiv管理，当前状态：
            插画推送：${pixivEntity.push.str()}
        """.trimIndent()
            ).replyMarkup(inlineKeyboardMarkup).chatId(message.chatId).messageId(message.messageId).build()
        bot.execute(editMessageText)
    }

    fun pixivManager() = callback {
        query("pixivManager") {
            val pixivEntity = pixivService.findByTgId(query.from.id) ?: error("未绑定pixiv账号")
            editMessage(bot, query.message, pixivEntity)
        }
        query("pixivPushOpen") {
            val pixivEntity = pixivService.findByTgId(query.from.id)!!
            pixivEntity.push = Status.ON
            pixivService.save(pixivEntity)
            editMessage(bot, query.message, pixivEntity)
        }
        query("pixivPushClose") {
            val pixivEntity = pixivService.findByTgId(query.from.id)!!
            pixivEntity.push = Status.OFF
            pixivService.save(pixivEntity)
            editMessage(bot, query.message, pixivEntity)
        }
    }

    private fun editMessage(bot: BaseAbilityBot, message: Message, douYinEntity: DouYinEntity) {
        val pushOpenButton = inlineKeyboardButton("视频推送（开）", "douYinPushOpen")
        val pushCloseButton = inlineKeyboardButton("视频推送（关）", "douYinPushClose")
        val editMessageText = EditMessageText.builder().chatId(message.chatId).messageId(message.messageId)
            .text("""
                抖音管理，当前状态：
                视频推送：${douYinEntity.push.str()}
            """.trimIndent()).replyMarkup(InlineKeyboardMarkup(listOf(listOf(pushOpenButton, pushCloseButton), listOf(returnButton()))))
            .build()
        bot.execute(editMessageText)
    }

    fun douYinManager() = callback {
        "douYinManager" {
            val douYinEntity = douYinService.findByTgId(tgId) ?: error("未绑定抖音账号")
            editMessage(bot, message, douYinEntity)
        }
        "douYinPushOpen" {
            val douYinEntity = douYinService.findByTgId(tgId)!!
            douYinEntity.push = Status.ON
            douYinService.save(douYinEntity)
            editMessage(bot, message, douYinEntity)
        }
        "douYinPushClose" {
            val douYinEntity = douYinService.findByTgId(tgId)!!
            douYinEntity.push = Status.OFF
            douYinService.save(douYinEntity)
            editMessage(bot, message, douYinEntity)
        }
    }

    private fun editMessage(bot: BaseAbilityBot, message: Message, smZdmEntity: SmZdmEntity) {
        val signOpenButton = inlineKeyboardButton("自动签到（开）", "smZdmSignOpen")
        val signCloseButton = inlineKeyboardButton("自动签到（关）", "smZdmSignClose")
        val editMessageText = EditMessageText.builder().chatId(message.chatId).messageId(message.messageId)
            .text("""
                什么值得买管理，当前状态：
                签到：${smZdmEntity.sign.str()}
            """.trimIndent()).replyMarkup(InlineKeyboardMarkup(listOf(listOf(signOpenButton, signCloseButton), listOf(returnButton()))))
            .build()
        bot.execute(editMessageText)
    }

    fun CallbackSubscriber.smZdm() {
        before {
            val smZdmEntity = smZdmService.findByTgId(tgId) ?: error("未绑定什么值得买账号")
            set(smZdmEntity)
        }
        "smZdmManager" {
            val smZdmEntity = firstArg<SmZdmEntity>()
            editMessage(bot, message, smZdmEntity)
        }
        "smZdmSignOpen" {
            val smZdmEntity = firstArg<SmZdmEntity>()
            smZdmEntity.sign = Status.ON
            smZdmService.save(smZdmEntity)
            editMessage(bot, message, smZdmEntity)
        }
        "smZdmSignClose" {
            val smZdmEntity = firstArg<SmZdmEntity>()
            smZdmEntity.sign = Status.OFF
            smZdmService.save(smZdmEntity)
            editMessage(bot, message, smZdmEntity)
        }
    }



}
