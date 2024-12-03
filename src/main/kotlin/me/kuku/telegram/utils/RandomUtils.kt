package me.kuku.telegram.utils

import kotlin.random.Random

object RandomUtils {

    fun num(num: Int): String {
        return (1..num)
            .map { Random.nextInt(0, 10) } // 生成单个数字
            .joinToString("")
    }

    fun letter(length: Int): String {
        val chars = ('a'..'z') + ('A'..'Z') // 包括大小写字母
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }

}