package me.kuku.telegram.extension

import me.kuku.telegram.logic.YgoLogic
import me.kuku.telegram.utils.ability
import me.kuku.telegram.utils.callbackStartWith
import me.kuku.telegram.utils.execute
import me.kuku.utils.OkHttpKtUtils
import org.springframework.stereotype.Service
import org.telegram.abilitybots.api.util.AbilityExtension
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Service
class ToolExtension(
    private val ygoLogic: YgoLogic
): AbilityExtension {

    fun queryYgoCard() = ability("ygo", "游戏王查卡", 1) {
        val cardList = ygoLogic.search(firstArg())
        val list = mutableListOf<List<InlineKeyboardButton>>()
        for (i in cardList.indices) {
            val card = cardList[i]
            list.add(listOf(InlineKeyboardButton(card.chineseName).apply { callbackData = "ygoCard-${card.cardPassword}" }))
        }
        val sendMessage =
            SendMessage.builder().chatId(chatId()).text("请选择查询的卡片").replyMarkup(InlineKeyboardMarkup(list))
                .build()
        execute(sendMessage)
    }

    fun selectCard() = callbackStartWith("ygoCard") {
        val id = it.data.split("-")[1]
        val card = ygoLogic.searchDetail(id.toLong())
        val sendPhoto = SendPhoto()
        sendPhoto.chatId = it.message.chatId.toString()
        sendPhoto.photo = InputFile(OkHttpKtUtils.getByteStream(card.imageUrl), "${card.japaneseName}.jpg")
        sendPhoto.caption = "中文名：${card.chineseName}\n日文名：${card.japaneseName}\n英文名：${card.englishName}\n效果：\n${card.effect}"
        execute(sendPhoto)
    }



}