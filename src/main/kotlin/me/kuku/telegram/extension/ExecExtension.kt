package me.kuku.telegram.extension

import kotlinx.coroutines.delay
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.*
import me.kuku.telegram.utils.ability
import me.kuku.telegram.utils.callback
import me.kuku.telegram.utils.execute
import me.kuku.telegram.utils.waitNextMessage
import org.springframework.stereotype.Service
import org.telegram.abilitybots.api.util.AbilityExtension
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
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
        return InlineKeyboardMarkup(listOf(
            listOf(baiduButton, biliBiliButton),
            listOf(hostLocButton, kuGouButton),
            listOf(miHoYoButton, netEaseButton),
            listOf(stepButton, weiboButton),
        ))
    }

    private fun returnButton(): List<InlineKeyboardButton> {
        return listOf(InlineKeyboardButton("返回").apply { callbackData = "returnExec" })
    }

    fun returnMarkup() = callback("returnExec") {
        val messageId = it.message.messageId
        val editMessageText = EditMessageText()
        editMessageText.chatId = it.message.chatId.toString()
        editMessageText.replyMarkup = execKeyboardMarkup()
        editMessageText.messageId = messageId
        editMessageText.text = "请选择手动执行选项"
        execute(editMessageText)
    }

    fun exec() = ability("exec", "手动执行") {
        val markup = execKeyboardMarkup()
        val sendMessage = SendMessage()
        sendMessage.replyMarkup = markup
        sendMessage.chatId = chatId().toString()
        sendMessage.text = "请选择手动执行选项"
        execute(sendMessage)
    }

    fun baiduExec() = callback {
        query("baiduExec") {
            val chatId = it.message.chatId
            baiduService.findByTgId(chatId) ?: error("未绑定百度账号")
            val tieBaSignButton = InlineKeyboardButton("贴吧签到").apply { callbackData = "tieBaSign" }
            val ybbSignButton = InlineKeyboardButton("游帮帮加速器签到").apply { callbackData = "ybbSign" }
            val ybbWatchAdButton = InlineKeyboardButton("游帮帮加速器看广告").apply { callbackData = "ybbWatchAd" }
            val markup = InlineKeyboardMarkup(listOf(listOf(tieBaSignButton), listOf(ybbSignButton), listOf(ybbWatchAdButton), returnButton()))
            val editMessage = EditMessageText.builder().chatId(chatId).messageId(it.message.messageId).text("百度").replyMarkup(markup).build()
            execute(editMessage)
        }
        query("tieBaSign") {
            val chatId = it.message.chatId
            val baiduEntity = baiduService.findByTgId(chatId)!!
            val ss = baiduLogic.tieBaSign(baiduEntity)
            val sendMessage = SendMessage.builder().text(ss.message).chatId(chatId).build()
            execute(sendMessage)
        }
        query("ybbSign") {
            val chatId = it.message.chatId
            val baiduEntity = baiduService.findByTgId(chatId)!!
            val result = baiduLogic.ybbSign(baiduEntity)
            val sendMessage = SendMessage.builder().text(result.message).chatId(chatId).build()
            execute(sendMessage)
        }
        query("ybbWatchAd") {
            val chatId = it.message.chatId
            val baiduEntity = baiduService.findByTgId(chatId)!!
            val res = baiduLogic.ybbWatchAd(baiduEntity, "v3")
            val sendMessage = EditMessageText.builder().text(res.message).chatId(chatId).build()
            execute(sendMessage)
        }
    }

    fun biliBiliExec() = callback {
        query("biliBiliExec") {
            val chatId = it.message.chatId
            biliBiliService.findByTgId(chatId) ?: error("未绑定哔哩哔哩账号")
            val biliBiliSignButton = InlineKeyboardButton("签到").apply { callbackData = "biliBiliSign" }
            val markup = InlineKeyboardMarkup(listOf(listOf(biliBiliSignButton), returnButton()))
            val editMessage = EditMessageText.builder().chatId(chatId).messageId(it.message.messageId).text("哔哩哔哩").replyMarkup(markup).build()
            execute(editMessage)
        }
        query("biliBiliSign") {
            val chatId = it.message.chatId
            val biliBiliEntity = biliBiliService.findByTgId(chatId)!!
            val firstRank = BiliBiliLogic.ranking()[0]
            val res = BiliBiliLogic.report(biliBiliEntity, firstRank.aid, firstRank.cid, 300)
            val sendMessage = SendMessage().also { message -> message.chatId = chatId.toString() }
            if (res.failure()) {
                sendMessage.text = res.message
                execute(sendMessage)
            } else {
                BiliBiliLogic.share(biliBiliEntity, firstRank.aid)
                BiliBiliLogic.liveSign(biliBiliEntity)
                sendMessage.text = "哔哩哔哩签到成功"
                execute(sendMessage)
            }

        }
    }

    fun hostLocExec() = callback {
        query("hostLocExec") {
            val chatId = it.message.chatId
            hostLocService.findByTgId(chatId) ?: error("未绑定HostLoc账号")
            val hostLocSignButton = InlineKeyboardButton("签到").apply { callbackData = "hostLocSign" }
            val markup = InlineKeyboardMarkup(listOf(listOf(hostLocSignButton), returnButton()))
            val editMessage = EditMessageText.builder().chatId(chatId).messageId(it.message.messageId).text("HostLoc").replyMarkup(markup).build()
            execute(editMessage)
        }
        query("hostLocSign") {
            val chatId = it.message.chatId
            val hostLocEntity = hostLocService.findByTgId(chatId)!!
            val sendMessage = SendMessage.builder().chatId(chatId).text("HostLoc签到后台进行中").build()
            execute(sendMessage)
            HostLocLogic.sign(hostLocEntity.cookie)
        }
    }

    fun kuGouExec() = callback {
        query("kuGouExec") {
            val chatId = it.message.chatId
            kuGouService.findByTgId(chatId) ?: error("未绑定酷狗账号")
            val kuGouMusicianSignButton = InlineKeyboardButton("音乐人").apply { callbackData = "kuGouMusicianSign" }
            val kuGouListenButton = InlineKeyboardButton("概念版听歌得vip").apply { callbackData = "kuGouListen" }
            val markup = InlineKeyboardMarkup(listOf(listOf(kuGouMusicianSignButton), listOf(kuGouListenButton), returnButton()))
            val editMessageText = EditMessageText.builder().chatId(chatId).messageId(it.message.messageId).text("酷狗").replyMarkup(markup).build()
            execute(editMessageText)
        }
        query("kuGouMusicianSign") {
            val chatId = it.message.chatId
            val kuGouEntity = kuGouService.findByTgId(chatId)!!
            val res = kuGouLogic.musicianSign(kuGouEntity)
            val sendMessage = SendMessage.builder().chatId(chatId).text(res.message).build()
            execute(sendMessage)
        }
        query("kuGouListen") {
            val chatId = it.message.chatId
            val kuGouEntity = kuGouService.findByTgId(chatId)!!
            val res = kuGouLogic.listenMusic(kuGouEntity)
            val sendMessage = SendMessage.builder().chatId(chatId).text(res.message).build()
            execute(sendMessage)
        }
    }

    fun miHoYoExec() = callback {
        query("miHoYoExec") {
            val chatId = it.message.chatId
            miHoYoService.findByTgId(chatId) ?: error("未绑定米哈游账号")
            val genShinSignButton = InlineKeyboardButton("原神签到").apply { callbackData = "genShinSign" }
            val markup = InlineKeyboardMarkup(listOf(listOf(genShinSignButton), returnButton()))
            val editMessageText = EditMessageText.builder().chatId(chatId).messageId(it.message.messageId).text("米哈游").replyMarkup(markup).build()
            execute(editMessageText)
        }
        query("genShinSign") {
            val chatId = it.message.chatId
            val miHoYoEntity = miHoYoService.findByTgId(chatId)!!
            val result = MiHoYoLogic.sign(miHoYoEntity)
            val sendMessage = SendMessage.builder().chatId(chatId).text(result.message).build()
            execute(sendMessage)
        }
    }

    fun netEaseExec() = callback {
        query("netEaseExec") {
            val chatId = it.message.chatId
            netEaseService.findByTgId(chatId) ?: error("未绑定网易云音乐账号")
            val netEaseSignButton = InlineKeyboardButton("签到").apply { callbackData = "netEaseSign" }
            val netEaseMusicianSignButton = InlineKeyboardButton("音乐人签到").apply { callbackData = "netEaseMusicianSign" }
            val markup = InlineKeyboardMarkup(listOf(listOf(netEaseSignButton), listOf(netEaseMusicianSignButton), returnButton()))
            val editMessageText = EditMessageText.builder().chatId(chatId).messageId(it.message.messageId).text("网易云音乐").replyMarkup(markup).build()
            execute(editMessageText)
        }
        query("netEaseSign") {
            val chatId = it.message.chatId
            val netEaseEntity = netEaseService.findByTgId(chatId)!!
            val result = NetEaseLogic.sign(netEaseEntity)
            val message = if (result.failure()) {
                result.message
            } else {
                delay(3000)
                NetEaseLogic.listenMusic(netEaseEntity)
                "网易云音乐签到成功"
            }
            val sendMessage = SendMessage.builder().chatId(chatId).text(message).build()
            execute(sendMessage)
        }
        query("netEaseMusicianSign") {
            val chatId = it.message.chatId
            val netEaseEntity = netEaseService.findByTgId(chatId)!!
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
            val sendMessage = SendMessage.builder().chatId(chatId).text(message).build()
            execute(sendMessage)
        }
    }

    fun stepExec() = callback {
        query("stepExec") {
            val chatId = it.message.chatId
            val stepEntity = stepService.findByTgId(chatId) ?: error("未绑定任何刷步数账号")
            val list = mutableListOf<List<InlineKeyboardButton>>()
            if (stepEntity.leXinCookie.isNotEmpty()) {
                list.add(listOf(InlineKeyboardButton("乐心运动刷步数").apply { callbackData = "leXinStepExec" }))
            }
            if (stepEntity.miLoginToken.isNotEmpty()) {
                list.add(listOf(InlineKeyboardButton("小米运动刷步数").apply { callbackData = "xiaomiStepExec" }))
            }
            list.add(returnButton())
            val markup = InlineKeyboardMarkup(list)
            val editMessageText = EditMessageText.builder().chatId(chatId).messageId(it.message.messageId).text("刷步数").replyMarkup(markup).build()
            execute(editMessageText)
        }
        query("leXinStepExec") {
            val chatId = it.message.chatId
            execute(SendMessage.builder().chatId(chatId).text("请发送需要刷的步数").build())
            val step = it.waitNextMessage().text.toIntOrNull() ?: error("步数不为数字")
            val stepEntity = stepService.findByTgId(chatId)!!
            val res = LeXinStepLogic.modifyStepCount(stepEntity, step)
            execute(SendMessage.builder().chatId(chatId).text(res.message).build())
        }
        query("xiaomiStepExec") {
            val chatId = it.message.chatId
            execute(SendMessage.builder().chatId(chatId).text("请发送需要刷的步数").build())
            val step = it.waitNextMessage().text.toIntOrNull() ?: error("步数不为数字")
            val stepEntity = stepService.findByTgId(chatId)!!
            val res = XiaomiStepLogic.modifyStepCount(stepEntity, step)
            execute(SendMessage.builder().chatId(chatId).text(res.message).build())
        }
    }

    fun weiboExec() = callback {
        query("weiboExec") {
            val chatId = it.message.chatId
            weiboService.findByTgId(chatId) ?: error("未绑定微博账号")
            val superTalkSignButton = InlineKeyboardButton("超话签到").apply { callbackData = "superTalkSign" }
            val markup = InlineKeyboardMarkup(listOf(listOf(superTalkSignButton), returnButton()))
            val editMessageText = EditMessageText.builder().chatId(chatId).text("微博").messageId(it.message.messageId).replyMarkup(markup).build()
            execute(editMessageText)
        }
        query("superTalkSign") {
            val chatId = it.message.chatId
            val weiboEntity = weiboService.findByTgId(chatId)!!
            val result = WeiboLogic.superTalkSign(weiboEntity)
            val sendMessage = SendMessage.builder().chatId(chatId).text(result.message).build()
            execute(sendMessage)
        }
    }


}