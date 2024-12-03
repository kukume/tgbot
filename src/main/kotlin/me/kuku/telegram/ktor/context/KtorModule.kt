package me.kuku.telegram.ktor.context

import io.ktor.server.application.*

interface KtorModule {

    fun Application.register()

}