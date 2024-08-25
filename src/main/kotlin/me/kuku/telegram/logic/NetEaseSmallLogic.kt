package me.kuku.telegram.logic

import kotlinx.coroutines.delay
import me.kuku.telegram.entity.NetEaseSmallService
import org.springframework.stereotype.Service

@Service
class NetEaseSmallLogic(
    private val netEaseSmallService: NetEaseSmallService
) {

    suspend fun check() {
        val list = netEaseSmallService.findAll()
        for (netEaseSmallEntity in list) {
            if (netEaseSmallEntity.csrf.isEmpty()) {
                val loginResult = NetEaseLogic.login(netEaseSmallEntity.username, netEaseSmallEntity.password)
                delay(1000)
                if (loginResult.success()) {
                    val newEntity = loginResult.data()
                    netEaseSmallEntity.csrf = newEntity.csrf
                    netEaseSmallEntity.musicU = newEntity.musicU
                    netEaseSmallService.save(netEaseSmallEntity)
                } else {
                    netEaseSmallService.delete(netEaseSmallEntity)
                }
            }
        }
    }

    suspend fun listenMusic(id: Int) {
        val list = netEaseSmallService.findAll()
        for (netEaseSmallEntity in list) {
            if (netEaseSmallEntity.csrf.isNotEmpty()) {
                NetEaseLogic.listenMusic(netEaseSmallEntity, id)
                delay(2000)
            }
        }
    }


}