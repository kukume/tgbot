package me.kuku.telegram.logic

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import me.kuku.telegram.entity.NetEaseSmallService
import me.kuku.telegram.entity.Status
import me.kuku.utils.client
import org.springframework.stereotype.Service

@Service
class NetEaseSmallLogic(
    private val netEaseSmallService: NetEaseSmallService
) {

    suspend fun check() {
        val list = netEaseSmallService.findAll()
        for (netEaseSmallEntity in list) {
            if (netEaseSmallEntity.status == Status.ON && !netEaseSmallEntity.oneDayAgo()) continue
            val loginResult = NetEaseLogic.login(netEaseSmallEntity.username, netEaseSmallEntity.password)
            delay(2000)
            if (loginResult.success()) {
                val newEntity = loginResult.data()
                netEaseSmallEntity.csrf = newEntity.csrf
                netEaseSmallEntity.musicU = newEntity.musicU
                netEaseSmallEntity.reason = ""
                netEaseSmallEntity.status = Status.ON
                netEaseSmallService.save(netEaseSmallEntity)
            } else {
                kotlin.runCatching {
                    val jsonNode = client.get("http://192.168.1.177:8000/neteasemusic/login?phone=${netEaseSmallEntity.username}&password=${netEaseSmallEntity.password}")
                        .body<JsonNode>()
                    val musicU = jsonNode["musicU"].asText()
                    val csrf = jsonNode["csrf"].asText()
                    netEaseSmallEntity.csrf = csrf
                    netEaseSmallEntity.musicU = musicU
                    netEaseSmallEntity.status = Status.ON
                    netEaseSmallEntity.reason = ""
                    netEaseSmallService.save(netEaseSmallEntity)
                }.onFailure {
                    netEaseSmallEntity.status = Status.OFF
                    netEaseSmallEntity.reason = loginResult.message
                    netEaseSmallService.save(netEaseSmallEntity)
                }

            }
        }
    }

    suspend fun listenMusic(id: Long) {
        val list = netEaseSmallService.findByStatus(Status.ON)
        for (netEaseSmallEntity in list) {
            if (netEaseSmallEntity.csrf.isNotEmpty()) {
                NetEaseLogic.listenMusic0(netEaseSmallEntity, id)
            }
        }
    }


}