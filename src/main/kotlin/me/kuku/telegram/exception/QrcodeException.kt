package me.kuku.telegram.exception

open class QrcodeException(message: String? = null): RuntimeException(message)

open class QrcodeScanException(message: String? = null): QrcodeException(message)

class QrcodeNotScannedException(message: String? = null): QrcodeScanException(message)

fun qrcodeNotScanned(message: String? = null): Nothing = throw QrcodeNotScannedException(message)

class QrcodeScannedException(message: String? = null): QrcodeScanException(message)

fun qrcodeScanned(message: String? = null): Nothing = throw QrcodeScannedException(message)

class QrcodeExpireException(message: String? = null): QrcodeException(message)

fun qrcodeExpire(message: String = "二维码已过期"): Nothing = throw QrcodeExpireException(message)