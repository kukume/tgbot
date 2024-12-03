package me.kuku.telegram.utils

object RegexUtils {

    fun extract(text: String, start: String, end: String): String? {
        val pattern = "$start(.*?)$end".toRegex()
        return pattern.find(text)?.groupValues?.get(1)
    }

}