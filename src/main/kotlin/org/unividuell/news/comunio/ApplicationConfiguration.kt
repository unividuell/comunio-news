package org.unividuell.news.comunio

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@ConfigurationProperties(prefix = "comunio")
data class ComunioConfig(
    val cid: Number,
    val stats: ComunioStatsConfig,
) {
    data class ComunioStatsConfig(
        val baseUrl: String,
        val userAgent: String,
        val credentials: ComunioStatsCredentials,
    ) {
        data class ComunioStatsCredentials(
            val username: String,
            val password: String,
        )
    }
}