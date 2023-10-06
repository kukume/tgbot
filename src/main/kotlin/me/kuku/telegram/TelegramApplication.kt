package me.kuku.telegram

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing

@SpringBootApplication
@EnableReactiveMongoAuditing
class TelegramApplication

fun main(args: Array<String>) {
    runApplication<TelegramApplication>(*args)
}
