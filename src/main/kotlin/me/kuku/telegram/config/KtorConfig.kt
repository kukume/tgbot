package me.kuku.telegram.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import me.kuku.pojo.CommonResult
import me.kuku.utils.Jackson
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class KtorConfig {

    fun Application.statusPages() {

        install(StatusPages) {

            exception<MissingRequestParameterException> { call, cause ->
                call.respond(
                    HttpStatusCode.BadRequest,
                    CommonResult.failure(code = 400, message = cause.message ?: "参数丢失", data = null)
                )
            }

            exception<Throwable> { call, cause ->
                call.respond(HttpStatusCode.InternalServerError, CommonResult.failure<Unit>(cause.toString()))
                throw cause
            }
        }

    }

    @Bean
    fun objectMapper(): ObjectMapper {
        return Jackson.objectMapper
    }

}
