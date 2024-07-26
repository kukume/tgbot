package me.kuku.telegram

import com.mongodb.kotlin.client.coroutine.MongoClient
import io.ktor.server.cio.*
import io.ktor.server.config.yaml.*
import me.kuku.telegram.config.CronConfiguration
import me.kuku.telegram.config.PidConfiguration
import me.kuku.telegram.config.TelegramConfiguration

fun main(args: Array<String>) {
    PidConfiguration.init()
    TelegramConfiguration.init()
    CronConfiguration.init()
    EngineMain.main(args)
}

val yamlConfig = YamlConfig(null)!!
private val mongoConfig = yamlConfig.config("ktor.mongo")

private val mongoClient = MongoClient.create(mongoConfig.property("uri").getString())

val mongoDatabase = mongoClient.getDatabase(mongoConfig.property("database").getString())