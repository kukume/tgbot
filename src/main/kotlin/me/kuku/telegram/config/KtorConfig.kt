package me.kuku.telegram.config

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

fun Application.config() {

    install(StatusPages) {

        exception<MissingRequestParameterException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("code" to 400, "message" to (cause.message ?: "param not found"))
            )
        }

        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, mapOf("code" to 500, "message" to cause.toString()))
            throw cause
        }
    }

    install(CallLogging)

    install(Thymeleaf) {
        setTemplateResolver(ClassLoaderTemplateResolver().apply {
            prefix = "templates/"
            suffix = ".html"
            characterEncoding = "utf-8"
            isCacheable = false
        })
    }

    install(ContentNegotiation) {
        jackson()
    }

}