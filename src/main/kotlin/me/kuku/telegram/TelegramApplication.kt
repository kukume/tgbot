package me.kuku.telegram

import me.kuku.utils.OkHttpUtils
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.net.InetSocketAddress
import java.net.Proxy

@SpringBootApplication
class TelegramApplication

fun main(args: Array<String>) {
    OkHttpUtils.proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", 7890))
    runApplication<TelegramApplication>(*args)
}