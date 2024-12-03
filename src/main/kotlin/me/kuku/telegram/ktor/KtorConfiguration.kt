package me.kuku.telegram.ktor

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import me.kuku.telegram.ktor.context.KtorModule
import me.kuku.telegram.ktor.context.KtorRouter
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(KtorProperties::class)
class KtorConfiguration(
    private val properties: KtorProperties
) {


    @Bean
    fun applicationEngine(context: ApplicationContext): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
        return embeddedServer(Netty, host = properties.host, port = properties.port) {
            val modules = context.getBeansOfType(KtorModule::class.java).values
            val routes = context.getBeansOfType(KtorRouter::class.java).values

            //注册模块
            modules.forEach {
                it.apply { register() }
            }

            //注册路由
            routing {
                routes.forEach {
                    it.apply { route() }
                }
            }
        }.start()
    }


}

@ConfigurationProperties(prefix = "spring.ktor")
open class KtorProperties(
    open var host: String = "0.0.0.0",
    open var port: Int = 8080
)