package me.kuku.telegram.extension

import me.kuku.telegram.entity.MailEntity
import me.kuku.telegram.entity.MailService
import me.kuku.telegram.utils.*
import org.springframework.stereotype.Component
import org.telegram.abilitybots.api.util.AbilityExtension
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.util.*
import javax.mail.Session

@Component
class MailExtension(
    private val mailService: MailService
): AbilityExtension {

    fun markup(): InlineKeyboardMarkup {
        val addButton = inlineKeyboardButton("新增", "mailAdd")
        val queryButton = inlineKeyboardButton("查询", "mailQuery")
        return InlineKeyboardMarkup(listOf(
            listOf(addButton),
            listOf(queryButton)
        ))
    }

    fun returnButton() = listOf(inlineKeyboardButton("返回", "mailReturn"))

    fun mailReturn() = callback("mailReturn") {
        val chatId = it.message.chatId
        val messageId = it.message.messageId
        val editMessageText = EditMessageText.builder().chatId(chatId)
            .messageId(messageId).text("请选择：").replyMarkup(markup()).build()
        execute(editMessageText)
    }

    fun mail() = ability("mail", "邮件推送，将会推送您的未读邮件到已读为止") {
        val sendMessage = SendMessage.builder().chatId(chatId().toString())
            .text("邮箱推送操作，请选择：").replyMarkup(markup()).build()
        execute(sendMessage)
    }

    fun mailOp() = callback {
        query("mailAdd") {
            val chatId = it.message.chatId
            val tgId = it.from.id
            execute(SendMessage.builder().chatId(chatId).text("请发送邮件imap服务器的地址").build())
            val host = it.waitNextMessage().text
            execute(SendMessage.builder().chatId(chatId).text("请发送邮件imap服务器的端口").build())
            val port = it.waitNextMessage().text.toIntOrNull() ?: error("错误的端口")
            execute(SendMessage.builder().chatId(chatId).text("请发送您的邮箱").build())
            val username = it.waitNextMessage().text
            val mailEntity = mailService.findByTgIdAndUsername(tgId, username)
            if (mailEntity != null) error("您已添加该邮箱")
            execute(SendMessage.builder().chatId(chatId).text("请发送您的密码").build())
            val password = it.waitNextMessage().text
            val properties = Properties()
            properties.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
            val session = Session.getInstance(properties)
            session.getStore("imap").use { store ->
                store.connect(host, port, username, password)
            }
            val saveEntity = MailEntity().also { entity ->
                entity.host = host
                entity.tgId = tgId
                entity.port = port
                entity.username = username
                entity.password = password
            }
            mailService.save(saveEntity)
            execute(SendMessage.builder().chatId(chatId).text("添加邮箱${username}成功").build())
        }
        fun markup(list: List<MailEntity>): InlineKeyboardMarkup {
            val buttonList = mutableListOf<List<InlineKeyboardButton>>()
            for (mailEntity in list) {
                buttonList.add(listOf(inlineKeyboardButton(mailEntity.username, "mailDelete-${mailEntity.username}")))
            }
            buttonList.add(returnButton())
            return InlineKeyboardMarkup(buttonList)
        }
        query("mailQuery") {
            val tgId = it.from.id
            val chatId = it.message.chatId
            val messageId = it.message.messageId
            val list = mailService.findByTgId(tgId)
            val editMessageText = EditMessageText.builder().chatId(chatId).messageId(messageId)
                .replyMarkup(markup(list))
                .text("您已提交的邮箱，点击按钮可删除").build()
            execute(editMessageText)
        }
        queryStartWith("mailDelete") {
            val username = it.data.split("-")[1]
            val tgId = it.from.id
            val chatId = it.message.chatId
            val messageId = it.message.messageId
            mailService.deleteByTgIdAndUsername(tgId, username)
            val list = mailService.findByTgId(tgId)
            val editMessageText = EditMessageText.builder().chatId(chatId).messageId(messageId)
                .replyMarkup(markup(list))
                .text("您已提交的邮箱，点击按钮可删除").build()
            execute(editMessageText)
        }
    }

}