package me.kuku.telegram.extension

import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import me.kuku.telegram.context.AbilitySubscriber
import me.kuku.telegram.context.TelegramSubscribe
import me.kuku.telegram.context.inlineKeyboardButton
import me.kuku.telegram.context.nextMessage
import me.kuku.telegram.entity.Status
import me.kuku.telegram.entity.SwitchEntity
import me.kuku.telegram.entity.SwitchService
import org.bson.types.ObjectId

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

fun TelegramSubscribe.switchCallback() {
    callback("mainSwitch") {
        val list = SwitchService.findByTgId(tgId)
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
            val findList = SwitchService.findByTgId(tgId)
            findList.forEach {
                it.status = Status.OFF
                SwitchService.save(it)
            }
            "主身份"
        } else {
            val switchEntity = SwitchService.findById(ObjectId(id)) ?: error("未找到该身份")
            val list = SwitchService.findByTgIdAndStatus(tgId, Status.ON)
            list.forEach {
                it.status = Status.OFF
                SwitchService.save(it)
            }
            switchEntity.status = Status.ON
            SwitchService.save(switchEntity)
            switchEntity.name
        }
        editMessageText("切换身份${name}成功")
    }
    callback("addSwitch") {
        editMessageText("请发送您添加的身份名称")
        val remark = nextMessage().text()
        val findList = SwitchService.findByTgIdAndName(tgId, remark)
        if (findList.isNotEmpty()) error("该名称已存在")
        val switchEntity = SwitchEntity().also {
            it.name = remark
            it.tgId = tgId
        }
        SwitchService.save(switchEntity)
        editMessageText("添加身份${remark}成功")
    }
    callback("editSwitch") {
        val list = SwitchService.findByTgId(tgId)
        if (list.isEmpty()) error("您还没有添加身份")
        val inlineKeyboardMarkup = InlineKeyboardMarkup(
            list.map { inlineKeyboardButton(it.name, "editSwitch-${it.id}") }.toTypedArray()
        )
        editMessageText("请选择要编辑的身份", inlineKeyboardMarkup)
    }
    callbackStartsWith("editSwitch-") {
        val id = query.data().split("-")[1]
        val switchEntity = SwitchService.findById(ObjectId(id)) ?: error("未找到该身份")
        val oldRemark = switchEntity.name
        editMessageText("请发送您修改后的身份名称")
        val remark = nextMessage().text()
        val findList = SwitchService.findByTgIdAndName(tgId, remark)
        if (findList.isNotEmpty() && findList.first().id != ObjectId(id)) error("该名称已存在")
        switchEntity.name = remark
        SwitchService.save(switchEntity)
        SwitchService.editName(tgId, oldRemark, remark)
        editMessageText("修改身份名成功，新身份名：${remark}，旧身份名：${oldRemark}")
    }
    callback("deleteSwitch") {
        val list = SwitchService.findByTgId(tgId)
        if (list.isEmpty()) error("您还没有添加身份")
        val inlineKeyboardMarkup = InlineKeyboardMarkup(
            list.map { inlineKeyboardButton(it.name, "deleteSwitch-${it.id}") }.toTypedArray()
        )
        editMessageText("请选择要删除的身份", inlineKeyboardMarkup)
    }
    callbackStartsWith("deleteSwitch-") {
        val id = query.data().split("-")[1]
        val switchEntity = SwitchService.findById(ObjectId(id)) ?: error("未找到该身份")
        SwitchService.delete(switchEntity)
        SwitchService.deleteName(tgId, switchEntity.name)
        editMessageText("删除身份${switchEntity.name}成功")
    }
}