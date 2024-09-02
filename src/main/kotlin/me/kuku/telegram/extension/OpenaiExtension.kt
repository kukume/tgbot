package me.kuku.telegram.extension

import com.fasterxml.jackson.databind.JsonNode
import com.pengrad.telegrambot.model.PhotoSize
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.GetFile
import io.ktor.client.call.*
import io.ktor.client.request.*
import me.kuku.telegram.context.AbilitySubscriber
import me.kuku.telegram.context.Locality
import me.kuku.telegram.context.asyncExecute
import me.kuku.telegram.context.byteArray
import me.kuku.telegram.entity.BotConfigService
import me.kuku.utils.Jackson
import me.kuku.utils.base64Encode
import me.kuku.utils.client
import me.kuku.utils.setJsonBody
import org.springframework.stereotype.Component

@Component
class OpenaiExtension(
    private val botConfigService: BotConfigService
) {

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
                text = message.text()
                photoSizeList = message.photo()
            }
            photoSizeList?.groupBy { it.fileUniqueId().dropLast(1) }?.mapNotNull { (_, group) -> group.maxByOrNull { it.fileSize() } }
                ?.forEach { photoSize ->
                val getFile = GetFile(photoSize.fileId())
                val getFileResponse = bot.asyncExecute(getFile)
                val base64 = getFileResponse.file().byteArray().base64Encode()
                photoList.add(base64)
            }
            val botConfigEntity = botConfigService.find()
            if (botConfigEntity.openaiToken.ifEmpty { "" }.isEmpty()) error("not setting openai token")
            val params = Jackson.createObjectNode()
            params.put("model", "gpt-4o-mini")
            val messages = Jackson.createArrayNode()
            val singleMessage = Jackson.createObjectNode()
            singleMessage.put("role", "user")
            if (photoList.isEmpty()) {
                singleMessage.put("content", text)
            } else {
                val content = Jackson.createArrayNode()
                val textContent = Jackson.createObjectNode()
                textContent.put("type", "text")
                textContent.put("text", text)
                content.add(textContent)
                for (photo in photoList) {
                    val imageContent = Jackson.createObjectNode()
                    imageContent.put("type", "image_url")
                    val imageUrl = Jackson.createObjectNode()
                    imageUrl.put("url", "data:image/jpeg;base64,$photo")
                    imageContent.putIfAbsent("image_url", imageUrl)
                    content.add(imageContent)
                }
                singleMessage.putIfAbsent("content", content)
            }
            messages.add(singleMessage)
            params.putIfAbsent("messages", messages)
            val jsonNode = client.post("https://api.openai.com/v1/chat/completions") {
                setJsonBody(params)
                headers {
                    append("Authorization", "Bearer ${botConfigEntity.openaiToken}")
                }
            }.body<JsonNode>()
            val responseText = "```text\n" + jsonNode["choices"][0]["message"]["content"].asText() + "\n```"
            val usage = jsonNode["usage"]
            val promptToken = usage["prompt_tokens"].asInt()
            val completionToken = usage["completion_tokens"].asInt()
            sendMessage(">model: gpt\\-4o\\-mini\n>promptToken: $promptToken\n>completionToken: $completionToken\n\n$responseText",
                parseMode = ParseMode.MarkdownV2,
                replyToMessageId = message.messageId())
        }


    }

}