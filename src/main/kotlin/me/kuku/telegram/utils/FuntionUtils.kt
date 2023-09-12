package me.kuku.telegram.utils

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import kotlin.concurrent.thread

suspend fun ffmpeg(command: String) {
    val runtime = Runtime.getRuntime()
    val process = withContext(Dispatchers.IO) {
        runtime.exec("${if (System.getProperty("os.name").contains("Windows")) "cmd /C " else ""}$command")
    }
    thread(true) {
        BufferedReader(InputStreamReader(process.inputStream)).use { br ->
            while (true) {
                br.readLine() ?: break
            }
        }
    }
    thread(true) {
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