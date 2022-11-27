package me.kuku.telegram.extension

import me.kuku.telegram.entity.BuffEntity
import me.kuku.telegram.entity.BuffMonitor
import me.kuku.telegram.entity.BuffService
import me.kuku.telegram.logic.BuffLogic
import me.kuku.telegram.logic.PaintWearInterval
import me.kuku.telegram.utils.*
import org.springframework.stereotype.Component
import org.telegram.abilitybots.api.util.AbilityExtension
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
class BuffExtension(
    private val buffService: BuffService
): AbilityExtension {

    private fun init(): InlineKeyboardMarkup {
        val addBuffButton = inlineKeyboardButton("新增", "addBuff")
        val queryBuffButton = inlineKeyboardButton("查询", "queryBuff")
        return InlineKeyboardMarkup(listOf(
            listOf(addBuffButton),
            listOf(queryBuffButton)
        ))
    }

    private fun returnButton(): InlineKeyboardButton {
        return inlineKeyboardButton("返回", "returnBuff")
    }

    fun returnCall() = callback("returnBuff") {
        val messageId = it.message.messageId
        val editMessageText = EditMessageText.builder().text("请选择网易buff操作").chatId(it.message.chatId).replyMarkup(init())
            .messageId(messageId).build()
        execute(editMessageText)
    }

    fun buff() = ability("buff", "网易buff管理") {
        val userid = user().id
        buffService.findByTgId(userid) ?: error("未绑定网易Buff")
        val sendMessage = SendMessage.builder().text("请选择网易buff操作").replyMarkup(init()).chatId(chatId()).build()
        execute(sendMessage)
    }

    fun addBuff() = callback {

        before {
            val userid = it.from.id
            val buffEntity = buffService.findByTgId(userid)!!
            set("buffEntity", buffEntity)
        }

        query("addBuff") {
            val chatId = it.message.chatId
            val messageId = it.message.messageId
            val buffEntity = firstArg<BuffEntity>()
            val middleMessage = execute(SendMessage.builder().chatId(chatId).text("请发送饰品搜索条件").build())
            val userMessage = it.waitNextMessage()
            val text = userMessage.text
            val searchList = BuffLogic.search(buffEntity, text)
            val buttonList = mutableListOf<List<InlineKeyboardButton>>()
            for (buffSearch in searchList) {
                buttonList.add(listOf(inlineKeyboardButton(buffSearch.name, "buffSearch-${buffSearch.id}")))
            }
            buttonList.add(listOf(returnButton()))
            val editMessageText = EditMessageText.builder().text("您发送的是$text，请选择具体饰品").chatId(chatId)
                .replyMarkup(InlineKeyboardMarkup(buttonList)).messageId(messageId).build()
            var deleteMessage = DeleteMessage.builder().chatId(chatId).messageId(middleMessage.messageId).build()
            execute(deleteMessage)
            deleteMessage = DeleteMessage.builder().chatId(chatId).messageId(userMessage.messageId).build()
            execute(deleteMessage)
            execute(editMessageText)
        }

        queryStartWith("buffSearch") {
            val chatId = it.message.chatId
            val goodsId = it.data.split("-")[1].toInt()
            val paintWearList = BuffLogic.paintWear(goodsId)
            val buttonList = mutableListOf<List<InlineKeyboardButton>>()
            for (paintWearInterval in paintWearList) {
                val text = "${paintWearInterval.min}-${paintWearInterval.max}"
                buttonList.add(listOf(inlineKeyboardButton(text, "buffWear-$goodsId:${text}")))
            }
            buttonList.add(listOf(inlineKeyboardButton("自定义", "buffWear-$goodsId:customer")))
            buttonList.add(listOf(returnButton()))
            val goodsInfo = BuffLogic.goodsInfo(goodsId)
            val sendMessage = EditMessageText.builder().text("您选择的是（${goodsInfo.name}），请选择监控的磨损度范围").chatId(chatId)
                .replyMarkup(InlineKeyboardMarkup(buttonList)).messageId(it.message.messageId).build()
            execute(sendMessage)
        }

        queryStartWith("buffWear") {
            val split = it.data.split(":")
            val goodsId = split[0].split("-")[1].toInt()
            val str = split[1]
            val chatId = it.message.chatId
            val messageId = it.message.messageId
            val min: Double
            val max: Double
            if (str == "customer") {
                val botSendMessage1 = execute(SendMessage.builder().text("请发送您能接受的最小磨损").chatId(chatId).build())
                val minMessage = it.waitNextMessage()
                min = minMessage.text.toDoubleOrNull() ?: error("磨损格式不正确")
                val botSendMessage2 = execute(SendMessage.builder().text("请发送您能接受的最高磨损").chatId(chatId).build())
                val maxMessage = it.waitNextMessage()
                max = maxMessage.text.toDoubleOrNull() ?: error("磨损格式不正确")
                execute(DeleteMessage.builder().chatId(chatId).messageId(botSendMessage1.messageId).build())
                execute(DeleteMessage.builder().chatId(chatId).messageId(botSendMessage2.messageId).build())
                execute(DeleteMessage.builder().chatId(chatId).messageId(minMessage.messageId).build())
                execute(DeleteMessage.builder().chatId(chatId).messageId(maxMessage.messageId).build())
            } else {
                val arr = str.split("-")
                min = arr[0].toDouble()
                max = arr[1].toDouble()
            }
            val goodsInfo = BuffLogic.goodsInfo(goodsId)
            val sendMessage = execute(
                SendMessage.builder().text("您选择的是${goodsInfo.name}，您设置的磨损范围（$min-$max），请发送您能接受的最高价格").chatId(chatId)
                    .build()
            )
            val priceMessage = it.waitNextMessage()
            val price = priceMessage.text.toDoubleOrNull() ?: error("您发送的价格不符合规范")
            execute(DeleteMessage.builder().chatId(chatId).messageId(sendMessage.messageId).build())
            execute(DeleteMessage.builder().chatId(chatId).messageId(priceMessage.messageId).build())
            val prefix = "buffPay-${goodsId}-${min}-${max}-${price}"
            val list = listOf(
                listOf(inlineKeyboardButton("Buff余额（支付宝）", "$prefix-3")),
                listOf(inlineKeyboardButton("支付宝花呗", "$prefix-10")),
                listOf(inlineKeyboardButton("Buff余额（银行卡）", "$prefix-1")),
                listOf(inlineKeyboardButton("微信", "$prefix-6")),
                listOf(returnButton())
            )
            val editMessageText = EditMessageText.builder()
                .text("您选择的是${goodsInfo.name}，您设置的磨损范围（$min-$max），您能接受的最高价格为$price，请选择您的支付方式")
                .replyMarkup(InlineKeyboardMarkup(list)).chatId(chatId).messageId(messageId).build()
            execute(editMessageText)
        }

        queryStartWith("buffPay") {
            val split = it.data.split("-")
            val goodsId = split[1].toInt()
            val min = split[2].toDouble()
            val max = split[3].toDouble()
            val price = split[4].toDouble()
            val payMethod = split[5].toInt()
            val goodsInfo = BuffLogic.goodsInfo(goodsId)
            val buffEntity = firstArg<BuffEntity>()
            buffEntity.monitors.add(BuffMonitor(goodsId, goodsInfo.name, PaintWearInterval(min, max), price, payMethod))
            buffService.save(buffEntity)
            val editMessageText = EditMessageText.builder()
                .text("您选择的是${goodsInfo.name}，您设置的磨损范围（$min-$max），您能接受的最高价格为$price，您的支付方式是$payMethod（3是buff余额（支付宝），10是支付宝花呗，1是buff余额（银行卡），6是微信），网易buff监控添加成功")
                .replyMarkup(InlineKeyboardMarkup(listOf(listOf(returnButton())))).chatId(it.message.chatId).messageId(it.message.messageId).build()
            execute(editMessageText)
        }


    }

    @Suppress("DuplicatedCode")
    fun queryBuff() = callback {

        before {
            val userid = it.from.id
            val buffEntity = buffService.findByTgId(userid)!!
            set("buffEntity", buffEntity)
        }

        query("queryBuff") {
            val buffEntity = firstArg<BuffEntity>()
            val list = mutableListOf<List<InlineKeyboardButton>>()
            for (monitor in buffEntity.monitors) {
                list.add(listOf(inlineKeyboardButton(monitor.goodsName, "queryBuff-${monitor.id}")))
            }
            list.add(listOf(returnButton()))
            val editMessageText = EditMessageText.builder().text("您已设置的监控如下").messageId(it.message.messageId)
                .chatId(it.message.chatId)
                .replyMarkup(InlineKeyboardMarkup(list))
                .build()
            execute(editMessageText)
        }

        queryStartWith("queryBuff-") {
            val uuid = it.data.split("-")[1]
            val buffEntity = firstArg<BuffEntity>()
            val monitor = buffEntity.monitors.find { buff -> buff.id == uuid } ?: error("未查询到这个监控的饰品")
            val list = mutableListOf<List<InlineKeyboardButton>>()
            list.add(listOf(inlineKeyboardButton("编辑", "editBuff-${uuid}")))
            list.add(listOf(inlineKeyboardButton("删除", "deleteBuff-${uuid}")))
            list.add(listOf(returnButton()))
            val editMessageText = EditMessageText.builder().text("""
                您选择的是${monitor.goodsName}，您设置的磨损范围（${monitor.paintWearInterval.min}-${monitor.paintWearInterval.max}），您能接受的最高价格为${monitor.maxPrice}，您的支付方式是${monitor.payMethod}（3是buff余额（支付宝），10是支付宝花呗，1是buff余额（银行卡），6是微信）
                请选择您的操作
            """.trimIndent()).messageId(it.message.messageId).replyMarkup(InlineKeyboardMarkup(list)).chatId(it.message.chatId).build()
            execute(editMessageText)
        }

        queryStartWith("editBuff-") {
            val uuid = it.data.split("-")[1]
            val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(
                listOf(inlineKeyboardButton("编辑磨损范围", "editBuffWear-$uuid")),
                listOf(inlineKeyboardButton("编辑最高价格", "editBuffPrice-$uuid")),
                listOf(inlineKeyboardButton("编辑支付方法", "editBuffPay-$uuid")),
                listOf(returnButton())
            ))
            val editMessageText = EditMessageText.builder().text("请选择：").replyMarkup(inlineKeyboardMarkup)
                .messageId(it.message.messageId).chatId(it.message.chatId).build()
            execute(editMessageText)
        }

        queryStartWith("editBuffPay-") {
            val prefix = "editBuffPay2-${it.data.split("-")[1]}"
            val list = listOf(
                listOf(inlineKeyboardButton("Buff余额（支付宝）", "$prefix-3")),
                listOf(inlineKeyboardButton("支付宝花呗", "$prefix-10")),
                listOf(inlineKeyboardButton("Buff余额（银行卡）", "$prefix-1")),
                listOf(inlineKeyboardButton("微信", "$prefix-6")),
                listOf(returnButton())
            )
            val editMessageText = EditMessageText.builder()
                .text("请选择您要修改的支付方式")
                .replyMarkup(InlineKeyboardMarkup(list)).chatId(it.message.chatId).messageId(it.message.messageId).build()
            execute(editMessageText)
        }

        queryStartWith("editBuffPay2-") {
            val split = it.data.split("-")
            val uuid = split[1]
            val payMethod = split[2].toInt()
            val buffEntity = firstArg<BuffEntity>()
            buffEntity.monitors.find { s -> s.id == uuid }?.payMethod = payMethod
            buffService.save(buffEntity)
            val editMessageText = EditMessageText.builder()
                .text("修改支付方式成功")
                .replyMarkup(InlineKeyboardMarkup(listOf(listOf(returnButton())))).chatId(it.message.chatId).messageId(it.message.messageId).build()
            execute(editMessageText)
        }

        queryStartWith("editBuffPrice") {
            val uuid = it.data.split("-")[1]
            val chatId = it.message.chatId
            val sendMessage = execute(SendMessage.builder().chatId(chatId).text("请发送你要修改的价格").build())
            val priceMessage = it.waitNextMessage()
            val price = priceMessage.text.toDoubleOrNull() ?: error("价格不符合规范")
            val buffEntity = firstArg<BuffEntity>()
            buffEntity.monitors.find { s -> s.id == uuid }?.maxPrice = price
            buffService.save(buffEntity)
            execute(DeleteMessage.builder().chatId(chatId).messageId(sendMessage.messageId).build())
            execute(DeleteMessage.builder().chatId(chatId).messageId(priceMessage.messageId).build())
            val editMessageText = EditMessageText.builder()
                .text("修改价格成功")
                .replyMarkup(InlineKeyboardMarkup(listOf(listOf(returnButton())))).chatId(it.message.chatId).messageId(it.message.messageId).build()
            execute(editMessageText)
        }

        queryStartWith("editBuffWear-") {
            val uuid = it.data.split("-")[1]
            val goodsId = firstArg<BuffEntity>().monitors.find { s -> s.id == uuid }?.goodsId ?: error("未找到这个监控饰品项")
            val paintWearList = BuffLogic.paintWear(goodsId)
            val buttonList = mutableListOf<List<InlineKeyboardButton>>()
            for (paintWearInterval in paintWearList) {
                val text = "${paintWearInterval.min}-${paintWearInterval.max}"
                buttonList.add(listOf(inlineKeyboardButton(text, "editBuffWear2-$uuid:${text}")))
            }
            buttonList.add(listOf(inlineKeyboardButton("自定义", "editBuffWear2-$uuid:customer")))
            buttonList.add(listOf(returnButton()))
            val sendMessage = EditMessageText.builder().text("请选择修改的磨损度范围").chatId(it.message.chatId)
                .replyMarkup(InlineKeyboardMarkup(buttonList)).messageId(it.message.messageId).build()
            execute(sendMessage)
        }

        queryStartWith("editBuffWear2-") {
            val chatId = it.message.chatId
            val split = it.data.split(":")
            val uuid = split[0].split("-")[1]
            val buffEntity = firstArg<BuffEntity>()
            val monitor = buffEntity.monitors.find { s -> s.id == uuid } ?: error("未找到这个监控饰品项")
            val str = split[1]
            val min: Double
            val max: Double
            if (str == "customer") {
                val botSendMessage1 = execute(SendMessage.builder().text("请发送您能接受的最小磨损").chatId(chatId).build())
                val minMessage = it.waitNextMessage()
                min = minMessage.text.toDoubleOrNull() ?: error("磨损格式不正确")
                val botSendMessage2 = execute(SendMessage.builder().text("请发送您能接受的最高磨损").chatId(chatId).build())
                val maxMessage = it.waitNextMessage()
                max = maxMessage.text.toDoubleOrNull() ?: error("磨损格式不正确")
                execute(DeleteMessage.builder().chatId(chatId).messageId(botSendMessage1.messageId).build())
                execute(DeleteMessage.builder().chatId(chatId).messageId(botSendMessage2.messageId).build())
                execute(DeleteMessage.builder().chatId(chatId).messageId(minMessage.messageId).build())
                execute(DeleteMessage.builder().chatId(chatId).messageId(maxMessage.messageId).build())
            } else {
                val arr = str.split("-")
                min = arr[0].toDouble()
                max = arr[1].toDouble()
            }
            monitor.paintWearInterval = PaintWearInterval(min, max)
            buffService.save(buffEntity)
            val editMessageText = EditMessageText.builder().text("修改磨损度范围成功")
                .chatId(it.message.chatId).messageId(it.message.messageId).replyMarkup(InlineKeyboardMarkup(listOf(listOf(returnButton())))).build()
            execute(editMessageText)
        }

        queryStartWith("deleteBuff") {
            val uuid = it.data.split("-")[1]
            val buffEntity = firstArg<BuffEntity>()
            buffEntity.monitors.removeIf { s -> s.id == uuid }
            buffService.save(buffEntity)
            val editMessageText = EditMessageText.builder().text("删除网易buff监控成功")
                .chatId(it.message.chatId).messageId(it.message.messageId).replyMarkup(InlineKeyboardMarkup(listOf(listOf(returnButton())))).build()
            execute(editMessageText)
        }

    }




}