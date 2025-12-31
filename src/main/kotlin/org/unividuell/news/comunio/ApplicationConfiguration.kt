package org.unividuell.news.comunio

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "comunio")
data class ComunioConfig(
    val cid: Number,
    val season: Number,
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

@ConfigurationProperties(prefix = "openligadb")
data class OpenligaDbConfig(
    val season: Int,
)

@ConfigurationProperties(prefix = "app")
data class AppConfig(
    val season: Season,
    val clubIdMapping: List<ClubIdMapping>
) {
    data class Season(
        val start: Int,
        val end: Int,
    )
    data class ClubIdMapping(
        val name: String,
        val olid: Int,
        val cid : Int,
    )
}