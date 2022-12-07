package me.kuku.telegram.extension

import me.kuku.telegram.entity.*
import me.kuku.telegram.utils.ability
import me.kuku.telegram.utils.callback
import me.kuku.telegram.utils.execute
import org.springframework.stereotype.Component
import org.telegram.abilitybots.api.util.AbilityExtension
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
class DeleteExtension(
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
    private val douYinService: DouYinService,
    private val twitterService: TwitterService,
    private val pixivService: PixivService,
    private val buffService: BuffService
): AbilityExtension {

    private fun markup(): InlineKeyboardMarkup {
        val baiduButton = InlineKeyboardButton("百度").also { it.callbackData = "baiduDelete" }
        val biliBiliButton = InlineKeyboardButton("哔哩哔哩").also { it.callbackData = "biliBiliDelete" }
        val douYuButton = InlineKeyboardButton("斗鱼").also { it.callbackData = "douYuDelete" }
        val hostLocButton = InlineKeyboardButton("HostLoc").also { it.callbackData = "hostLocDelete" }
        val huYaButton = InlineKeyboardButton("虎牙").also { it.callbackData = "huYaDelete" }
        val kuGouButton = InlineKeyboardButton("酷狗").also { it.callbackData = "kuGouDelete" }
        val miHoYoButton = InlineKeyboardButton("米忽悠").also { it.callbackData = "miHoYoDelete" }
        val netEaseButton = InlineKeyboardButton("网易云音乐").also { it.callbackData = "netEaseDelete" }
        val stepButton = InlineKeyboardButton("修改步数").also { it.callbackData = "stepDelete" }
        val weiboStepButton = InlineKeyboardButton("微博").also { it.callbackData = "weiboDelete" }
        val douYinButton = InlineKeyboardButton("抖音").also { it.callbackData = "douYinDelete" }
        val twitterButton = InlineKeyboardButton("twitter").also { it.callbackData = "twitterDelete" }
        val pixivButton = InlineKeyboardButton("pixiv").also { it.callbackData = "pixivDelete" }
        val buffButton = InlineKeyboardButton("网易buff").also { it.callbackData = "buffDelete" }
        return InlineKeyboardMarkup(listOf(
            listOf(baiduButton, biliBiliButton),
            listOf(douYuButton, hostLocButton),
            listOf(huYaButton, kuGouButton),
            listOf(miHoYoButton, netEaseButton),
            listOf(stepButton, weiboStepButton),
            listOf(douYinButton, twitterButton),
            listOf(pixivButton, buffButton)
        ))
    }

    fun delete() = ability("delete", "删除登录的账号") {
        val sendMessage = SendMessage()
        sendMessage.text = "请点击按钮，以删除对应账号"
        sendMessage.replyMarkup = markup()
        sendMessage.chatId = chatId().toString()
        execute(sendMessage)
    }

    fun deleteOperate() = callback {
        query("baiduDelete") {
            baiduService.deleteByTgId(query.from.id)
            bot.execute(SendMessage.builder().text("删除百度成功").chatId(query.from.id).build())
        }
        query("biliBiliDelete") {
            biliBiliService.deleteByTgId(query.from.id)
            bot.execute(SendMessage.builder().text("删除哔哩哔哩成功").chatId(query.from.id).build())
        }
        query("douYuDelete") {
            douYuService.deleteByTgId(query.from.id)
            bot.execute(SendMessage.builder().text("删除斗鱼成功").chatId(query.from.id).build())
        }
        query("hostLocDelete") {
            hostLocService.deleteByTgId(query.from.id)
            bot.execute(SendMessage.builder().text("删除HostLoc成功").chatId(query.from.id).build())
        }
        query("huYaDelete") {
            huYaService.deleteByTgId(query.from.id)
            bot.execute(SendMessage.builder().text("删除虎牙成功").chatId(query.from.id).build())
        }
        query("kuGouDelete") {
            kuGouService.deleteByTgId(query.from.id)
            bot.execute(SendMessage.builder().text("删除酷狗成功").chatId(query.from.id).build())
        }
        query("miHoYoDelete") {
            miHoYoService.deleteByTgId(query.from.id)
            bot.execute(SendMessage.builder().text("删除米哈游成功").chatId(query.from.id).build())
        }
        query("netEaseDelete") {
            netEaseService.deleteByTgId(query.from.id)
            bot.execute(SendMessage.builder().text("删除网易云音乐成功").chatId(query.from.id).build())
        }
        query("stepDelete") {
            stepService.deleteByTgId(query.from.id)
            bot.execute(SendMessage.builder().text("删除修改步数成功").chatId(query.from.id).build())
        }
        query("weiboDelete") {
            weiboService.deleteByTgId(query.from.id)
            bot.execute(SendMessage.builder().text("删除微博成功").chatId(query.from.id).build())
        }
        query("douYinDelete") {
            douYinService.deleteByTgId(query.from.id)
            bot.execute(SendMessage.builder().text("删除抖音成功").chatId(query.from.id).build())
        }
        query("twitterDelete") {
            twitterService.deleteByTgId(query.from.id)
            bot.execute(SendMessage.builder().text("删除Twitter成功").chatId(query.from.id).build())
        }
        query("pixivDelete") {
            pixivService.deleteByTgId(query.from.id)
            bot.execute(SendMessage.builder().text("删除Pixiv成功").chatId(query.from.id).build())
        }
        query("buffDelete") {
            buffService.deleteByTgId(query.from.id)
            bot.execute(SendMessage.builder().text("删除网易Buff成功").chatId(query.from.id).build())
        }
    }


}
