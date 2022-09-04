package me.kuku.telegram.extension

import me.kuku.telegram.entity.*
import me.kuku.telegram.utils.ability
import me.kuku.telegram.utils.callback
import me.kuku.telegram.utils.execute
import me.kuku.telegram.utils.waitNextMessage
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
    private val weiboService: WeiboService
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
        val weiboStepButton = InlineKeyboardButton("微博").also { it.callbackData = "weiboManager" }
        return InlineKeyboardMarkup(listOf(
            listOf(baiduButton, biliBiliButton),
            listOf(douYuButton, hostLocButton),
            listOf(huYaButton, kuGouButton),
            listOf(miHoYoButton, netEaseButton),
            listOf(xiaomiStepButton, weiboStepButton)
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
        val messageId = it.message.messageId
        val editMessageText = EditMessageText()
        editMessageText.chatId = it.message.chatId.toString()
        editMessageText.replyMarkup = managerKeyboardMarkup()
        editMessageText.messageId = messageId
        editMessageText.text = "请选择管理选项"
        execute(editMessageText)
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
        val chatId = it.message.chatId
        val baiduEntity = baiduService.findByTgId(chatId) ?: error("未绑定百度账号")
        editBaiduMessage(this, it.message, baiduEntity)
    }

    fun baiduSignOpen() = callback("baiduSignOpen") {
        val chatId = it.message.chatId
        val baiduEntity = baiduService.findByTgId(chatId)!!
        baiduEntity.sign = Status.ON
        baiduService.save(baiduEntity)
        editBaiduMessage(this, it.message, baiduEntity)
    }

    fun baiduSignClose() = callback("baiduSignClose") {
        val chatId = it.message.chatId
        val baiduEntity = baiduService.findByTgId(chatId)!!
        baiduEntity.sign = Status.OFF
        baiduService.save(baiduEntity)
        editBaiduMessage(this, it.message, baiduEntity)
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
            val chatId = it.message.chatId
            val biliBiliEntity = biliBiliService.findByTgId(chatId) ?: error("未绑定哔哩哔哩账号")
            editBiliBiliMessage(this, it.message, biliBiliEntity)
        }
        query("biliBiliPushOpen") {
            val chatId = it.message.chatId
            val biliBiliEntity = biliBiliService.findByTgId(chatId)!!
            biliBiliEntity.push = Status.ON
            biliBiliService.save(biliBiliEntity)
            editBiliBiliMessage(this, it.message, biliBiliEntity)
        }
        query("biliBiliPushClose") {
            val chatId = it.message.chatId
            val biliBiliEntity = biliBiliService.findByTgId(chatId)!!
            biliBiliEntity.push = Status.OFF
            biliBiliService.save(biliBiliEntity)
            editBiliBiliMessage(this, it.message, biliBiliEntity)
        }
        query("biliBiliSignOpen") {
            val chatId = it.message.chatId
            val biliBiliEntity = biliBiliService.findByTgId(chatId)!!
            biliBiliEntity.sign = Status.ON
            biliBiliService.save(biliBiliEntity)
            editBiliBiliMessage(this, it.message, biliBiliEntity)
        }
        query("biliBiliSignClose") {
            val chatId = it.message.chatId
            val biliBiliEntity = biliBiliService.findByTgId(chatId)!!
            biliBiliEntity.sign = Status.OFF
            biliBiliService.save(biliBiliEntity)
            editBiliBiliMessage(this, it.message, biliBiliEntity)
        }
        query("biliBiliLiveOpen") {
            val chatId = it.message.chatId
            val biliBiliEntity = biliBiliService.findByTgId(chatId)!!
            biliBiliEntity.live = Status.ON
            biliBiliService.save(biliBiliEntity)
            editBiliBiliMessage(this, it.message, biliBiliEntity)
        }
        query("biliBiliLiveClose") {
            val chatId = it.message.chatId
            val biliBiliEntity = biliBiliService.findByTgId(chatId)!!
            biliBiliEntity.live = Status.OFF
            biliBiliService.save(biliBiliEntity)
            editBiliBiliMessage(this, it.message, biliBiliEntity)
        }
    }

    private fun editDouYuMessage(bot: BaseAbilityBot, message: Message, douYuEntity: DouYuEntity) {
        val liveOpenButton = InlineKeyboardButton("开播提醒（开）").apply { callbackData = "douYuLiveOpen" }
        val liveCloseButton = InlineKeyboardButton("开播提醒（关）").apply { callbackData = "douYuLiveClose" }
        val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
            listOf(liveOpenButton, liveCloseButton),
            listOf(returnButton())
        ))
        val editMessageText = EditMessageText.builder()
            .text(
                """
            斗鱼自动签到管理，当前状态：
            开播提醒：${douYuEntity.live.str()}
        """.trimIndent()
            ).replyMarkup(inlineKeyboardMarkup).chatId(message.chatId).messageId(message.messageId).build()
        bot.execute(editMessageText)
    }

    fun douYuManager() = callback{
        query("douYuManager") {
            val chatId = it.message.chatId
            val douYuEntity = douYuService.findByTgId(chatId) ?: error("未绑定斗鱼账号")
            editDouYuMessage(this, it.message, douYuEntity)
        }
        query("douYuLiveOpen") {
            val chatId = it.message.chatId
            val douYuEntity = douYuService.findByTgId(chatId)!!
            douYuEntity.live = Status.ON
            douYuService.save(douYuEntity)
            editDouYuMessage(this, it.message, douYuEntity)
        }
        query("douYuLiveClose") {
            val chatId = it.message.chatId
            val douYuEntity = douYuService.findByTgId(chatId)!!
            douYuEntity.live = Status.OFF
            douYuService.save(douYuEntity)
            editDouYuMessage(this, it.message, douYuEntity)
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
            val chatId = it.message.chatId
            val huYaEntity = huYaService.findByTgId(chatId) ?: error("未绑定虎牙账号")
            editHuYaMessage(this, it.message, huYaEntity)
        }
        query("huYaLiveOpen") {
            val chatId = it.message.chatId
            val huYaEntity = huYaService.findByTgId(chatId)!!
            huYaEntity.live = Status.ON
            huYaService.save(huYaEntity)
            editHuYaMessage(this, it.message, huYaEntity)
        }
        query("huYaLiveClose") {
            val chatId = it.message.chatId
            val huYaEntity = huYaService.findByTgId(chatId)!!
            huYaEntity.live = Status.OFF
            huYaService.save(huYaEntity)
            editHuYaMessage(this, it.message, huYaEntity)
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
            val chatId = it.message.chatId
            val hostLocEntity = hostLocService.findByTgId(chatId) ?: error("未绑定HostLoc账号")
            editHostLocMessage(this, it.message, hostLocEntity)
        }
        query("hostLocPushOpen") {
            val chatId = it.message.chatId
            val hostLocEntity = hostLocService.findByTgId(chatId)!!
            hostLocEntity.push = Status.ON
            hostLocService.save(hostLocEntity)
            editHostLocMessage(this, it.message, hostLocEntity)
        }
        query("hostLocPushClose") {
            val chatId = it.message.chatId
            val hostLocEntity = hostLocService.findByTgId(chatId)!!
            hostLocEntity.push = Status.OFF
            hostLocService.save(hostLocEntity)
            editHostLocMessage(this, it.message, hostLocEntity)
        }
        query("hostLocSignOpen") {
            val chatId = it.message.chatId
            val hostLocEntity = hostLocService.findByTgId(chatId)!!
            hostLocEntity.sign = Status.ON
            hostLocService.save(hostLocEntity)
            editHostLocMessage(this, it.message, hostLocEntity)
        }
        query("hostLocSignClose") {
            val chatId = it.message.chatId
            val hostLocEntity = hostLocService.findByTgId(chatId)!!
            hostLocEntity.sign = Status.OFF
            hostLocService.save(hostLocEntity)
            editHostLocMessage(this, it.message, hostLocEntity)
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
            val chatId = it.message.chatId
            val kuGouEntity = kuGouService.findByTgId(chatId) ?: error("未绑定酷狗账号")
            editKuGouMessage(this, it.message, kuGouEntity)
        }
        query("kuGouSignOpen") {
            val chatId = it.message.chatId
            val kuGouEntity = kuGouService.findByTgId(chatId)!!
            kuGouEntity.sign = Status.ON
            kuGouService.save(kuGouEntity)
            editKuGouMessage(this, it.message, kuGouEntity)
        }
        query("kuGouSignClose") {
            val chatId = it.message.chatId
            val kuGouEntity = kuGouService.findByTgId(chatId)!!
            kuGouEntity.sign = Status.OFF
            kuGouService.save(kuGouEntity)
            editKuGouMessage(this, it.message, kuGouEntity)
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
            val chatId = it.message.chatId
            val miHoYoEntity = miHoYoService.findByTgId(chatId) ?: error("未绑定酷狗账号")
            editMessage(this, it.message, miHoYoEntity)
        }
        query("miHoYoSignOpen") {
            val chatId = it.message.chatId
            val miHoYoEntity = miHoYoService.findByTgId(chatId)!!
            miHoYoEntity.sign = Status.ON
            miHoYoService.save(miHoYoEntity)
            editMessage(this, it.message, miHoYoEntity)
        }
        query("miHoYoSignClose") {
            val chatId = it.message.chatId
            val miHoYoEntity = miHoYoService.findByTgId(chatId)!!
            miHoYoEntity.sign = Status.OFF
            miHoYoService.save(miHoYoEntity)
            editMessage(this, it.message, miHoYoEntity)
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
            val chatId = it.message.chatId
            val miHoYoEntity = netEaseService.findByTgId(chatId) ?: error("未绑定酷狗账号")
            editMessage(this, it.message, miHoYoEntity)
        }
        query("netEaseSignOpen") {
            val chatId = it.message.chatId
            val netEaseEntity = netEaseService.findByTgId(chatId)!!
            netEaseEntity.sign = Status.ON
            netEaseService.save(netEaseEntity)
            editMessage(this, it.message, netEaseEntity)
        }
        query("netEaseSignClose") {
            val chatId = it.message.chatId
            val netEaseEntity = netEaseService.findByTgId(chatId)!!
            netEaseEntity.sign = Status.OFF
            netEaseService.save(netEaseEntity)
            editMessage(this, it.message, netEaseEntity)
        }
        query("netEaseMusicianSignOpen") {
            val chatId = it.message.chatId
            val netEaseEntity = netEaseService.findByTgId(chatId)!!
            netEaseEntity.musicianSign = Status.ON
            netEaseService.save(netEaseEntity)
            editMessage(this, it.message, netEaseEntity)
        }
        query("netEaseMusicianSignClose") {
            val chatId = it.message.chatId
            val netEaseEntity = netEaseService.findByTgId(chatId)!!
            netEaseEntity.musicianSign = Status.OFF
            netEaseService.save(netEaseEntity)
            editMessage(this, it.message, netEaseEntity)
        }
    }

    private fun editMessage(bot: BaseAbilityBot, message: Message, stepEntity: StepEntity) {
        val modifyStepButton = InlineKeyboardButton("修改步数").apply { callbackData = "modifyStep" }
        val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
            listOf(modifyStepButton),
            listOf(returnButton())
        ))
        val editMessageText = EditMessageText.builder()
            .text(
                """
            刷步数管理，当前状态：
            自动步数：${stepEntity.step} (小于0为关闭自动刷步数)
        """.trimIndent()
            ).replyMarkup(inlineKeyboardMarkup).chatId(message.chatId).messageId(message.messageId).build()
        bot.execute(editMessageText)
    }

    fun stepManager() = callback{
        query("stepManager") {
            val chatId = it.message.chatId
            val stepEntity = stepService.findByTgId(chatId) ?: error("未绑定刷步数的账号")
            editMessage(this, it.message, stepEntity)
        }
        query("modifyStep") {
            val chatId = it.message.chatId
            val stepEntity = stepService.findByTgId(chatId)!!
            execute(SendMessage(chatId.toString(), "请发送需要修改的步数"))
            val step = it.waitNextMessage().text
            stepEntity.step = step.toIntOrNull() ?: -1
            stepService.save(stepEntity)
            editMessage(this, it.message, stepEntity)
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
            val chatId = it.message.chatId
            val weiboEntity = weiboService.findByTgId(chatId) ?: error("未绑定HostLoc账号")
            editMessage(this, it.message, weiboEntity)
        }
        query("weiboPushOpen") {
            val chatId = it.message.chatId
            val weiboEntity = weiboService.findByTgId(chatId)!!
            weiboEntity.push = Status.ON
            weiboService.save(weiboEntity)
            editMessage(this, it.message, weiboEntity)
        }
        query("weiboPushClose") {
            val chatId = it.message.chatId
            val weiboEntity = weiboService.findByTgId(chatId)!!
            weiboEntity.push = Status.OFF
            weiboService.save(weiboEntity)
            editMessage(this, it.message, weiboEntity)
        }
        query("weiboSignOpen") {
            val chatId = it.message.chatId
            val weiboEntity = weiboService.findByTgId(chatId)!!
            weiboEntity.sign = Status.ON
            weiboService.save(weiboEntity)
            editMessage(this, it.message, weiboEntity)
        }
        query("weiboSignClose") {
            val chatId = it.message.chatId
            val weiboEntity = weiboService.findByTgId(chatId)!!
            weiboEntity.sign = Status.OFF
            weiboService.save(weiboEntity)
            editMessage(this, it.message, weiboEntity)
        }
    }



}