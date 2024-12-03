package me.kuku.telegram.extension

import com.aallam.openai.api.chat.*
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import com.pengrad.telegrambot.model.PhotoSize
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.EditMessageText
import com.pengrad.telegrambot.request.GetFile
import io.ktor.util.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import me.kuku.telegram.context.AbilitySubscriber
import me.kuku.telegram.context.Locality
import me.kuku.telegram.context.asyncExecute
import me.kuku.telegram.context.byteArray
import me.kuku.telegram.entity.BotConfigService
import me.kuku.telegram.utils.CacheManager
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class OpenaiExtension(
    private val botConfigService: BotConfigService
) {

    private val cache = CacheManager.getCache<String, MutableList<ChatMessage>>("gpt-chat-context", Duration.ofMinutes(2))


    fun AbilitySubscriber.openai() {

        sub(name = "chat", locality = Locality.ALL) {
            val replyToMessage = message.replyToMessage()
            var text = ""
            val photoList = mutableListOf<String>()
            val photoSizeList: Array<PhotoSize>?
            if (replyToMessage != null) {
                text = (replyToMessage.text() ?: "") + message.text()
                photoSizeList = replyToMessage.photo()
            } else {
                text = firstArg()
                photoSizeList = message.photo()
            }
            photoSizeList?.groupBy { it.fileUniqueId().dropLast(1) }?.mapNotNull { (_, group) -> group.maxByOrNull { it.fileSize() } }
                ?.forEach { photoSize ->
                val getFile = GetFile(photoSize.fileId())
                val getFileResponse = bot.asyncExecute(getFile)
                val base64 = getFileResponse.file().byteArray().encodeBase64()
                photoList.add(base64)
            }

            val key = chatId.toString() + message.from().id()

            val cacheBody = cache[key] ?: mutableListOf()

            val botConfigEntity = botConfigService.find()
            if (botConfigEntity.openaiToken.ifEmpty { "" }.isEmpty()) error("not setting openai token")
            val openaiHost = if (botConfigEntity.openaiUrl.isEmpty()) OpenAIHost.OpenAI else OpenAIHost(botConfigEntity.openaiUrl)
            val openaiModel = botConfigEntity.openaiModel.ifEmpty { "gpt-4o-mini" }

            val openai = OpenAI(botConfigEntity.openaiToken, host = openaiHost)

            val chatMessage = ChatMessage(
                role = ChatRole.User,
                content = ContentPartBuilder().also {
                    it.text(text)
                    for (photo in photoList) {
                        it.image("data:image/jpeg;base64,$photo")
                    }
                }.build()
            )

            cacheBody.add(chatMessage)

            val request = ChatCompletionRequest(
                model = ModelId(openaiModel),
                messages = cacheBody,
                streamOptions = streamOptions {
                    includeUsage = true
                }
            )

            runBlocking {
                val response = sendMessage("Processing\\.\\.\\.", parseMode = ParseMode.MarkdownV2, replyToMessageId = message.messageId())
                val sendMessageObject = response.message()
                val sendMessageId = sendMessageObject.messageId()
                var openaiText = ""
                var prefix = ">model: ${openaiModel.replace("-", "\\-")}\n"
                var alreadySendText = ""
                var i = 5
                openai.chatCompletions(request).onEach {
                    it.choices.getOrNull(0)?.delta?.content?.let { content ->
                        openaiText += content
                    }
                    it.usage?.let { usage ->
                        prefix += ">promptToken: ${usage.promptTokens}\n>completionToken: ${usage.completionTokens}\n"
                    }
                    if (i++ % 20 == 0) {
                        val sendText = "$prefix\n```text\n$openaiText```"
                        if (alreadySendText != sendText) {
                            alreadySendText = sendText
                            val editMessageText = EditMessageText(chatId, sendMessageId, sendText)
                                .parseMode(ParseMode.MarkdownV2)
                            bot.asyncExecute(editMessageText)
                        }
                    }
                }.onCompletion {
                    val sendText = "$prefix\n```text\n$openaiText```"
                    cacheBody.add(ChatMessage(ChatRole.Assistant, openaiText))
                    cache[key] = cacheBody
                    if (alreadySendText != sendText) {
                        alreadySendText = sendText
                        val editMessageText = EditMessageText(chatId, sendMessageId, sendText)
                            .parseMode(ParseMode.MarkdownV2)
                        bot.asyncExecute(editMessageText)
                    }
                }.launchIn(this).join()
            }
        }


    }

}