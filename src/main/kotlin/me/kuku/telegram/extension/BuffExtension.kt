package me.kuku.telegram.extension

import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.request.SendDocument
import me.kuku.telegram.entity.BuffEntity
import me.kuku.telegram.entity.BuffMonitor
import me.kuku.telegram.entity.BuffService
import me.kuku.telegram.entity.BuffType
import me.kuku.telegram.logic.BuffLogic
import me.kuku.telegram.logic.PaintWearInterval
import me.kuku.telegram.utils.*
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileOutputStream

@Component
class BuffExtension(
    private val buffService: BuffService
) {

    private fun init(): InlineKeyboardMarkup {
        val addBuffButton = inlineKeyboardButton("新增监控", "addBuff")
        val queryBuffButton = inlineKeyboardButton("查询监控", "queryBuff")
        return InlineKeyboardMarkup(
            arrayOf(addBuffButton),
            arrayOf(queryBuffButton)
        )
    }

    fun AbilitySubscriber.buff() {
        sub("buff") {
            buffService.findByTgId(tgId) ?: error("未绑定网易Buff")
            sendMessage("请选择网易buff操作", init())
        }
    }

    fun TelegramSubscribe.addBuff() {
        before { set(buffService.findByTgId(tgId)!!) }

        callback("addBuff") {
            val buffEntity = firstArg<BuffEntity>()
            editMessageText("请发送饰品搜索条件")
            val text = nextMessage().text()
            val searchList = BuffLogic.search(buffEntity, text)
            val buttonList = mutableListOf<Array<InlineKeyboardButton>>()
            for (buffSearch in searchList) {
                buttonList.add(arrayOf(inlineKeyboardButton(buffSearch.name, "buffSearch-${buffSearch.id}")))
            }
            editMessageText("您发送的是$text，请选择具体饰品", InlineKeyboardMarkup(*buttonList.toTypedArray()))
        }

        callbackStartsWith("buffSearch") {
            val goodsId = query.data().split("-")[1].toInt()
            val buttonList = mutableListOf<Array<InlineKeyboardButton>>()
            val goodsInfo = BuffLogic.goodsInfo(goodsId)
            try {
                val paintWearList = BuffLogic.paintWear(goodsId)
                for (paintWearInterval in paintWearList) {
                    val text = "${paintWearInterval.min}-${paintWearInterval.max}"
                    buttonList.add(arrayOf(inlineKeyboardButton(text, "buffWear-$goodsId:${text}")))
                }
                buttonList.add(arrayOf(inlineKeyboardButton("自定义", "buffWear-$goodsId:customer")))
                editMessageText("您选择的是（${goodsInfo.name}），请选择监控的磨损度范围",
                    InlineKeyboardMarkup(*buttonList.toTypedArray()))
            } catch (e: IllegalStateException) {
                buttonList.add(arrayOf(inlineKeyboardButton("推送", "buffType-$goodsId-null-null-1")))
                buttonList.add(arrayOf(inlineKeyboardButton("购买", "buffType-$goodsId-null-null-2")))
                editMessageText("您选择的是（${goodsInfo.name}），请选择监控类型\n推送：每隔一段时间推送该饰品的价格\n购买：每隔一段时间检测价格，符合要求直接购买",
                    InlineKeyboardMarkup(*buttonList.toTypedArray()))
            }
        }

        callbackStartsWith("buffWear") {
            val split = query.data().split(":")
            val goodsId = split[0].split("-")[1].toInt()
            val str = split[1]
            val min: Double
            val max: Double
            if (str == "customer") {
                editMessageText("请发送您能接受的最小磨损")
                min = nextMessage().text().toDoubleOrNull() ?: error("磨损格式不正确")
                editMessageText("请发送您能接受的最高磨损")
                max = nextMessage().text().toDoubleOrNull() ?: error("磨损格式不正确")
            } else {
                val arr = str.split("-")
                min = arr[0].toDouble()
                max = arr[1].toDouble()
            }
            val goodsInfo = BuffLogic.goodsInfo(goodsId)
            val list = arrayOf(
                arrayOf(inlineKeyboardButton("推送", "buffType-$goodsId-$min-$max-1")),
                arrayOf(inlineKeyboardButton("购买", "buffType-$goodsId-$min-$max-2")),
            )
            editMessageText("您选择的是（${goodsInfo.name}），您设置的磨损范围（$min-$max），请选择监控类型\n推送：每隔一段时间推送该饰品的价格\n购买：每隔一段时间检测价格，符合要求直接购买",
                InlineKeyboardMarkup(*list))
        }

        callbackStartsWith("buffType-") {
            val arr = query.data().split("-")
            val goodsId = arr[1].toInt()
            val goodsInfo = BuffLogic.goodsInfo(goodsId)
            val min = arr[2].toDoubleOrNull()
            val max = arr[3].toDoubleOrNull()
            val type = arr.last().toInt()
            if (type == 1) {
                val buffEntity = firstArg<BuffEntity>()
                buffEntity.monitors.add(
                    BuffMonitor(goodsId, goodsInfo.name,
                        PaintWearInterval(min ?: 0.0, max ?: 0.0), 0.0, 0, BuffType.Push)
                )
                buffService.save(buffEntity)
                editMessageText("您选择的是${goodsInfo.name}，您设置的磨损范围（$min-$max），网易buff监控添加成功", top = true)
            } else {
                editMessageText("您选择的是${goodsInfo.name}，您设置的磨损范围（$min-$max），请发送您能接受的最高价格")
                val price = nextMessage().text().toDoubleOrNull() ?: error("您发送的价格不符合规范")
                val prefix = "buffPay-${goodsId}-${min}-${max}-${price}"
                val list = arrayOf(
                    arrayOf(inlineKeyboardButton("Buff余额（支付宝）", "$prefix-3")),
                    arrayOf(inlineKeyboardButton("支付宝花呗", "$prefix-10")),
                    arrayOf(inlineKeyboardButton("Buff余额（银行卡）", "$prefix-1")),
                    arrayOf(inlineKeyboardButton("微信", "$prefix-6"))
                )
                editMessageText("您选择的是${goodsInfo.name}，您设置的磨损范围（$min-$max），您能接受的最高价格为$price，请选择您的支付方式",
                    InlineKeyboardMarkup(*list))
            }
        }

        callbackStartsWith("buffPay") {
            val split = query.data().split("-")
            val goodsId = split[1].toInt()
            val min = split[2].toDouble()
            val max = split[3].toDouble()
            val price = split[4].toDouble()
            val payMethod = split[5].toInt()
            val goodsInfo = BuffLogic.goodsInfo(goodsId)
            val buffEntity = firstArg<BuffEntity>()
            buffEntity.monitors.add(
                BuffMonitor(goodsId, goodsInfo.name, PaintWearInterval(min, max), price, payMethod, BuffType.Buy)
            )
            buffService.save(buffEntity)
            editMessageText("您选择的是${goodsInfo.name}，您设置的磨损范围（$min-$max），您能接受的最高价格为$price，您的支付方式是$payMethod（3是buff余额（支付宝），10是支付宝花呗，1是buff余额（银行卡），6是微信），网易buff监控添加成功", top = true)
        }


    }

    @Volatile
    private var lock = 0

    @Suppress("DuplicatedCode")
    fun TelegramSubscribe.queryBuff() {

        before { set(buffService.findByTgId(tgId)!!) }

        callback("queryBuff") {
            val buffEntity = firstArg<BuffEntity>()
            val list = mutableListOf<Array<InlineKeyboardButton>>()
            for (monitor in buffEntity.monitors) {
                list.add(arrayOf(inlineKeyboardButton(monitor.goodsName, "queryBuff-${monitor.id}")))
            }
            editMessageText("您已设置的监控如下", InlineKeyboardMarkup(*list.toTypedArray()))
        }

        callbackStartsWith("queryBuff-") {
            val uuid = query.data().split("-")[1]
            val buffEntity = firstArg<BuffEntity>()
            val monitor = buffEntity.monitors.find { buff -> buff.id == uuid } ?: error("未查询到这个监控的饰品")
            val list = mutableListOf<Array<InlineKeyboardButton>>()
            list.add(arrayOf(inlineKeyboardButton("编辑", "editBuff-${uuid}")))
            list.add(arrayOf(inlineKeyboardButton("删除", "deleteBuff-${uuid}")))
            list.add(arrayOf(inlineKeyboardButton("导出在售列表", "exportBuffSell-$uuid")))
            editMessageText("""
                您选择的是${monitor.goodsName}
                您设置的类型是${if (monitor.type == BuffType.Push) "推送" else if (monitor.type == BuffType.Buy) "购买" else "取消"}
                您设置的磨损范围（${monitor.paintWearInterval.min}-${monitor.paintWearInterval.max}）
                您能接受的最高价格为${monitor.maxPrice}
                您的支付方式是${monitor.payMethod}（3是buff余额（支付宝），10是支付宝花呗，1是buff余额（银行卡），6是微信）
                请选择您的操作
            """.trimIndent(), InlineKeyboardMarkup(*list.toTypedArray()))
        }

        callbackStartsWith("editBuff-") {
            val uuid = query.data().split("-")[1]
            val inlineKeyboardMarkup = InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("编辑监控类型", "editBuffType-$uuid")),
                arrayOf(inlineKeyboardButton("编辑磨损范围", "editBuffWear-$uuid")),
                arrayOf(inlineKeyboardButton("编辑最高价格", "editBuffPrice-$uuid")),
                arrayOf(inlineKeyboardButton("编辑支付方法", "editBuffPay-$uuid"))
            )
            editMessageText("请选择：", inlineKeyboardMarkup)
        }

        callbackStartsWith("editBuffType-") {
            val uuid = query.data().split("-")[1]
            editMessageText("请选择更改的监控类型，由推送更改为购买请补充磨损率以及价格要求\n推送：每隔一段时间推送该饰品的价格\n购买：每隔一段时间检测价格，符合要求直接购买\n取消：什么都不做",
                InlineKeyboardMarkup(
                    arrayOf(inlineKeyboardButton("推送", "editBuffType2-$-$uuid-1")),
                    arrayOf(inlineKeyboardButton("购买", "editBuffType2-$uuid-2")),
                    arrayOf(inlineKeyboardButton("取消", "editBuffType2-$uuid-3"))
                ))
        }

        callbackStartsWith("editBuffType2-") {
            val arr = query.data().split("-")
            val uuid = arr[1]
            val type = arr[2].toInt()
            val buffEntity = firstArg<BuffEntity>()
            val monitor = buffEntity.monitors.find { it.id == uuid }!!
            monitor.type = if (type == 1) BuffType.Push else if (type == 2) BuffType.Buy else BuffType.Non
            buffService.save(buffEntity)
            editMessageText("更新buff监控类型成功", top = true)
        }

        callbackStartsWith("editBuffPay-") {
            val prefix = "editBuffPay2-${query.data().split("-")[1]}"
            val list = listOf(
                arrayOf(inlineKeyboardButton("Buff余额（支付宝）", "$prefix-3")),
                arrayOf(inlineKeyboardButton("支付宝花呗", "$prefix-10")),
                arrayOf(inlineKeyboardButton("Buff余额（银行卡）", "$prefix-1")),
                arrayOf(inlineKeyboardButton("微信", "$prefix-6"))
            )
            editMessageText("请选择您要修改的支付方式", InlineKeyboardMarkup(*list.toTypedArray()))
        }

        callbackStartsWith("editBuffPay2-") {
            val split = query.data().split("-")
            val uuid = split[1]
            val payMethod = split[2].toInt()
            val buffEntity = firstArg<BuffEntity>()
            buffEntity.monitors.find { s -> s.id == uuid }?.payMethod = payMethod
            buffService.save(buffEntity)
            editMessageText("修改支付方式成功")
        }

        callbackStartsWith("editBuffPrice") {
            val uuid = query.data().split("-")[1]
            editMessageText("请发送你要修改的价格")
            val price = nextMessage().text().toDoubleOrNull() ?: error("价格不符合规范")
            val buffEntity = firstArg<BuffEntity>()
            buffEntity.monitors.find { s -> s.id == uuid }?.maxPrice = price
            buffService.save(buffEntity)
            editMessageText("修改价格成功")
        }

        callbackStartsWith("editBuffWear-") {
            val uuid = query.data().split("-")[1]
            val goodsId = firstArg<BuffEntity>().monitors.find { s -> s.id == uuid }?.goodsId ?: error("未找到这个监控饰品项")
            val paintWearList = BuffLogic.paintWear(goodsId)
            val buttonList = mutableListOf<Array<InlineKeyboardButton>>()
            for (paintWearInterval in paintWearList) {
                val text = "${paintWearInterval.min}-${paintWearInterval.max}"
                buttonList.add(arrayOf(inlineKeyboardButton(text, "editBuffWear2-$uuid:${text}")))
            }
            buttonList.add(arrayOf(inlineKeyboardButton("自定义", "editBuffWear2-$uuid:customer")))
            editMessageText("请选择修改的磨损度范围", InlineKeyboardMarkup(*buttonList.toTypedArray()))
        }

        callbackStartsWith("editBuffWear2-") {
            val split = query.data().split(":")
            val uuid = split[0].split("-")[1]
            val buffEntity = firstArg<BuffEntity>()
            val monitor = buffEntity.monitors.find { s -> s.id == uuid } ?: error("未找到这个监控饰品项")
            val str = split[1]
            val min: Double
            val max: Double
            if (str == "customer") {
                editMessageText("请发送您能接受的最小磨损")
                min = nextMessage().text().toDoubleOrNull() ?: error("磨损格式不正确")
                editMessageText("请发送您能接受的最高磨损")
                max = nextMessage().text().toDoubleOrNull() ?: error("磨损格式不正确")
            } else {
                val arr = str.split("-")
                min = arr[0].toDouble()
                max = arr[1].toDouble()
            }
            monitor.paintWearInterval = PaintWearInterval(min, max)
            buffService.save(buffEntity)
            editMessageText("修改磨损度范围成功")
        }

        callbackStartsWith("deleteBuff") {
            val uuid = query.data().split("-")[1]
            val buffEntity = firstArg<BuffEntity>()
            buffEntity.monitors.removeIf { s -> s.id == uuid }
            buffService.save(buffEntity)
            editMessageText("删除网易buff监控成功", top = true)
        }

        callbackStartsWith("exportBuffSell-") {
            if (lock >= 1) errorAnswerCallbackQuery("已有后台正在导出，请稍后再试", true)
            lock++
            try {
                val uuid = query.data().split("-")[1]
                val monitor = firstArg<BuffEntity>().monitors.find { s -> s.id == uuid }!!
                val paintWearInterval = monitor.paintWearInterval
                answerCallbackQuery("导出已在后台进行中，将在成功的时候发送给您，请注意消息", true)
                val list =
                    BuffLogic.sellRepeat(firstArg(), monitor.goodsId, paintWearInterval.min(), paintWearInterval.max(), 100)
                BuffLogic.export(list).use {
                    val fileName = "tmp" + File.separator + "${System.currentTimeMillis()}-${uuid}.xlsx"
                    val file = File(fileName)
                    FileOutputStream(file).use { os -> it.writeTo(os) }
                    val sendDocument = SendDocument(chatId, file)
                    bot.execute(sendDocument)
                    file.delete()
                }
            } finally {
                lock--
            }
        }

    }




}
