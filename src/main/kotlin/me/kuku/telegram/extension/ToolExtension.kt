package me.kuku.telegram.extension

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.InputMediaPhoto
import com.pengrad.telegrambot.request.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import me.kuku.telegram.entity.BiliBiliService
import me.kuku.telegram.logic.BiliBiliLogic
import me.kuku.telegram.logic.ToolLogic
import me.kuku.telegram.logic.YgoLogic
import me.kuku.telegram.utils.*
import me.kuku.utils.*
import org.springframework.stereotype.Service

@Service
class ToolExtension(
    private val ygoLogic: YgoLogic,
    private val toolLogic: ToolLogic,
    private val biliBiliService: BiliBiliService
) {

    private val mutex = Mutex()

    fun AbilitySubscriber.queryYgoCard() {
        sub("ygo", 1) {
            val cardList = ygoLogic.search(firstArg())
            val list = mutableListOf<Array<InlineKeyboardButton>>()
            for (i in cardList.indices) {
                val card = cardList[i]
                list.add(arrayOf(InlineKeyboardButton(card.chineseName).callbackData("ygoCard-${card.cardPassword}")))
            }
            sendMessage("请选择查询的卡片", replyKeyboard = InlineKeyboardMarkup(*list.toTypedArray()))
        }
        sub("lolicon", locality = Locality.ALL) {
            val r18 = kotlin.runCatching {
                if (firstArg().lowercase() == "r18") 1 else 0
            }.getOrDefault(0)
            val jsonNode = OkHttpKtUtils.getJson("https://api.lolicon.app/setu/v2?r18=$r18")
            val url = jsonNode["data"][0]["urls"]["original"].asText()
            val bytes = OkHttpKtUtils.getBytes(url)
            if (bytes.size > 1024 * 10 * 1024) {
                val sendDocument = SendDocument(chatId, bytes)
                message.messageThreadId()?.let {
                    sendDocument.messageThreadId(it)
                }
                bot.execute(sendDocument)
            } else {
                val sendPhoto = SendPhoto(chatId, bytes)
                message.messageThreadId()?.let {
                    sendPhoto.messageThreadId(it)
                }
                bot.execute(sendPhoto)
            }
        }
        sub("loliconmulti", locality = Locality.ALL) {
            loLiConMulti(chatId.toString(), bot, this, message.messageThreadId())
        }
        sub("tool") {
            sendMessage("请选择小工具", toolKeyboardMarkup())
        }
        sub("bv", input = 1) {
            mutex.withLock {
                val biliBiliEntity = biliBiliService.findByTgId(tgId)
                    ?: errorAnswerCallbackQuery("未绑定哔哩哔哩，无法获取视频")
                val bvId = firstArg()
                val file = BiliBiliLogic.videoByBvId(biliBiliEntity, bvId)
                if (file.length() > 1024 * 1024 * 1024 * 2L) {
                    sendMessage("该视频大于2G，无法发送")
                } else {
                    val sendVideo =
                        SendVideo(tgId, file).caption(bvId)
                    bot.execute(sendVideo)
                }
                file.delete()
            }
        }
    }

    fun TelegramSubscribe.selectCard() {
        callbackStartsWith("ygoCard") {
            answerCallbackQuery("获取成功")
            val id = query.data().split("-")[1]
            val card = ygoLogic.searchDetail(id.toLong())
            val sendPhoto = SendPhoto(chatId, OkHttpKtUtils.getBytes(card.imageUrl))
            sendPhoto.caption("中文名：${card.chineseName}\n日文名：${card.japaneseName}\n英文名：${card.englishName}\n效果：\n${card.effect}\n链接：${card.url}")
            bot.execute(sendPhoto)
        }
    }

    private fun toolKeyboardMarkup(): InlineKeyboardMarkup {
        val fishermanCalendarButton = InlineKeyboardButton("摸鱼日历").callbackData("FishermanCalendarTool")
        val ttsButton = InlineKeyboardButton("tts").callbackData("ttsTool")
        val saucenaoButton = InlineKeyboardButton("saucenao识图").callbackData("saucenaoTool")
        return InlineKeyboardMarkup(
            arrayOf(fishermanCalendarButton),
            arrayOf(ttsButton, saucenaoButton)
        )
    }

    private suspend fun loLiConMulti(chatId: String, bot: TelegramBot, abilityContext: AbilityContext, messageThreadId: Int? = null) {
        val r18 = kotlin.runCatching {
            if (abilityContext.firstArg().lowercase() == "r18") 1 else 0
        }.getOrDefault(0)
        val jsonNode = OkHttpKtUtils.getJson("https://api.lolicon.app/setu/v2?num=5&r18=$r18")
        val list = jsonNode["data"].map { node -> node["urls"]["original"].asText() }
        val inputMediaList = mutableListOf<InputMediaPhoto>()
        for (i in list.indices) {
            val s = list[i]
            val bytes = OkHttpKtUtils.getBytes(s)
            if (bytes.size > 1024 * 10 * 1024) continue
            val name = s.substring(s.lastIndexOf('/') + 1)
            val mediaPhoto = InputMediaPhoto(bytes).fileName(name)
            inputMediaList.add(mediaPhoto)
        }
        val sendMediaGroup = SendMediaGroup(chatId, *inputMediaList.toTypedArray())
        messageThreadId?.let {
            sendMediaGroup.messageThreadId(it)
        }
        bot.execute(sendMediaGroup)
    }

    fun TelegramSubscribe.colorPic() {
        callback("FishermanCalendarTool") {
            OkHttpKtUtils.getBytes("https://api.kukuqaq.com/fishermanCalendar?preview").let {
                val sendPhoto = SendPhoto(chatId, it)
                bot.execute(sendPhoto)
            }
            answerCallbackQuery("获取成功")
        }
        callback("ttsTool") {
            editMessageText("请发送生成的语音日语文字")
            val text = nextMessage().text()
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
                    base.base64Decode().let {
                        val sendAudio = SendAudio(chatId, it).fileName("tts.wav")
                        bot.execute(sendAudio)
                    }
                    break
                }
            }
        }
       callback("saucenaoTool") {
            editMessageText("请发送需要识别的图片")
            val message = nextMessage()
            val photoList = message.photo()
            if (photoList.isEmpty()) error("您发送的不为图片")
            val photo = photoList.last()
            val getFile = GetFile(photo.fileId())
            val file = bot.execute(getFile).file()
            val list = toolLogic.saucenao(file.byteArray())
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
