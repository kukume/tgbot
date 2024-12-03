package me.kuku.telegram.ktor.context

import io.ktor.server.routing.*

interface KtorRouter {

    fun Routing.route()

}