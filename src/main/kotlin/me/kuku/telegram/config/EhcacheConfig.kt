package me.kuku.telegram.config

import me.kuku.telegram.entity.OciEntity
import me.kuku.telegram.extension.OciCache
import org.ehcache.CacheManager
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.CacheManagerBuilder
import org.ehcache.config.builders.ExpiryPolicyBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class EhcacheConfig(
    private val applicationContext: ApplicationContext
) {

    @Bean
    fun cacheManager(): CacheManager {
        return CacheManagerBuilder.newCacheManagerBuilder()
            .withCache("bindOci",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(Long::class.javaObjectType, OciEntity::class.java,
                    ResourcePoolsBuilder.heap(100)).withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofMinutes(2))))
            .withCache("selectOci",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(Long::class.javaObjectType, OciCache::class.java,
                    ResourcePoolsBuilder.heap(100)).withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofMinutes(2))))
            .withCache("chooseOci",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(String::class.java, String::class.java,
                    ResourcePoolsBuilder.heap(100)).withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofMinutes(2))))
            .withCache("callbackHistory",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(String::class.java, String::class.java,
                    ResourcePoolsBuilder.heap(2)).withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofMinutes(2))))
            .build(true)
    }

}
