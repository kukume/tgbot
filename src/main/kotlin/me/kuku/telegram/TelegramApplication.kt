package me.kuku.telegram

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.config.EnableMongoAuditing

@SpringBootApplication
@EnableMongoAuditing
class TelegramApplication

fun main(args: Array<String>) {
    runApplication<TelegramApplication>(*args)
}
