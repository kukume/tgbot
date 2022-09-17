package me.kuku.telegram.scheduled

import com.sun.mail.imap.IMAPStore
import me.kuku.telegram.config.TelegramBot
import me.kuku.telegram.entity.MailService
import me.kuku.utils.DateTimeFormatterUtils
import me.kuku.utils.OkHttpKtUtils
import org.jsoup.Jsoup
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeUtility

@Component
class MailScheduled(
    private val mailService: MailService,
    private val telegramBot: TelegramBot
) {


    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    @Transactional
    suspend fun check() {
        val mailList = mailService.findAll()
        for (mailEntity in mailList) {
            val properties = Properties()
            properties.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
            val session = Session.getInstance(properties)
            session.getStore("imap").use { store ->
                kotlin.runCatching {
                    store.connect(mailEntity.host, mailEntity.port, mailEntity.username, mailEntity.password)
                    if (store is IMAPStore) {
                        store.id(mapOf("name" to "kuku", "version" to "1.0.0", "vendor" to "myclient", "support-email" to "kuku@kuku.me"))
                    }
                    store.getFolder("INBOX").use { folder ->
                        folder.open(Folder.READ_WRITE)
                        val messages = folder.messages.also { it.reverse() }
                        for (message in messages) {
                            val flags = message.flags
                            if (!flags.contains(Flags.Flag.SEEN)) {
                                val from = message.from.map { it as InternetAddress }
                                    .joinToString(" ") { MimeUtility.decodeText(it.toUnicodeString()) }
                                val to = message.allRecipients.map { it as InternetAddress }
                                    .joinToString(" ") { it.toUnicodeString() }
                                val localDateTime = message.sentDate.toInstant().atZone(ZoneId.of("+8")).toLocalDateTime()
                                val sendDate = DateTimeFormatterUtils.format(localDateTime, "yyyy-MM-dd")
                                val subject = MimeUtility.decodeText(message.subject)
                                val content = mailContent(message)
                                val document = Jsoup.parse(content.toString())
                                val sb = StringBuilder()
                                sb.appendLine(document.body().wholeText())
                                sb.appendLine("邮件内容已文本展示，其中超链接如下：")
                                document.getElementsByTag("a").forEach { a ->
                                    sb.appendLine("${a.text()}->${a.attr("href")}")
                                }
                                sb.removeSuffix("\n")
                                val jsonNode = OkHttpKtUtils.postJson(
                                    "https://api.jpa.cc/paste",
                                    mapOf("poster" to "tgbot", "syntax" to "text", "content" to sb.toString())
                                )
                                val sendMessage = SendMessage.builder().chatId(mailEntity.tgId)
                                    .text("#邮件推送\n您的邮箱${mailEntity.username}有新邮件了！！\n\n发件人：$from\n收件人：$to\n时间：$sendDate\n主题：$subject\n内容：${jsonNode["url"].asText()}").build()
                                telegramBot.execute(sendMessage)
                                message.setFlag(Flags.Flag.SEEN, true)
                            } else break
                        }
                    }
                }.onFailure {
                    val sendMessage = SendMessage.builder().chatId(mailEntity.tgId)
                        .text("由于异常：${it.message}，邮箱（${mailEntity.username}）信息发送失败").build()
                    telegramBot.execute(sendMessage)
//                    mailService.delete(mailEntity)
                }
            }
        }
    }

    private fun mailContent(part: Part, sb: StringBuilder = StringBuilder()): StringBuilder {
        val isContainTextAttach = part.contentType.indexOf("name") > 0
        return if (part.isMimeType("text/*") && !isContainTextAttach) {
            sb.append(part.content.toString())
            sb
        } else if (part.isMimeType("message/rfc822")) {
            mailContent(part.content as Part, sb)
        } else if (part.isMimeType("multipart/*")) {
            val multipart = part.content as Multipart
            val count = multipart.count
            for (i in 0 until count) {
                val bodyPart = multipart.getBodyPart(i)
                mailContent(bodyPart, sb)
            }
            sb
        } else sb
    }

}