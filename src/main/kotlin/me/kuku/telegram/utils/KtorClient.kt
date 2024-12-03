package me.kuku.telegram.utils

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*

val client by lazy {
    HttpClient(OkHttp) {
        engine {
            config {
                followRedirects(false)
            }
        }

        followRedirects = false

        install(ContentNegotiation) {
            jackson()
        }

        install(Logging)

    }
}

fun HttpMessageBuilder.cookieString(content: String): Unit = headers.set(HttpHeaders.Cookie, content)

fun HttpMessageBuilder.origin(content: String): Unit = headers.set(HttpHeaders.Origin, content)

fun HttpMessageBuilder.referer(content: String): Unit = headers.set(HttpHeaders.Referrer, content)

fun List<Cookie>.renderCookieHeader(): String {
    return this.filterNot { it.value == "deleted" }.joinToString("") { "${it.name}=${it.value}; " }
}

fun HttpRequestBuilder.setJsonBody(content: Any) {
    contentType(ContentType.Application.Json)
    setBody(content)
}