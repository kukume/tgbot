package me.kuku.telegram.logic

import kotlinx.coroutines.delay
import me.kuku.telegram.entity.NetEaseSmallService

object NetEaseSmallLogic {

    suspend fun check() {
        val list = NetEaseSmallService.findAll()
        for (netEaseSmallEntity in list) {
            if (netEaseSmallEntity.csrf.isEmpty()) {
                val loginResult = NetEaseLogic.login(netEaseSmallEntity.username, netEaseSmallEntity.password)
                delay(1000)
                if (loginResult.success()) {
                    val newEntity = loginResult.data()
                    netEaseSmallEntity.csrf = newEntity.csrf
                    netEaseSmallEntity.musicU = newEntity.musicU
                    NetEaseSmallService.save(netEaseSmallEntity)
                }
            }
        }
    }

    suspend fun listenMusic(id: Int) {
        val list = NetEaseSmallService.findAll()
        for (netEaseSmallEntity in list) {
            if (netEaseSmallEntity.csrf.isNotEmpty()) {
                NetEaseLogic.listenMusic(netEaseSmallEntity, id)
            }
        }
    }


}