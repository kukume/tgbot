package me.kuku.telegram.config

import java.io.File

object PidConfiguration {

    fun init() {
        val pid = ProcessHandle.current().pid()
        File("application.pid").writeText(pid.toString())
    }

}