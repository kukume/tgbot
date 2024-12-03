package me.kuku.telegram.utils

import java.net.URLDecoder
import java.net.URLEncoder
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher

fun String.md5(): String {
    val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

fun String.toUrlDecode(): String {
    return URLDecoder.decode(this, "UTF-8")
}

fun String.toUrlEncode(): String {
    return URLEncoder.encode(this, "UTF-8")
}

fun String.rsaEncrypt(publicKeyStr: String): String {
    val keyFactory = KeyFactory.getInstance("RSA")
    val decodedKey = Base64.getDecoder().decode(publicKeyStr.toByteArray())
    val keySpec = X509EncodedKeySpec(decodedKey)
    val publicKey =  keyFactory.generatePublic(keySpec)
    val cipher = Cipher.getInstance("RSA")
    cipher.init(Cipher.ENCRYPT_MODE, publicKey)
    val encryptedBytes = cipher.doFinal(this.toByteArray(Charsets.UTF_8))
    return Base64.getEncoder().encodeToString(encryptedBytes)
}

fun ByteArray.hex(): String {
    return this.joinToString("") { "%02x".format(it) }
}