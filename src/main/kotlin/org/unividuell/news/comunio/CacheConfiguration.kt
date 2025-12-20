package org.unividuell.news.comunio

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import tools.jackson.databind.ObjectMapper

@Configuration
@EnableCaching
class CacheConfiguration {

    private val logger = KotlinLogging.logger {  }

    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): RedisCacheManager {
        val json = ObjectMapper()
        val config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(java.time.Duration.ofMinutes(30))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(GenericJacksonJsonRedisSerializer(json))
            )
        return RedisCacheManager.builder(connectionFactory).cacheDefaults(config).build()
    }

    @Autowired
    lateinit var dataRedisConnectionDetails: DataRedisConnectionDetails

    @PostConstruct
    fun logRedisPort() {
        logger.info { "Redis standalone port: ${dataRedisConnectionDetails.standalone?.port}" }
    }
}