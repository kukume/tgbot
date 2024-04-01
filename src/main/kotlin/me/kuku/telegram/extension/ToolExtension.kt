package me.kuku.telegram.extension

import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.InputMediaPhoto
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.kuku.telegram.context.*
import me.kuku.telegram.entity.BiliBiliService
import me.kuku.telegram.logic.BiliBiliLogic
import me.kuku.telegram.logic.ToolLogic
import me.kuku.telegram.logic.TwitterLogic
import me.kuku.telegram.logic.YgoLogic
import me.kuku.telegram.utils.githubCommit
import me.kuku.utils.*
import org.springframework.stereotype.Service

@Service
class ToolExtension(
    private val ygoLogic: YgoLogic,
    private val toolLogic: ToolLogic,
    private val biliBiliService: BiliBiliService
) {

    private val mutex = Mutex()

    fun MixSubscribe.queryYgoCard() {
        ability {
            sub("ygo", 1) {
                val cardList = ygoLogic.search(firstArg())
                val list = mutableListOf<Array<InlineKeyboardButton>>()
                for (i in cardList.indices) {
                    val card = cardList[i]
                    list.add(arrayOf(InlineKeyboardButton(card.chineseName).callbackData("ygoCard-${card.cardPassword}")))
                }
                sendMessage("请选择查询的卡片", replyKeyboard = InlineKeyboardMarkup(*list.toTypedArray()))
            }
        }
        telegram {
            callbackStartsWith("ygoCard") {
                answerCallbackQuery("获取成功")
                val id = query.data().split("-")[1]
                val card = ygoLogic.searchDetail(id.toLong())
                val sendPhoto = SendPhoto(chatId, OkHttpKtUtils.getBytes(card.imageUrl))
                sendPhoto.caption("中文名：${card.chineseName}\n日文名：${card.japaneseName}\n英文名：${card.englishName}\n效果：\n${card.effect}\n链接：${card.url}")
                bot.asyncExecute(sendPhoto)
            }
        }
    }

    fun AbilitySubscriber.tool() {
        sub("info", locality = Locality.ALL) {
            val id = message.chat().id()
            val messageThreadId = message.messageThreadId()
            sendMessage("""
                chatId: `$id`
                messageThreadId: `$messageThreadId`
            """.trimIndent(), parseMode = ParseMode.Markdown)
        }
        sub("updatelog") {
            val commitList = githubCommit()
            val list = mutableListOf<Array<InlineKeyboardButton>>()
            for (githubCommit in commitList) {
                list.add(arrayOf(InlineKeyboardButton("${githubCommit.date} - ${githubCommit.message}").callbackData("none")))
            }
            sendMessage("更新日志", InlineKeyboardMarkup(*list.stream().limit(6).toList().toTypedArray()))
        }
    }

    fun AbilitySubscriber.loLiCon() {
        sub("lolicon", locality = Locality.ALL) {
            val r18 = kotlin.runCatching {
                if (firstArg().lowercase() == "r18") 1 else 0
            }.getOrDefault(0)
            val jsonNode = OkHttpKtUtils.getJson("https://api.lolicon.app/setu/v2?r18=$r18")
            val url = jsonNode["data"][0]["urls"]["original"].asText()
            val bytes = OkHttpKtUtils.getBytes(url)
            if (bytes.size > 1024 * 10 * 1024) {
                val sendDocument = SendDocument(chatId, bytes).fileName("lolicon.jpg")
                message.messageThreadId()?.let {
                    sendDocument.messageThreadId(it)
                }
                bot.asyncExecute(sendDocument)
            } else {
                val sendPhoto = SendPhoto(chatId, bytes)
                message.messageThreadId()?.let {
                    sendPhoto.messageThreadId(it)
                }
                bot.asyncExecute(sendPhoto)
            }
        }
        sub("loliconmulti", locality = Locality.ALL) {
            val r18 = kotlin.runCatching {
                if (firstArg().lowercase() == "r18") 1 else 0
            }.getOrDefault(0)
            val jsonNode = OkHttpKtUtils.getJson("https://api.lolicon.app/setu/v2?num=5&r18=$r18")
            val list = jsonNode["data"].map { node -> node["urls"]["original"].asText() }
            val inputMediaList = mutableListOf<InputMediaPhoto>()
            for (i in list.indices) {
                val s = list[i]
                val bytes = OkHttpKtUtils.getBytes(s)
                if (bytes.size > 1024 * 10 * 1024) continue
                val mediaPhoto = InputMediaPhoto(bytes)
                inputMediaList.add(mediaPhoto)
            }
            val sendMediaGroup = SendMediaGroup(chatId, *inputMediaList.toTypedArray())
            message.messageThreadId()?.let {
                sendMediaGroup.messageThreadId(it)
            }
            bot.asyncExecute(sendMediaGroup)
        }
        sub("saucenao") {
            val photos = update.message().photo()
            val photoSize = photos.max() ?: error("未发现图片")
            val getFile = GetFile(photoSize.fileId())
            val fileResponse = bot.asyncExecute(getFile)
            val byteArray = fileResponse.file().byteArray()
            val list = toolLogic.saucenao(byteArray)
            if (list.isEmpty()) error("未找到结果")
            val result = list[0]
            sendMessage("""
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

    fun AbilitySubscriber.dyna() {
        sub("bv", input = 1, locality = Locality.ALL) {
            mutex.withLock {
                val biliBiliEntity = biliBiliService.findByTgId(tgId)
                    ?: biliBiliService.findAll().randomOrNull() ?: errorAnswerCallbackQuery("未绑定哔哩哔哩，无法获取视频")
                val bvId = firstArg()
                val file = BiliBiliLogic.videoByBvId(biliBiliEntity, bvId)
                if (file.length() > 1024 * 1024 * 1024 * 2L) {
                    sendMessage("该视频大于2G，无法发送")
                } else {
                    val sendVideo =
                        SendVideo(chatId, file).caption(bvId)
                    messageThreadId?.let { sendVideo.messageThreadId(it) }
                    bot.asyncExecute(sendVideo)
                }
                file.delete()
            }
        }
        sub("x", 1, locality = Locality.ALL) {
            mutex.withLock {
                val id = try {
                    firstArg().toLong()
                } catch (e: NumberFormatException) {
                    MyUtils.regex("(?<=/status/)[0-9]*", firstArg())?.toLong() ?: error("错误的x链接")
                }
                val twitterPojo = TwitterLogic.tweet(id)
                val text = TwitterLogic.convertStr(twitterPojo)
                val videoUrl = if (twitterPojo.videoList.isNotEmpty()) twitterPojo.videoList[0]
                else ""
                try {
                    if (videoUrl.isNotEmpty()) {
                        client.get(videoUrl).body<ByteArray>().let {
                            val sendVideo = SendVideo(chatId, it).fileName("${twitterPojo.id}.mp4")
                                .caption(text)
                            messageThreadId?.let { id -> sendVideo.messageThreadId(id) }
                            bot.asyncExecute(sendVideo)
                        }
                    } else if (twitterPojo.photoList.isNotEmpty() || twitterPojo.forwardPhotoList.isNotEmpty()) {
                        val imageList = twitterPojo.photoList
                        imageList.addAll(twitterPojo.forwardPhotoList)
                        bot.sendPic(chatId, text, imageList, messageThreadId)
                    } else bot.sendTextMessage(chatId, text, messageThreadId)
                } catch (e: Exception) {
                    bot.sendTextMessage(chatId, text, messageThreadId)
                }
            }
        }
    }

}
