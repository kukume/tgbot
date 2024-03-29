package me.kuku.telegram.extension

import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import me.kuku.telegram.context.AbilitySubscriber
import me.kuku.telegram.context.TelegramSubscribe
import me.kuku.telegram.context.inlineKeyboardButton
import me.kuku.telegram.context.nextMessage
import me.kuku.telegram.entity.Status
import me.kuku.telegram.entity.SwitchEntity
import me.kuku.telegram.entity.SwitchService
import org.springframework.stereotype.Component

@Component
class SwitchExtension(
    private val switchService: SwitchService
) {

    fun AbilitySubscriber.switch() {
        sub("switch") {
            val inlineKeyboardMarkup = InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("切换身份", "mainSwitch")),
                arrayOf(inlineKeyboardButton("添加身份", "addSwitch")),
                arrayOf(inlineKeyboardButton("编辑身份", "editSwitch")),
                arrayOf(inlineKeyboardButton("删除身份", "deleteSwitch"))
            )
            sendMessage("""
                请选择身份操作
            """.trimIndent(), inlineKeyboardMarkup)
        }
    }

    fun TelegramSubscribe.switch() {
        callback("mainSwitch") {
            val list = switchService.findByTgId(tgId)
            val enable = list.find { it.status == Status.ON }
            val str = if (enable == null) "主身份"
            else list.first().name
            val inlineKeyboardMarkup = InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("主身份", "mainSwitch-0")),
                list.map { inlineKeyboardButton(it.name, "mainSwitch-${it.id}") }.toTypedArray()
            )
            editMessageText("请选择需要切换的身份\n当前激活身份：$str", inlineKeyboardMarkup)
        }
        callbackStartsWith("mainSwitch-") {
            val id = query.data().split("-")[1]
            val name = if (id == "0") {
                val findList = switchService.findByTgId(tgId)
                findList.forEach {
                    it.status = Status.OFF
                    switchService.save(it)
                }
                "主身份"
            } else {
                val switchEntity = switchService.findById(id) ?: error("未找到该身份")
                val list = switchService.findByTgIdAndStatus(tgId, Status.ON)
                list.forEach {
                    it.status = Status.OFF
                    switchService.save(it)
                }
                switchEntity.status = Status.ON
                switchService.save(switchEntity)
                switchEntity.name
            }
            editMessageText("切换身份${name}成功")
        }
        callback("addSwitch") {
            editMessageText("请发送您添加的身份名称")
            val remark = nextMessage().text()
            val findList = switchService.findByTgIdAndName(tgId, remark)
            if (findList.isNotEmpty()) error("该名称已存在")
            val switchEntity = SwitchEntity().also {
                it.name = remark
                it.tgId = tgId
            }
            switchService.save(switchEntity)
            editMessageText("添加身份${remark}成功")
        }
        callback("editSwitch") {
            val list = switchService.findByTgId(tgId)
            if (list.isEmpty()) error("您还没有添加身份")
            val inlineKeyboardMarkup = InlineKeyboardMarkup(
                list.map { inlineKeyboardButton(it.name, "editSwitch-${it.id}") }.toTypedArray()
            )
            editMessageText("请选择要编辑的身份", inlineKeyboardMarkup)
        }
        callbackStartsWith("editSwitch-") {
            val id = query.data().split("-")[1]
            val switchEntity = switchService.findById(id) ?: error("未找到该身份")
            val oldRemark = switchEntity.name
            editMessageText("请发送您修改后的身份名称")
            val remark = nextMessage().text()
            val findList = switchService.findByTgIdAndName(tgId, remark)
            if (findList.isNotEmpty() && findList.first().id != id) error("该名称已存在")
            switchEntity.name = remark
            switchService.save(switchEntity)
            switchService.editName(tgId, oldRemark, remark)
            editMessageText("修改身份名成功，新身份名：${remark}，旧身份名：${oldRemark}")
        }
        callback("deleteSwitch") {
            val list = switchService.findByTgId(tgId)
            if (list.isEmpty()) error("您还没有添加身份")
            val inlineKeyboardMarkup = InlineKeyboardMarkup(
                list.map { inlineKeyboardButton(it.name, "deleteSwitch-${it.id}") }.toTypedArray()
            )
            editMessageText("请选择要删除的身份", inlineKeyboardMarkup)
        }
        callbackStartsWith("deleteSwitch-") {
            val id = query.data().split("-")[1]
            val switchEntity = switchService.findById(id) ?: error("未找到该身份")
            switchService.delete(switchEntity)
            switchService.deleteName(tgId, switchEntity.name)
            editMessageText("删除身份${switchEntity.name}成功")
        }
    }

}