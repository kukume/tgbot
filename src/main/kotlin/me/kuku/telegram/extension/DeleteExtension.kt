package me.kuku.telegram.extension

import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import me.kuku.telegram.context.AbilitySubscriber
import me.kuku.telegram.context.TelegramSubscribe
import me.kuku.telegram.context.inlineKeyboardButton
import me.kuku.telegram.entity.*
import org.springframework.stereotype.Component

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
    private val twitterService: TwitterService,
    private val pixivService: PixivService,
    private val smZdmService: SmZdmService,
    private val aliDriveService: AliDriveService,
    private val leiShenService: LeiShenService,
    private val nodeSeekService: NodeSeekService,
    private val gloDosService: GlaDosService,
    private val iqyService: IqyService,
    private val eCloudService: ECloudService
) {

    private fun markup(): InlineKeyboardMarkup {
        val baiduButton = InlineKeyboardButton("百度").callbackData("baiduDelete")
        val biliBiliButton = InlineKeyboardButton("哔哩哔哩").callbackData("biliBiliDelete")
        val douYuButton = InlineKeyboardButton("斗鱼").callbackData("douYuDelete")
        val hostLocButton = InlineKeyboardButton("HostLoc").callbackData("hostLocDelete")
        val huYaButton = InlineKeyboardButton("虎牙").callbackData("huYaDelete")
        val kuGouButton = InlineKeyboardButton("酷狗").callbackData("kuGouDelete")
        val miHoYoButton = InlineKeyboardButton("米忽悠").callbackData("miHoYoDelete")
        val netEaseButton = InlineKeyboardButton("网易云音乐").callbackData("netEaseDelete")
        val stepButton = InlineKeyboardButton("修改步数").callbackData("stepDelete")
        val weiboStepButton = InlineKeyboardButton("微博").callbackData("weiboDelete")
        val twitterButton = InlineKeyboardButton("twitter").callbackData("twitterDelete")
        val pixivButton = InlineKeyboardButton("pixiv").callbackData("pixivDelete")
        val smZdmButton = inlineKeyboardButton("什么值得买", "smZdmDelete")
        val aliDriveButton = inlineKeyboardButton("阿里云盘", "aliDriveDelete")
        val leiShenDelete = inlineKeyboardButton("雷神加速器", "leiShenDelete")
        val nodeSeek = inlineKeyboardButton("NodeSeek", "nodeSeekDelete")
        val gloDos = inlineKeyboardButton("GloDos", "gloDosDelete")
        val iqy = inlineKeyboardButton("爱奇艺", "iqyDelete")
        val eCloud = inlineKeyboardButton("天翼云盘", "eCloudDelete")
        return InlineKeyboardMarkup(
            arrayOf(baiduButton, biliBiliButton),
            arrayOf(douYuButton, hostLocButton),
            arrayOf(huYaButton, kuGouButton),
            arrayOf(miHoYoButton, netEaseButton),
            arrayOf(stepButton, weiboStepButton),
            arrayOf(twitterButton, pixivButton),
            arrayOf(smZdmButton, aliDriveButton),
            arrayOf(leiShenDelete, nodeSeek),
            arrayOf(gloDos, iqy),
            arrayOf(eCloud)
        )
    }

    fun AbilitySubscriber.delete() {
        sub("delete") {
            sendMessage("请点击按钮，以删除对应账号", markup())
        }
    }

    fun TelegramSubscribe.deleteOperate() {
        callback("baiduDelete") {
            baiduService.deleteByTgId(tgId)
            editMessageText("删除百度成功")
        }
        callback("biliBiliDelete") {
            biliBiliService.deleteByTgId(tgId)
            editMessageText("删除哔哩哔哩成功")
        }
        callback("douYuDelete") {
            douYuService.deleteByTgId(tgId)
            editMessageText("删除斗鱼成功")
        }
        callback("hostLocDelete") {
            hostLocService.deleteByTgId(tgId)
            editMessageText("删除HostLoc成功")
        }
        callback("huYaDelete") {
            huYaService.deleteByTgId(tgId)
            editMessageText("删除虎牙成功")
        }
        callback("kuGouDelete") {
            kuGouService.deleteByTgId(tgId)
            editMessageText("删除酷狗成功")
        }
        callback("miHoYoDelete") {
            miHoYoService.deleteByTgId(tgId)
            editMessageText("删除米哈游成功")
        }
        callback("netEaseDelete") {
            netEaseService.deleteByTgId(tgId)
            editMessageText("删除网易云音乐成功")
        }
        callback("stepDelete") {
            stepService.deleteByTgId(tgId)
            editMessageText("删除修改步数成功")
        }
        callback("weiboDelete") {
            weiboService.deleteByTgId(tgId)
            editMessageText("删除微博成功")
        }
        callback("twitterDelete") {
            twitterService.deleteByTgId(tgId)
            editMessageText("删除Twitter成功")
        }
        callback("pixivDelete") {
            pixivService.deleteByTgId(tgId)
            editMessageText("删除Pixiv成功")
        }
        callback("smZdmDelete") {
            smZdmService.deleteByTgId(tgId)
            editMessageText("删除什么值得买成功")
        }
        callback("aliDriveDelete") {
            aliDriveService.deleteByTgId(tgId)
            editMessageText("删除阿里云盘成功")
        }
        callback("leiShenDelete") {
            leiShenService.deleteByTgId(tgId)
            editMessageText("删除雷神加速器成功")
        }
        callback("nodeSeekDelete") {
            nodeSeekService.deleteByTgId(tgId)
            editMessageText("删除NodeSeek成功")
        }
        callback("gloDosDelete") {
            gloDosService.deleteByTgId(tgId)
            editMessageText("删除GloDos成功")
        }
        callback("iqyDelete") {
            iqyService.deleteByTgId(tgId)
            editMessageText("删除爱奇艺成功")
        }
        callback("eCloudDelete") {
            eCloudService.deleteByTgId(tgId)
            editMessageText("删除天翼云盘成功")
        }
    }


}
