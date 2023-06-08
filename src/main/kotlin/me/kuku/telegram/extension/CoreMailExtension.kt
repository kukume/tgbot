package me.kuku.telegram.extension

import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.ParseMode
import me.kuku.telegram.entity.CoreMailEntity
import me.kuku.telegram.entity.CoreMailService
import me.kuku.telegram.logic.CoreMailLogic
import me.kuku.telegram.utils.AbilitySubscriber
import me.kuku.telegram.utils.TelegramSubscribe
import me.kuku.telegram.utils.inlineKeyboardButton
import me.kuku.utils.MyUtils
import org.springframework.stereotype.Component

@Component
class CoreMailExtension(
    private val coreMailLogic: CoreMailLogic,
    private val coreMailService: CoreMailService
) {

    fun AbilitySubscriber.coreMail() {
        sub("coremail") {
            sendMessage("CoreMail管理", InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("新增", "coreMailAdd")),
                arrayOf(inlineKeyboardButton("查询", "coreMailQuery"))
            ))
        }
    }

    fun TelegramSubscribe.mail() {
        callback("coreMailAdd") {
            editMessageText("请发送邮箱的登录地址，仅包含 http:// 或者 https:// 和域名，例如：https://www.google.com")
            val entity = CoreMailEntity()
            entity.tgId = tgId
            val url = nextMessage().text()
            entity.url = url
            val xt = coreMailLogic.xt(entity)
            entity.type = xt
            editMessageText("请发送邮箱的后缀")
            val suffix = nextMessage().text()
            entity.suffix = suffix
            editMessageText("请发送登录用户名，不需要带邮箱后缀")
            val username = nextMessage().text()
            entity.username = username
            editMessageText("请发送登录密码")
            val password = nextMessage().text()
            entity.password = password
            coreMailService.save(entity)
            editMessageText("新增CoreMail信息成功")
        }
        callback("coreMailQuery") {
            val queryList = coreMailService.findByTgId(tgId)
            val list = mutableListOf<Array<InlineKeyboardButton>>()
            for (coreMailEntity in queryList) {
                list.add(
                    arrayOf(inlineKeyboardButton("${coreMailEntity.username}@${coreMailEntity.suffix}", "coreMailQuery-${coreMailEntity.id}"))
                )
            }
            editMessageText("您已添加的CoreMail信息如下", InlineKeyboardMarkup(*list.toTypedArray()))
        }
    }

    fun TelegramSubscribe.manager() {
        before {
            val id = query.data().split("-")[1]
            set(coreMailService.findById(id)!!)
        }
        callbackStartsWith("coreMailQuery-") {
            val entity = firstArg<CoreMailEntity>()
            editMessageText("""
                请选择操作：
                ${entity.mail()}
            """.trimIndent(), InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("编辑", "coreMailEdit-${entity.id}")),
                arrayOf(inlineKeyboardButton("操作", "coreMailManager-${entity.id}"))
            ))
        }
        callbackStartsWith("coreMailEdit-") {
            val entity = firstArg<CoreMailEntity>()
            editMessageText("""
                请选择编辑操作：
                ${entity.mail()}
            """.trimIndent(), InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("编辑url", "coreMailEditUrl-${entity.id}")),
                arrayOf(inlineKeyboardButton("编辑邮箱后缀", "coreMailEditSuffix-${entity.id}")),
                arrayOf(inlineKeyboardButton("编辑用户名", "coreMailEditUsername-${entity.id}")),
                arrayOf(inlineKeyboardButton("编辑密码", "coreMailEditPassword-${entity.id}")),
                arrayOf(inlineKeyboardButton("删除", "coreMailDelete-${entity.id}"))
            ), refreshReturn = true)
        }
        callbackStartsWith("coreMailEditUrl") {
            editMessageText("请发送更改后的url")
            val url = nextMessage().text()
            val entity = firstArg<CoreMailEntity>()
            entity.url = url
            val type = coreMailLogic.xt(entity)
            entity.type = type
            coreMailService.save(entity)
            editMessageText("更改url成功", refreshReturn = true)
        }
        callbackStartsWith("coreMailEditSuffix") {
            editMessageText("请发送更改后的邮箱后缀")
            val suffix = nextMessage().text()
            val entity = firstArg<CoreMailEntity>()
            entity.suffix = suffix
            coreMailService.save(entity)
            editMessageText("更改邮箱后缀成功", refreshReturn = true)
        }
        callbackStartsWith("coreMailEditUsername") {
            editMessageText("请发送更改后的用户名")
            val username = nextMessage().text()
            val entity = firstArg<CoreMailEntity>()
            entity.username = username
            coreMailService.save(entity)
            editMessageText("更改用户名成功", refreshReturn = true)
        }
        callbackStartsWith("coreMailEditPassword") {
            editMessageText("请发送更改后的密码")
            val password = nextMessage().text()
            val entity = firstArg<CoreMailEntity>()
            entity.password = password
            coreMailService.save(entity)
            editMessageText("更改密码成功", refreshReturn = true)
        }
        callbackStartsWith("coreMailDelete") {
            coreMailService.deleteById(query.data().split("-")[1])
            editMessageText("删除成功", refreshReturn = true, goBackStep = 2)
        }

        callbackStartsWith("coreMailManager-") {
            val entity = firstArg<CoreMailEntity>()
            coreMailLogic.login(firstArg())
            val sb = StringBuilder()
            val aliasList = coreMailLogic.alias(entity)
            aliasList.forEach { sb.append("`$it`").append("、") }
            val sbb = StringBuilder()
            coreMailLogic.queryForward(entity).emails.forEach { sbb.append("`$it`").append("、") }
            editMessageText("""
                请选择操作方式：
                别名：${sb.removeSuffix("、")}
                自动转发：${sbb.removeSuffix("、")}
            """.trimIndent(), InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("更新别名", "coreMailEditAlias-${entity.id}"))
            ), refreshReturn = true, parseMode = ParseMode.Markdown)
        }
        callbackStartsWith("coreMailRefreshCookie-") {
            coreMailLogic.login(firstArg())
            editMessageText("刷新cookie成功")
        }
        callbackStartsWith("coreMailEditAlias-") {
            val entity = firstArg<CoreMailEntity>()
            editMessageText("请选择更新别名的方式", InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("自定义", "coreMailEditAliasCustom-${entity.id}")),
                arrayOf(inlineKeyboardButton("随机", "coreMailEditAliasRandom-${entity.id}"))
            ))
        }
        callbackStartsWith("coreMailEditAliasCustom-") {
            editMessageText("请发送更新后的别名")
            val alias = nextMessage().text()
            val entity = firstArg<CoreMailEntity>()
            coreMailLogic.changeAlias(entity, alias)
            editMessageText("更新别名成功，新别名为`$alias@${entity.suffix}`", refreshReturn = true, goBackStep = 2, parseMode = ParseMode.Markdown)
        }
        callbackStartsWith("coreMailEditAliasRandom-") {
            val entity = firstArg<CoreMailEntity>()
            val alias = MyUtils.randomLetterLower(6)
            coreMailLogic.changeAlias(entity, alias)
            editMessageText("更新别名成功，新别名为`$alias@${entity.suffix}`", refreshReturn = true, goBackStep = 2, parseMode = ParseMode.Markdown)
        }
    }

}
