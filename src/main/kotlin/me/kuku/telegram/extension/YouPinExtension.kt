package me.kuku.telegram.extension

import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import me.kuku.telegram.entity.YouPinEntity
import me.kuku.telegram.entity.YouPinService
import me.kuku.telegram.logic.YouPinLogic
import me.kuku.telegram.utils.AbilitySubscriber
import me.kuku.telegram.utils.TelegramSubscribe
import me.kuku.telegram.utils.errorAnswerCallbackQuery
import me.kuku.telegram.utils.inlineKeyboardButton
import org.springframework.stereotype.Component

@Component
class YouPinExtension(
    private val youPinService: YouPinService
) {

    fun AbilitySubscriber.youPin() {
        sub("uuyp") {
            sendMessage("请选择悠悠有品", InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("新增监控", "addYouPin")),
                arrayOf(inlineKeyboardButton("查询监控", "queryYouPin"))
            ))
        }
    }

    fun TelegramSubscribe.youPin() {
        before { set(youPinService.findByTgId(tgId) ?: errorAnswerCallbackQuery("未绑定悠悠有品")) }
        callback("addYouPin") {
            editMessageText("请发送饰品名字")
            val keyword = nextMessage().text()
            val match = YouPinLogic.match(firstArg(), keyword)
            val array = arrayOfNulls<Array<InlineKeyboardButton>>(match.size)
            for ((i, item) in match.withIndex()) {
                array[i] = arrayOf(inlineKeyboardButton(item.commodityName, "youPinMatch-${item.templateId}"))
            }
            editMessageText("请选择饰品，如果没有您想选择的饰品， 请重新发送详细的饰品名字", InlineKeyboardMarkup(*array))
        }
        callbackStartsWith("youPinMatch-") {
            val id = query.data().split("-")[1].toInt()
            val market = YouPinLogic.market(firstArg(), id)
            if (market.haveAbrade()) {
                val abrade = market.abrade()!!
                val items = abrade.items!!
                val array = arrayOfNulls<Array<InlineKeyboardButton>>(items.size + 1)
                for ((i, item) in items.withIndex()) {
                    array[i] = arrayOf(inlineKeyboardButton(item.name, "youPinAbrade-$id-${item.minVal}-${item.maxVal}"))
                }
                array[items.size] = arrayOf(inlineKeyboardButton("自定义", "youPinAbradeCustom-$id"))
                editMessageText("请选择监控的该饰品的磨损", InlineKeyboardMarkup(*array))
            } else {
                editMessageText("请选择监控类别", InlineKeyboardMarkup(
                    arrayOf(inlineKeyboardButton("推送", "youPinMonitor-$id-null-null-1"))
                ))
            }
        }
        callbackStartsWith("youPinAbradeCustom-") {
            val id = query.data().split("-")[1].toInt()
            editMessageText("请发送磨损区间的最小值")
            val min = nextMessage().text().toDouble()
            editMessageText("请发送磨损区间的最大值")
            val max = nextMessage().text().toDouble()
            editMessageText("请选择监控类别", InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("推送", "youPinMonitor-$id-$min-$max-1"))
            ))
        }
        callbackStartsWith("youPinAbrade-") {
            val arr = query.data().split("-")
            val id = arr[1].toInt()
            val min = arr[2].toDouble()
            val max = arr[3].toDouble()
            editMessageText("请选择监控类别", InlineKeyboardMarkup(
                arrayOf(inlineKeyboardButton("推送", "youPinMonitor-$id-$min-$max-1"))
            ))
        }
        callbackStartsWith("youPinMonitor-") {
            val arr = query.data().split("-")
            val id = arr[1].toInt()
            val min = arr[2].toDoubleOrNull()
            val max = arr[3].toDoubleOrNull()
            val type = arr[4].toInt()
            val entity = firstArg<YouPinEntity>()
            val market = YouPinLogic.market(entity, id)
            if (type == 1) {
                val monitor = YouPinEntity.Monitor()
                monitor.templateId = id
                monitor.templateName = market.templateInfo.name
                monitor.minAbrade = min
                monitor.maxAbrade = max
                monitor.type = YouPinEntity.Monitor.Type.Push
                entity.monitors.add(monitor)
                youPinService.save(entity)
                editMessageText("添加悠悠有品监控成功", top = true)
            }
        }


        callback("queryYouPin") {
            val monitors = firstArg<YouPinEntity>().monitors
            val array = arrayOfNulls<Array<InlineKeyboardButton>>(monitors.size)
            for ((i, monitor) in monitors.withIndex()) {
                array[i] = arrayOf(inlineKeyboardButton(monitor.templateName, "queryYouPin-${monitor.id}"))
            }
            editMessageText("请选择查看详情", InlineKeyboardMarkup(*array))
        }
        callbackStartsWith("queryYouPin-") {
            val monitor = firstArg<YouPinEntity>().monitors
                .find { it.id == query.data().split("-")[1] }!!
            editMessageText("""
                悠悠有品监控详情
                名称：${monitor.templateName}
                最小磨损：${monitor.minAbrade}
                最大磨损：${monitor.maxAbrade}
                类型：${monitor.type.str()}
            """.trimIndent())
        }
    }

}
