package me.kuku.telegram

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableReactiveMongoAuditing
@EnableScheduling
class TelegramApplication

fun main(args: Array<String>) {
    runApplication<TelegramApplication>(*args)
}
