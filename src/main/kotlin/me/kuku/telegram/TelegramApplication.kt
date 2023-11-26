package me.kuku.telegram

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.ApplicationPidFileWriter
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableReactiveMongoAuditing
@EnableScheduling
class TelegramApplication

fun main(args: Array<String>) {
    SpringApplication(TelegramApplication::class.java)
        .also { it.addListeners(ApplicationPidFileWriter()) }.run(*args)
}
