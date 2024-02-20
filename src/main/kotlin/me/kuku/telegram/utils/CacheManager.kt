package me.kuku.telegram.utils

import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit

@Suppress("UNCHECKED_CAST")
object CacheManager {

    private val folder = File("cache")

    val cacheMap: ConcurrentMap<String, Cache<*, *>> = ConcurrentHashMap()

    private val logger = LoggerFactory.getLogger(CacheManager::class.java)

    init {
        if (!folder.exists()) folder.mkdir()
        folder.listFiles()?.let { files ->
            for (file in files) {
                val cache = FileInputStream(file).use {
                    ObjectInputStream(it).use { ois ->
                        try {
                            ois.readObject() as Cache<*, *>
                        } catch (e: Exception) {
                            logger.warn("读取缓存文件${file.path}失败")
                            file.deleteOnExit()
                            null
                        }
                    }
                } ?: continue
                cacheMap[file.nameWithoutExtension] = cache
            }
        }
        Thread.startVirtualThread {
            while (true) {
                Thread.sleep(1000)
                cacheMap.forEach { (_, v) ->
                    v.check()
                }
            }
        }
        Runtime.getRuntime().addShutdownHook(Thread {
            write()
        })
    }

    inline fun <reified K, V> getCache(key: String): Cache<K, V> {
        return cacheMap[key] as? Cache<K, V> ?: Cache<K, V>().also {
            cacheMap[key] = it
        }
    }

    inline fun <reified K, V> getCache(key: String, duration: Duration): Cache<K, V> {
        return cacheMap[key] as? Cache<K, V> ?: Cache<K, V>().also {
            it.expire = duration.toMillis()
            cacheMap[key] = it
        }
    }

    inline fun <reified K, V> getCache(key: String, expire: Long): Cache<K, V> {
        return cacheMap[key] as? Cache<K, V> ?: Cache<K, V>().also {
            cacheMap[key] = it
            it.expire = expire
        }
    }

    inline fun <reified K, V> getCache(key: String, expire: Long, timeUnit: TimeUnit): Cache<K, V> {
        return cacheMap[key] as? Cache<K, V> ?: Cache<K, V>().also {
            it.expire = timeUnit.toMillis(expire)
            cacheMap[key] = it
        }
    }

    @Synchronized
    fun write() {
        cacheMap.forEach { (k, v) ->
            FileOutputStream(File(folder, "$k.ser")).use {
                ObjectOutputStream(it).use { oos ->
                    oos.writeObject(v)
                }
            }
        }
    }



}

@Suppress("ConstPropertyName")
open class Cache<K, V>: Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }

    protected open val map = ConcurrentHashMap<K, Body<V>>()
    var expire: Long? = null

    class Body<V>: Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }

        var value: V? = null
        var time: Long = System.currentTimeMillis()
        var expire: Long = -1

        fun expire() = expire > 0 && System.currentTimeMillis() > time + expire
    }

    operator fun get(key: K): V? {
        renew(key)
        return map[key]?.value
    }

    private fun renew(key: K) {
        val body = map[key]
        if (body != null) {
            body.time = System.currentTimeMillis()
        }
    }

    fun put(key: K, value: V) {
        if (expire != null && expire!! > 0) {
            put(key, value, expire!!)
        } else {
            renew(key)
            map[key] = Body<V>().also {
                it.value = value
            }
        }
    }

    fun put(key: K, value: V, expire: Long, timeUnit: TimeUnit) {
        put(key, value, timeUnit.toMillis(expire))
    }

    fun put(key: K, value: V, expire: Long) {
        renew(key)
        map[key] = Body<V>().also {
            it.expire = expire
            it.value = value
        }
    }

    fun put(key: K, value: V, duration: Duration) {
        put(key, value, duration.toMillis())
    }


    fun remove(key: K) {
        map.remove(key)
    }

    fun clear() {
        map.clear()
    }

    fun containsKey(key: K): Boolean {
        return map.containsKey(key)
    }

    fun check() {
        val expireKey = mutableListOf<K>()
        map.forEach { (k, v) ->
            if (v.expire()) {
                expireKey.add(k)
            }
        }
        expireKey.forEach { map.remove(it) }
    }
}