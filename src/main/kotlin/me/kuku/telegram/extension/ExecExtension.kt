package me.kuku.telegram.extension

import kotlinx.coroutines.delay
import me.kuku.telegram.entity.*
import me.kuku.telegram.logic.*
import me.kuku.telegram.utils.*
import me.kuku.utils.MyUtils
import me.kuku.utils.OkHttpKtUtils
import org.springframework.stereotype.Service
import org.telegram.abilitybots.api.util.AbilityExtension
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendVideo
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo
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
    private val douYinService: DouYinService
): AbilityExtension {

    private val recommendCache = mutableMapOf<Long, List<DouYinWork>>()

    private fun execKeyboardMarkup(): InlineKeyboardMarkup {
        val baiduButton = InlineKeyboardButton("百度").also { it.callbackData = "baiduExec" }
        val biliBiliButton = InlineKeyboardButton("哔哩哔哩").also { it.callbackData = "biliBiliExec" }
        val hostLocButton = InlineKeyboardButton("HostLoc").also { it.callbackData = "hostLocExec" }
        val kuGouButton = InlineKeyboardButton("酷狗").also { it.callbackData = "kuGouExec" }
        val miHoYoButton = InlineKeyboardButton("米忽悠").also { it.callbackData = "miHoYoExec" }
        val netEaseButton = InlineKeyboardButton("网易云音乐").also { it.callbackData = "netEaseExec" }
        val stepButton = InlineKeyboardButton("刷步数").also { it.callbackData = "stepExec" }
        val weiboButton = InlineKeyboardButton("微博").also { it.callbackData = "weiboExec" }
        val douYinButton = InlineKeyboardButton("抖音").also { it.callbackData = "douYinExec" }
        return InlineKeyboardMarkup(listOf(
            listOf(baiduButton, biliBiliButton),
            listOf(hostLocButton, kuGouButton),
            listOf(miHoYoButton, netEaseButton),
            listOf(stepButton, weiboButton),
            listOf(douYinButton)
        ))
    }

    private fun returnButton(): List<InlineKeyboardButton> {
        return listOf(InlineKeyboardButton("返回").apply { callbackData = "returnExec" })
    }

    fun returnMarkup() = callback("returnExec") {
        val messageId = query.message.messageId
        val editMessageText = EditMessageText()
        editMessageText.chatId = query.message.chatId.toString()
        editMessageText.replyMarkup = execKeyboardMarkup()
        editMessageText.messageId = messageId
        editMessageText.text = "请选择手动执行选项"
        bot.execute(editMessageText)
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
            val chatId = query.message.chatId
            val tgId = query.from.id
            baiduService.findByTgId(tgId) ?: error("未绑定百度账号")
            val tieBaSignButton = InlineKeyboardButton("贴吧签到").apply { callbackData = "tieBaSign" }
            val ybbSignButton = InlineKeyboardButton("游帮帮加速器签到").apply { callbackData = "ybbSign" }
            val ybbWatchAdButton = InlineKeyboardButton("游帮帮加速器看广告").apply { callbackData = "ybbWatchAd" }
            val markup = InlineKeyboardMarkup(listOf(listOf(tieBaSignButton), listOf(ybbSignButton), listOf(ybbWatchAdButton), returnButton()))
            val editMessage = EditMessageText.builder().chatId(chatId).messageId(query.message.messageId).text("百度").replyMarkup(markup).build()
            bot.execute(editMessage)
        }
        query("tieBaSign") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            val baiduEntity = baiduService.findByTgId(tgId)!!
            baiduLogic.tieBaSign(baiduEntity)
            val sendMessage = SendMessage.builder().text("贴吧签到成功").chatId(chatId).build()
            bot.execute(sendMessage)
        }
        query("ybbSign") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            val baiduEntity = baiduService.findByTgId(tgId)!!
            baiduLogic.ybbSign(baiduEntity)
            val sendMessage = SendMessage.builder().text("游帮帮签到成功").chatId(chatId).build()
            bot.execute(sendMessage)
        }
        query("ybbWatchAd") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            val baiduEntity = baiduService.findByTgId(tgId)!!
            baiduLogic.ybbWatchAd(baiduEntity, "v3")
            val sendMessage = SendMessage.builder().text("游帮帮观看广告成功").chatId(chatId).build()
            bot.execute(sendMessage)
        }
    }

    fun biliBiliExec() = callback {
        query("biliBiliExec") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            biliBiliService.findByTgId(tgId) ?: error("未绑定哔哩哔哩账号")
            val biliBiliSignButton = InlineKeyboardButton("签到").apply { callbackData = "biliBiliSign" }
            val markup = InlineKeyboardMarkup(listOf(listOf(biliBiliSignButton), returnButton()))
            val editMessage = EditMessageText.builder().chatId(chatId).messageId(query.message.messageId).text("哔哩哔哩").replyMarkup(markup).build()
            bot.execute(editMessage)
        }
        query("biliBiliSign") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            val biliBiliEntity = biliBiliService.findByTgId(tgId)!!
            val firstRank = BiliBiliLogic.ranking()[0]
            BiliBiliLogic.report(biliBiliEntity, firstRank.aid, firstRank.cid, 300)
            val sendMessage = SendMessage().also { message -> message.chatId = chatId.toString() }
            BiliBiliLogic.share(biliBiliEntity, firstRank.aid)
            BiliBiliLogic.liveSign(biliBiliEntity)
            sendMessage.text = "哔哩哔哩签到成功"
            bot.execute(sendMessage)
        }
    }

    fun hostLocExec() = callback {
        query("hostLocExec") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            hostLocService.findByTgId(tgId) ?: error("未绑定HostLoc账号")
            val hostLocSignButton = InlineKeyboardButton("签到").apply { callbackData = "hostLocSign" }
            val markup = InlineKeyboardMarkup(listOf(listOf(hostLocSignButton), returnButton()))
            val editMessage = EditMessageText.builder().chatId(chatId).messageId(query.message.messageId).text("HostLoc").replyMarkup(markup).build()
            bot.execute(editMessage)
        }
        query("hostLocSign") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            val hostLocEntity = hostLocService.findByTgId(tgId)!!
            val sendMessage = SendMessage.builder().chatId(chatId).text("HostLoc签到后台进行中").build()
            bot.execute(sendMessage)
            HostLocLogic.sign(hostLocEntity.cookie)
        }
    }

    fun kuGouExec() = callback {
        query("kuGouExec") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            kuGouService.findByTgId(tgId) ?: error("未绑定酷狗账号")
            val kuGouMusicianSignButton = InlineKeyboardButton("音乐人").apply { callbackData = "kuGouMusicianSign" }
            val kuGouListenButton = InlineKeyboardButton("概念版听歌得vip").apply { callbackData = "kuGouListen" }
            val markup = InlineKeyboardMarkup(listOf(listOf(kuGouMusicianSignButton), listOf(kuGouListenButton), returnButton()))
            val editMessageText = EditMessageText.builder().chatId(chatId).messageId(query.message.messageId).text("酷狗").replyMarkup(markup).build()
            bot.execute(editMessageText)
        }
        query("kuGouMusicianSign") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            val kuGouEntity = kuGouService.findByTgId(tgId)!!
            kuGouLogic.musicianSign(kuGouEntity)
            val sendMessage = SendMessage.builder().chatId(chatId).text("酷狗音乐人签到成功").build()
            bot.execute(sendMessage)
        }
        query("kuGouListen") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            val kuGouEntity = kuGouService.findByTgId(tgId)!!
            kuGouLogic.listenMusic(kuGouEntity)
            val sendMessage = SendMessage.builder().chatId(chatId).text("酷狗听歌得vip成功").build()
            bot.execute(sendMessage)
        }
    }

    fun miHoYoExec() = callback {
        query("miHoYoExec") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            miHoYoService.findByTgId(tgId) ?: error("未绑定米哈游账号")
            val genShinSignButton = InlineKeyboardButton("原神签到").apply { callbackData = "genShinSign" }
            val markup = InlineKeyboardMarkup(listOf(listOf(genShinSignButton), returnButton()))
            val editMessageText = EditMessageText.builder().chatId(chatId).messageId(query.message.messageId).text("米哈游").replyMarkup(markup).build()
            bot.execute(editMessageText)
        }
        query("genShinSign") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            val miHoYoEntity = miHoYoService.findByTgId(tgId)!!
            MiHoYoLogic.sign(miHoYoEntity)
            val sendMessage = SendMessage.builder().chatId(chatId).text("原神签到成功").build()
            bot.execute(sendMessage)
        }
    }

    fun netEaseExec() = callback {
        query("netEaseExec") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            netEaseService.findByTgId(tgId) ?: error("未绑定网易云音乐账号")
            val netEaseSignButton = InlineKeyboardButton("签到").apply { callbackData = "netEaseSign" }
            val netEaseMusicianSignButton = InlineKeyboardButton("音乐人签到").apply { callbackData = "netEaseMusicianSign" }
            val markup = InlineKeyboardMarkup(listOf(listOf(netEaseSignButton), listOf(netEaseMusicianSignButton), returnButton()))
            val editMessageText = EditMessageText.builder().chatId(chatId).messageId(query.message.messageId).text("网易云音乐").replyMarkup(markup).build()
            bot.execute(editMessageText)
        }
        query("netEaseSign") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            val netEaseEntity = netEaseService.findByTgId(tgId)!!
            val result = NetEaseLogic.sign(netEaseEntity)
            val message = if (result.failure()) {
                result.message
            } else {
                delay(3000)
                NetEaseLogic.listenMusic(netEaseEntity)
                "网易云音乐签到成功"
            }
            val sendMessage = SendMessage.builder().chatId(chatId).text(message).build()
            bot.execute(sendMessage)
        }
        query("netEaseMusicianSign") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            val netEaseEntity = netEaseService.findByTgId(tgId)!!
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
            bot.execute(sendMessage)
        }
    }

    fun stepExec() = callback {
        query("stepExec") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            val stepEntity = stepService.findByTgId(tgId) ?: error("未绑定任何刷步数账号")
            val list = mutableListOf<List<InlineKeyboardButton>>()
            if (stepEntity.leXinCookie.isNotEmpty()) {
                list.add(listOf(InlineKeyboardButton("乐心运动刷步数").apply { callbackData = "leXinStepExec" }))
            }
            if (stepEntity.miLoginToken.isNotEmpty()) {
                list.add(listOf(InlineKeyboardButton("小米运动刷步数").apply { callbackData = "xiaomiStepExec" }))
            }
            list.add(returnButton())
            val markup = InlineKeyboardMarkup(list)
            val editMessageText = EditMessageText.builder().chatId(chatId).messageId(query.message.messageId).text("刷步数").replyMarkup(markup).build()
            bot.execute(editMessageText)
        }
        query("leXinStepExec") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            bot.execute(SendMessage.builder().chatId(chatId).text("请发送需要刷的步数").build())
            val step = query.waitNextMessage().text.toIntOrNull() ?: error("步数不为数字")
            val stepEntity = stepService.findByTgId(tgId)!!
            val res = LeXinStepLogic.modifyStepCount(stepEntity, step)
            bot.execute(SendMessage.builder().chatId(chatId).text(res.message).build())
        }
        query("xiaomiStepExec") {
            val chatId = query.message.chatId
            bot.execute(SendMessage.builder().chatId(chatId).text("请发送需要刷的步数").build())
            val step = query.waitNextMessage().text.toIntOrNull() ?: error("步数不为数字")
            val stepEntity = stepService.findByTgId(query.from.id)!!
            val res = XiaomiStepLogic.modifyStepCount(stepEntity, step)
            bot.execute(SendMessage.builder().chatId(chatId).text(res.message).build())
        }
    }

    fun weiboExec() = callback {
        query("weiboExec") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            weiboService.findByTgId(tgId) ?: error("未绑定微博账号")
            val superTalkSignButton = InlineKeyboardButton("超话签到").apply { callbackData = "superTalkSign" }
            val markup = InlineKeyboardMarkup(listOf(listOf(superTalkSignButton), returnButton()))
            val editMessageText = EditMessageText.builder().chatId(chatId).text("微博").messageId(query.message.messageId).replyMarkup(markup).build()
            bot.execute(editMessageText)
        }
        query("superTalkSign") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            val weiboEntity = weiboService.findByTgId(tgId)!!
            val result = WeiboLogic.superTalkSign(weiboEntity)
            val sendMessage = SendMessage.builder().chatId(chatId).text(result.message).build()
            bot.execute(sendMessage)
        }
    }

    fun douYinExec() = callback {
        query("douYinExec") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            douYinService.findByTgId(tgId) ?: error("未绑定抖音账号")
            val followButton = inlineKeyboardButton("关注列表", "follow")
            val recommendButton = inlineKeyboardButton("推荐", "recommend")
            val markup = InlineKeyboardMarkup(listOf(listOf(followButton), listOf(recommendButton), returnButton()))
            val editMessageText = EditMessageText.builder().chatId(chatId).text("抖音").messageId(query.message.messageId).replyMarkup(markup).build()
            bot.execute(editMessageText)
        }
        query("follow") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            val douYinEntity = douYinService.findByTgId(tgId)!!
            val follow = DouYinLogic.follow(douYinEntity)
            val list = mutableListOf<List<InlineKeyboardButton>>()
            for (douYinUser in follow) {
                val key = "${douYinUser.name}|${douYinUser.uid}|${douYinUser.secUid}"
                list.add(listOf(inlineKeyboardButton(key, "douYinFlow${douYinUser.uid}")))
            }
            val sendMessage = SendMessage(chatId.toString(), "请选择您的关注列表")
            sendMessage.replyMarkup = InlineKeyboardMarkup(list)
            bot.execute(sendMessage)
        }
        queryStartWith("douYinFlow") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            var douYinUser: DouYinUser? = null
            val id = query.data.substring(10).toLong()
            val keyboard = query.message.replyMarkup.keyboard
            for (buttons in keyboard) {
                val button = buttons[0]
                val arr = button.text.split("|")
                val old = DouYinUser(arr[1].toLong(), arr[2], arr[0])
                if (old.uid == id) {
                    douYinUser = old
                    break
                }
            }
            val douYinEntity = douYinService.findByTgId(tgId)!!
            val list = DouYinLogic.work(douYinEntity, douYinUser ?: error("没有找到这个用户"))
            val douYinWork = list[0]
            val url = douYinWork.videoUrlList.last()
            OkHttpKtUtils.getByteStream(url).use { iis ->
                val sendVideo = SendVideo(chatId.toString(), InputFile(iis, "${MyUtils.randomLetter(6)}.mp4"))
                sendVideo.caption = douYinWork.desc
                bot.execute(sendVideo)
            }
        }

        query("recommend") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            val douYinEntity = douYinService.findByTgId(tgId)!!
            val list = DouYinLogic.recommend(douYinEntity)
            recommendCache[tgId] = list
            val douYinWork = list[0]
            val url = douYinWork.videoUrlList[0]
            OkHttpKtUtils.getByteStream(url).use { iis ->
                val sendVideo = SendVideo(chatId.toString(), InputFile(iis, "${MyUtils.randomLetter(6)}.mp4"))
                sendVideo.caption = douYinWork.desc
                val beforeButton = inlineKeyboardButton("前", "recommendChange-1")
                val afterButton = inlineKeyboardButton("后", "recommendChange1")
                sendVideo.replyMarkup = InlineKeyboardMarkup(listOf(listOf(beforeButton, afterButton)))
                bot.execute(sendVideo)
            }
        }

        queryStartWith("recommendChange") {
            val chatId = query.message.chatId
            val tgId = query.from.id
            val index = query.data.substring(15).toInt()
            if (index < 0) error("前面没有视频了")
            val list = recommendCache[tgId] ?: error("缓存不存在，请重新点击推荐")
            if (index >= list.size) error("后面没有视频了")
            val douYinWork = list[index]
            val url = douYinWork.videoUrlList[0]
            val name = MyUtils.randomLetter(6) + ".mp4"
            OkHttpKtUtils.getByteStream(url).use { iis ->
                val editMessageMedia = EditMessageMedia(InputMediaVideo.builder()
                    .newMediaStream(iis).media("attach://$name").mediaName(name).isNewMedia(true).build()
                    .also { video -> video.caption = douYinWork.desc })
                editMessageMedia.chatId = chatId.toString()
                editMessageMedia.messageId = query.message.messageId
                val beforeButton = inlineKeyboardButton("前", "recommendChange${index - 1}")
                val afterButton = inlineKeyboardButton("后", "recommendChange${index + 1}")
                editMessageMedia.replyMarkup = InlineKeyboardMarkup(listOf(listOf(beforeButton, afterButton)))
                bot.execute(editMessageMedia)
            }
        }



    }


}
