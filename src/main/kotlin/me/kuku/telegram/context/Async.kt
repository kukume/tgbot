package me.kuku.telegram.context

import com.pengrad.telegrambot.Callback
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.BaseRequest
import com.pengrad.telegrambot.response.BaseResponse
import kotlinx.coroutines.CompletableDeferred
import java.io.IOException

suspend fun <T : BaseRequest<T, R>, R : BaseResponse> TelegramBot.asyncExecute(request: T): R {
    val completableDeferred = CompletableDeferred<R>()
    this.execute(request, object: Callback<T, R> {
        override fun onResponse(request: T, response: R) {
            if (response.isOk) {
                completableDeferred.complete(response)
            } else {
                completableDeferred.completeExceptionally(IllegalStateException(response.description()))
            }
        }

        override fun onFailure(request: T, e: IOException) {
            completableDeferred.completeExceptionally(e)
        }
    })
    return completableDeferred.await()
}