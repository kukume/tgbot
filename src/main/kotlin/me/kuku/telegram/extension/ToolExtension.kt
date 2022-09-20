package me.kuku.telegram.extension

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import me.kuku.telegram.config.TelegramBot
import me.kuku.telegram.logic.ToolLogic
import me.kuku.telegram.logic.YgoLogic
import me.kuku.telegram.utils.*
import me.kuku.utils.*
import org.springframework.stereotype.Service
import org.telegram.abilitybots.api.util.AbilityExtension
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.send.SendAudio
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
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

    fun loLiCon() = ability("lolicon", "lolicon图片") {
        val jsonNode = OkHttpKtUtils.getJson("https://api.lolicon.app/setu/v2?r18=2")
        val url = jsonNode["data"][0]["urls"]["original"].asText()
        OkHttpKtUtils.getByteStream(url).use { iis ->
            val sendPhoto = SendPhoto(chatId().toString(), InputFile(iis, url.substring(url.lastIndexOf('/') + 1)))
            bot().execute(sendPhoto)
        }

    }

    private fun toolKeyboardMarkup(): InlineKeyboardMarkup {
        val loLiConButton = InlineKeyboardButton("LoLiCon").also { it.callbackData = "LoLiConTool" }
        val fishermanCalendarButton = InlineKeyboardButton("摸鱼日历").also { it.callbackData = "FishermanCalendarTool" }
        val ttsButton = InlineKeyboardButton("tts").also { it.callbackData = "ttsTool" }
        val baiKeButton = InlineKeyboardButton("百科").also { it.callbackData = "baiKeTool" }
        val saucenaoButton = InlineKeyboardButton("saucenao识图").also { it.callbackData = "saucenaoTool" }
//        val dCloudButton = InlineKeyboardButton("dcloud上传").also { it.callbackData = "dCloudTool" }
        return InlineKeyboardMarkup(listOf(
            listOf(loLiConButton, fishermanCalendarButton),
            listOf(ttsButton, baiKeButton),
            listOf(saucenaoButton)
        ))
    }

    fun manager() = ability("tool", "工具") {
        val markup = toolKeyboardMarkup()
        val sendMessage = SendMessage()
        sendMessage.replyMarkup = markup
        sendMessage.chatId = chatId().toString()
        sendMessage.text = "请选择小工具"
        execute(sendMessage)
    }

    fun colorPic() = callback("LoLiConTool") {
        val chatId = it.message.chatId
        val jsonNode = OkHttpKtUtils.getJson("https://api.lolicon.app/setu/v2?num=5&r18=2")
        val list = jsonNode["data"].map { node -> node["urls"]["original"].asText() }
        val inputMediaList = mutableListOf<InputMedia>()
        val ii = mutableListOf<InputStream>()
        try {
            for (i in list.indices) {
                val s = list[i]
                val bis = OkHttpKtUtils.getByteStream(s)
                val name = s.substring(s.lastIndexOf('/') + 1)
                val mediaPhoto =
                    InputMediaPhoto.builder().newMediaStream(bis).media("attach://$name").mediaName(name).isNewMedia(true).build()
                inputMediaList.add(mediaPhoto)
                ii.add(bis)
            }
            val sendMediaGroup = SendMediaGroup(chatId.toString(), inputMediaList)
            execute(sendMediaGroup)
        } finally {
            ii.forEach { iis -> iis.close() }
        }
    }

    fun fishermanCalendar() = callback("FishermanCalendarTool") {
        OkHttpKtUtils.getByteStream("https://api.kukuqaq.com/fishermanCalendar?preview").use { iis ->
            val sendPhoto = SendPhoto(it.message.chatId.toString(), InputFile(iis, "FishermanCalendarTool.jpg"))
            execute(sendPhoto)
        }
    }

    private fun returnButton(): InlineKeyboardButton {
        return InlineKeyboardButton("返回").also { it.callbackData = "returnTool" }
    }

    fun returnMarkup() = callback("returnTool") {
        val messageId = it.message.messageId
        val editMessageText = EditMessageText()
        editMessageText.chatId = it.message.chatId.toString()
        editMessageText.replyMarkup = toolKeyboardMarkup()
        editMessageText.messageId = messageId
        editMessageText.text = "请选择小工具"
        execute(editMessageText)
    }


    fun tts() = callback {

        query("ttsTool") {
            val messageId = it.message.messageId
            val ttlButton = InlineKeyboardButton("tts").also { bu -> bu.callbackData = "tts" }
            val fishermanCalendarButton = InlineKeyboardButton("变声").also { bu -> bu.callbackData = "voiceChange" }
            val markup =  InlineKeyboardMarkup(listOf(
                listOf(ttlButton, fishermanCalendarButton),
                listOf(returnButton())
            ))
            val editMessageText = EditMessageText()
            editMessageText.chatId = it.message.chatId.toString()
            editMessageText.replyMarkup = markup
            editMessageText.messageId = messageId
            editMessageText.text = "请选择"
            execute(editMessageText)
        }

        query("tts") {
            val chatId = it.message.chatId
            execute(SendMessage.builder().text("请发送生成的语音日语文字").chatId(chatId).build())
            val text = it.waitNextMessage().text
            val jsonNode = OkHttpKtUtils.postJson("https://hf.space/embed/innnky/vits-nyaru/api/queue/push/", OkUtils.json("""
                {"fn_index":0,"data":["$text"],"action":"predict","session_hash":""}
            """.trimIndent()))
            val hash = jsonNode["hash"].asText()
            withTimeout(1000 * 20) {
                while (true) {
                    delay(1000)
                    val statusJsonNode = OkHttpKtUtils.postJson("https://hf.space/embed/innnky/vits-nyaru/api/queue/status/",
                        OkUtils.json("""{"hash":"$hash"}"""))
                    if (statusJsonNode["status"].asText() == "QUEUED") continue
                    val data = statusJsonNode["data"]["data"] ?: continue
                    val status = data[0].asText()
                    if (status != "Success") error(status)
                    val base = data[1].asText().substring(22)
                    base.base64Decode().inputStream().use { iis ->
                        val sendAudio = SendAudio(chatId.toString(), InputFile(iis, "tts.wav"))
                        execute(sendAudio)
                    }
                    break
                }
            }
        }

        query("voiceChange") {
            val chatId = it.message.chatId
            execute(SendMessage.builder().text("请发送语音").chatId(chatId).build())
            val message = it.waitNextMessage()
            val voice = message.voice ?: error("您发送的不为语言")
            val getFile = GetFile(voice.fileId)
            val file = execute(getFile)
            val url = "https://api.telegram.org/file/bot${telegramBot.botToken}/${file.filePath}"
            val bb = "data:audio/wav;base64," + OkHttpKtUtils.getBytes(url).base64Encode()
            OkHttpKtUtils.websocket("wss://spaces.huggingface.tech/innnky/soft-vits-vc/queue/join") {
                open {
                    send("""{"hash":""}""")
                }
                jsonMessage({ node -> node["msg"]?.asText() == "send_data" }) {
                    send("""
                        {"fn_index":0,"data":[{"data":"$bb","name":"tts.wav"}]}
                    """.trimIndent())
                }
                jsonMessage({ node -> node["msg"]?.asText() == "process_completed" }) { jsonNode ->
                    val data = jsonNode["output"]["data"]
                    val status = data[0].asText()
                    if (status == "Success") {
                        val base = data[1].asText().substring(22)
                        base.base64Decode().inputStream().use { iis ->
                            val sendAudio = SendAudio(chatId.toString(), InputFile(iis, "change.wav"))
                            execute(sendAudio)
                        }
                    } else {
                        silent().send(status, chatId)
                    }
                    close(101, "success")
                }
            }
        }

    }

    fun baiKeTool() = callback("baiKeTool") {
        val chatId = it.message.chatId
        val sendMessage = execute(SendMessage(chatId.toString(), "请发送百科内容"))
        val nextMessage = it.waitNextMessage()
        val text = nextMessage.text
        val res = toolLogic.baiKe(text)
        val deleteMessage = DeleteMessage(chatId.toString(), nextMessage.messageId)
        execute(deleteMessage)
        val sendDeleteMessage = DeleteMessage(chatId.toString(), sendMessage.messageId)
        execute(sendDeleteMessage)
        execute(SendMessage(chatId.toString(), res))
    }

    fun saucenaoTool() = callback("saucenaoTool") {
        val chatId = it.message.chatId
        execute(SendMessage.builder().text("请发送需要识别的图片").chatId(chatId).build())
        val message = it.waitNextMessage()
        val photoList = message.photo
        if (photoList.isEmpty()) error("您发送的不为图片")
        val photo = photoList.last()
        val getFile = GetFile(photo.fileId)
        val file = execute(getFile)
        val url = "https://api.telegram.org/file/bot${telegramBot.botToken}/${file.filePath}"
        val newUrl = toolLogic.dCloudUpload(url)
        val list = toolLogic.saucenao(newUrl)
        if (list.isEmpty()) error("未找到结果")
        val result = list[0]
        val sendMessage = SendMessage(chatId.toString(), """
            相似度：${result.similarity}
            名字：${result.indexName}
            标题：${result.title}
            预览链接：${result.thumbnail}
            源链接：${result.extUrls}
            作者：${result.author} 
            作者主页：${result.authUrl}
        """.trimIndent())
        execute(sendMessage)
    }



}