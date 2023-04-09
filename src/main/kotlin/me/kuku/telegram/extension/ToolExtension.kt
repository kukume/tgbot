package me.kuku.telegram.extension

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import me.kuku.telegram.config.TelegramBot
import me.kuku.telegram.logic.ToolLogic
import me.kuku.telegram.logic.YgoLogic
import me.kuku.telegram.utils.*
import me.kuku.utils.*
import org.springframework.stereotype.Service
import org.telegram.abilitybots.api.bot.BaseAbilityBot
import org.telegram.abilitybots.api.objects.Locality
import org.telegram.abilitybots.api.util.AbilityExtension
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.send.SendAudio
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.media.InputMedia
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.io.InputStream

@Service
class ToolExtension(
    private val ygoLogic: YgoLogic,
    private val telegramBot: TelegramBot,
    private val toolLogic: ToolLogic
): AbilityExtension {

    fun AbilitySubscriber.queryYgoCard() {
        sub("ygo", "游戏王查卡", 1) {
            val cardList = ygoLogic.search(firstArg())
            val list = mutableListOf<List<InlineKeyboardButton>>()
            for (i in cardList.indices) {
                val card = cardList[i]
                list.add(listOf(InlineKeyboardButton(card.chineseName).apply { callbackData = "ygoCard-${card.cardPassword}" }))
            }
            sendMessage("请选择查询的卡片", replyKeyboard = InlineKeyboardMarkup(list))
        }
        sub("lolicon", "lolicon图片", locality = Locality.ALL) {
            val jsonNode = OkHttpKtUtils.getJson("https://api.lolicon.app/setu/v2?r18=2")
            val url = jsonNode["data"][0]["urls"]["original"].asText()
            val bytes = OkHttpKtUtils.getBytes(url)
            if (bytes.size > 1024 * 10 * 1024) error("图片大于10M，发送失败")
            bytes.inputStream().use { iis ->
                val sendPhoto = SendPhoto(chatId.toString(), InputFile(iis, url.substring(url.lastIndexOf('/') + 1)))
                bot.execute(sendPhoto)
            }
        }
        sub("loliconmulti", "lolicon多张图片", locality = Locality.ALL) {
            loLiConMulti(chatId.toString(), bot)
        }
        sub("tool", "工具") {
            sendMessage("请选择小工具", toolKeyboardMarkup())
        }
    }

    fun TelegramSubscribe.selectCard() {
        callbackStartsWith("ygoCard") {
            answerCallbackQuery("获取成功")
            val id = query.data.split("-")[1]
            val card = ygoLogic.searchDetail(id.toLong())
            val sendPhoto = SendPhoto()
            sendPhoto.chatId = query.message.chatId.toString()
            sendPhoto.photo = InputFile(OkHttpKtUtils.getByteStream(card.imageUrl), "${card.japaneseName}.jpg")
            sendPhoto.caption = "中文名：${card.chineseName}\n日文名：${card.japaneseName}\n英文名：${card.englishName}\n效果：\n${card.effect}"
            bot.execute(sendPhoto)
        }
    }

    private fun toolKeyboardMarkup(): InlineKeyboardMarkup {
        val loLiConButton = InlineKeyboardButton("LoLiCon").also { it.callbackData = "LoLiConTool" }
        val fishermanCalendarButton = InlineKeyboardButton("摸鱼日历").also { it.callbackData = "FishermanCalendarTool" }
        val ttsButton = InlineKeyboardButton("tts").also { it.callbackData = "ttsTool" }
        val saucenaoButton = InlineKeyboardButton("saucenao识图").also { it.callbackData = "saucenaoTool" }
        return InlineKeyboardMarkup(listOf(
            listOf(loLiConButton, fishermanCalendarButton),
            listOf(ttsButton, saucenaoButton)
        ))
    }

    private suspend fun loLiConMulti(chatId: String, bot: BaseAbilityBot) {
        val jsonNode = OkHttpKtUtils.getJson("https://api.lolicon.app/setu/v2?num=5&r18=2")
        val list = jsonNode["data"].map { node -> node["urls"]["original"].asText() }
        val inputMediaList = mutableListOf<InputMedia>()
        val ii = mutableListOf<InputStream>()
        try {
            for (i in list.indices) {
                val s = list[i]
                val bytes = OkHttpKtUtils.getBytes(s)
                if (bytes.size > 1024 * 10 * 1024) error("图片大于10M，发送失败")
                val name = s.substring(s.lastIndexOf('/') + 1)
                val bis = bytes.inputStream()
                val mediaPhoto =
                    InputMediaPhoto.builder().newMediaStream(bis).media("attach://$name").mediaName(name).isNewMedia(true).build()
                inputMediaList.add(mediaPhoto)
                ii.add(bis)
            }
            val sendMediaGroup = SendMediaGroup(chatId, inputMediaList)
            bot.execute(sendMediaGroup)
        } finally {
            ii.forEach { iis -> iis.close() }
        }
    }

    fun TelegramSubscribe.colorPic() {
        callback("LoLiConTool") {
            loLiConMulti(chatId.toString(), bot)
        }
        callback("FishermanCalendarTool") {
            OkHttpKtUtils.getByteStream("https://api.kukuqaq.com/fishermanCalendar?preview").use { iis ->
                val sendPhoto = SendPhoto(query.message.chatId.toString(), InputFile(iis, "FishermanCalendarTool.jpg"))
                bot.execute(sendPhoto)
            }
            answerCallbackQuery("获取成功")
        }
        callback("ttsTool") {
            editMessageText("请发送生成的语音日语文字")
            val text = nextMessage().text
            val jsonNode = OkHttpKtUtils.postJson("https://innnky-vits-nyaru.hf.space/api/queue/push/", OkUtils.json("""
                {"fn_index":0,"data":["$text"],"action":"predict","session_hash":""}
            """.trimIndent()))
            val hash = jsonNode["hash"].asText()
            withTimeout(1000 * 20) {
                while (true) {
                    delay(1000)
                    val statusJsonNode = OkHttpKtUtils.postJson("https://innnky-vits-nyaru.hf.space/api/queue/status/",
                        OkUtils.json("""{"hash":"$hash"}"""))
                    if (statusJsonNode["status"].asText() == "QUEUED") continue
                    val data = statusJsonNode["data"]["data"] ?: continue
                    val status = data[0].asText()
                    if (status != "Success") error(status)
                    val base = data[1].asText().substring(22)
                    base.base64Decode().inputStream().use { iis ->
                        val sendAudio = SendAudio(chatId.toString(), InputFile(iis, "tts.wav"))
                        bot.execute(sendAudio)
                    }
                    break
                }
            }
        }
       callback("saucenaoTool") {
            editMessageText("请发送需要识别的图片")
            val message = nextMessage()
            val photoList = message.photo
            if (photoList.isEmpty()) error("您发送的不为图片")
            val photo = photoList.last()
            val getFile = GetFile(photo.fileId)
            val file = bot.execute(getFile)
            val url = "https://api.telegram.org/file/bot${telegramBot.token}/${file.filePath}"
            val newUrl = toolLogic.upload(url)
            val list = toolLogic.saucenao(newUrl)
            if (list.isEmpty()) error("未找到结果")
            val result = list[0]
            editMessageText("""
                相似度：${result.similarity}
                名字：${result.indexName}
                标题：${result.title}
                预览链接：${result.thumbnail}
                源链接：${result.extUrls}
                作者：${result.author} 
                作者主页：${result.authUrl}
            """.trimIndent())
        }
    }

}
