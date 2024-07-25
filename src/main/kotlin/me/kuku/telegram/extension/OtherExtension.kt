package me.kuku.telegram.extension

import me.kuku.telegram.context.TelegramSubscribe

fun TelegramSubscribe.other() {
    callback("notWrite") {
        answerCallbackQuery("没写")
    }
    callback("none") {
    }
}