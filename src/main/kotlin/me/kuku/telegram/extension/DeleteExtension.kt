package me.kuku.telegram.extension

import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import me.kuku.telegram.context.AbilitySubscriber
import me.kuku.telegram.context.TelegramSubscribe
import me.kuku.telegram.context.inlineKeyboardButton
import me.kuku.telegram.entity.*

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
        BaiduService.deleteByTgId(tgId)
        editMessageText("删除百度成功")
    }
    callback("biliBiliDelete") {
        BiliBiliService.deleteByTgId(tgId)
        editMessageText("删除哔哩哔哩成功")
    }
    callback("douYuDelete") {
        DouYuService.deleteByTgId(tgId)
        editMessageText("删除斗鱼成功")
    }
    callback("hostLocDelete") {
        HostLocService.deleteByTgId(tgId)
        editMessageText("删除HostLoc成功")
    }
    callback("huYaDelete") {
        HuYaService.deleteByTgId(tgId)
        editMessageText("删除虎牙成功")
    }
    callback("kuGouDelete") {
        KuGouService.deleteByTgId(tgId)
        editMessageText("删除酷狗成功")
    }
    callback("miHoYoDelete") {
        MiHoYoService.deleteByTgId(tgId)
        editMessageText("删除米哈游成功")
    }
    callback("netEaseDelete") {
        NetEaseService.deleteByTgId(tgId)
        editMessageText("删除网易云音乐成功")
    }
    callback("stepDelete") {
        StepService.deleteByTgId(tgId)
        editMessageText("删除修改步数成功")
    }
    callback("weiboDelete") {
        WeiboService.deleteByTgId(tgId)
        editMessageText("删除微博成功")
    }
    callback("twitterDelete") {
        TwitterService.deleteByTgId(tgId)
        editMessageText("删除Twitter成功")
    }
    callback("pixivDelete") {
        PixivService.deleteByTgId(tgId)
        editMessageText("删除Pixiv成功")
    }
    callback("smZdmDelete") {
        SmZdmService.deleteByTgId(tgId)
        editMessageText("删除什么值得买成功")
    }
    callback("aliDriveDelete") {
        AliDriveService.deleteByTgId(tgId)
        editMessageText("删除阿里云盘成功")
    }
    callback("leiShenDelete") {
        LeiShenService.deleteByTgId(tgId)
        editMessageText("删除雷神加速器成功")
    }
    callback("nodeSeekDelete") {
        NodeSeekService.deleteByTgId(tgId)
        editMessageText("删除NodeSeek成功")
    }
    callback("gloDosDelete") {
        GlaDosService.deleteByTgId(tgId)
        editMessageText("删除GloDos成功")
    }
    callback("iqyDelete") {
        IqyService.deleteByTgId(tgId)
        editMessageText("删除爱奇艺成功")
    }
    callback("eCloudDelete") {
        ECloudService.deleteByTgId(tgId)
        editMessageText("删除天翼云盘成功")
    }
}