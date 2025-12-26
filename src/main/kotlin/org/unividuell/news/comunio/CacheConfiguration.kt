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
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import tools.jackson.databind.DefaultTyping
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator

@Configuration
@EnableCaching
class CacheConfiguration {

    private val logger = KotlinLogging.logger {  }

    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): RedisCacheManager {
        val json = redisJsonMapper()

        val config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(java.time.Duration.ofMinutes(30))
            .disableCachingNullValues()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(GenericJacksonJsonRedisSerializer(json))
            )

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build()
    }

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory

        val json = redisJsonMapper()
        val serializer = GenericJacksonJsonRedisSerializer(json)

        // Keys as Strings, Values as JSON
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = serializer
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = serializer

        template.afterPropertiesSet()
        return template
    }

    @Bean
    fun redisJsonMapper(): JsonMapper {
        return JsonMapper.builder()
            .activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Any::class.java)
                .build(),
                DefaultTyping.NON_FINAL
            )
            .build()
    }

    @Autowired
    lateinit var dataRedisConnectionDetails: DataRedisConnectionDetails

    @PostConstruct
    fun logRedisPort() {
        logger.info { "Redis standalone port: ${dataRedisConnectionDetails.standalone?.port}" }
    }
}