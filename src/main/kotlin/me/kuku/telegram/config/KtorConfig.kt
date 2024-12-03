package me.kuku.telegram.config

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*


fun Application.statusPages() {

    install(StatusPages) {

        exception<MissingRequestParameterException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("code" to 400, "message" to cause.message)
            )
        }

        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, mapOf("code" to 500, "message" to cause.message))
            throw cause
        }
    }

}