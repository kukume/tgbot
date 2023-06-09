package me.kuku.telegram.config

import org.ehcache.CacheManager
import org.ehcache.config.builders.CacheManagerBuilder
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class EhcacheConfig {

    @Bean
    fun cacheManager(): CacheManager {
        return CacheManagerBuilder.newCacheManagerBuilder()
            .build(true)
    }

}
