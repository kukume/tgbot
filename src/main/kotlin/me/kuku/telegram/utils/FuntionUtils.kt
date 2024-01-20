package me.kuku.telegram.utils

import com.fasterxml.jackson.databind.JsonNode
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.kuku.utils.DateTimeFormatterUtils
import me.kuku.utils.client
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.time.LocalDateTime

suspend fun ffmpeg(command: String) {
    val runtime = Runtime.getRuntime()
    val process = withContext(Dispatchers.IO) {
        runtime.exec("${if (System.getProperty("os.name").contains("Windows")) "cmd /C " else ""}$command".split(" ").toTypedArray())
    }
    Thread.startVirtualThread {
        BufferedReader(InputStreamReader(process.inputStream)).use { br ->
            while (true) {
                br.readLine() ?: break
            }
        }
    }
    Thread.startVirtualThread {
        BufferedReader(InputStreamReader(process.errorStream)).use { br ->
            while (true) {
                br.readLine() ?: break
            }
        }
    }
    withContext(Dispatchers.IO) {
        process.waitFor()
    }
}

fun qrcode(text: String): ByteArray {
    val side = 200
    val hints = mapOf(EncodeHintType.CHARACTER_SET to "UTF8", EncodeHintType.MARGIN to 0)
    val bitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, side, side, hints)
    val bos = ByteArrayOutputStream()
    MatrixToImageWriter.writeToStream(bitMatrix, "JPEG", bos)
    return bos.use {
        it.toByteArray()
    }
}

suspend fun githubCommit(): List<GithubCommit> {
    val jsonNode = client.get("https://api.github.com/repos/kukume/tgbot/commits").body<JsonNode>()
    val list = mutableListOf<GithubCommit>()
    for (node in jsonNode) {
        val commit = node["commit"]
        val message = commit["message"].asText()
        val dateStr = commit["committer"]["date"].asText()
            .replace("T", " ").replace("Z", "")
        val zero = DateTimeFormatterUtils.parseToLocalDateTime(dateStr, "yyyy-MM-dd HH:mm:ss")
        val right = zero.plusHours(8)
        val date = DateTimeFormatterUtils.format(right, "yyyy-MM-dd HH:mm:ss")
        list.add(GithubCommit(date, message, right))
    }
    return list
}

class GithubCommit(val date: String, val message: String, val localDateTime: LocalDateTime)