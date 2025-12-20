package org.unividuell.news.comunio.lineup

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.unividuell.news.comunio.ComunioConfig

@Component
class MemberLineupClient(
    private val restClient: RestClient.Builder,
    private val comunioConfig: ComunioConfig,
    private val htmlJsonConverter: JacksonJsonHttpMessageConverter,
) {

    private val logger = KotlinLogging.logger {  }

    /**
     * scraps:
     * 1. GET https://stats.comunio.de/xhr/lineup.php?cid=13742756&s=2026&gid=395809&com=1
     */
    fun scrape(comunioGamedayId: Long): MemberLineupOutput {
        logger.info { "Scraping member lineup for comunioGamedayId $comunioGamedayId" }
        val response = fetch(comunioGamedayId = comunioGamedayId)
        val members = response.members.map { (memberId, memberName) ->
            MemberLineupOutput.ComunioMember(
                memberId = memberId,
                name = memberName,
                lineup = response.lineups.values
                    .filter { it.ownerId.toLong() == memberId }
                    .map { lineup ->
                        MemberLineupOutput.ComunioMember.MemberPlayer(
                            playerId = lineup.pid.toLong(),
                            clubId = lineup.clubId.toLong(),
                            position = lineup.pos,
                            active = lineup.active == "1"
                        )
                    }
            )
        }
        logger.info { "Scraped member lineup for comunioGamedayId $comunioGamedayId" }
        return MemberLineupOutput(comunioGamedayId = response.gamedayId, members = members)
    }

    private fun fetch(comunioGamedayId: Long): GameResponse {
        return restClient
            .configureMessageConverters { converters ->
                converters.addCustomConverter(htmlJsonConverter)
            }
            .build()
            .get()
            .uri { uriBuilder -> uriBuilder
                .path("/xhr/lineup.php")
                .queryParam("cid", comunioConfig.cid)
                .queryParam("s", comunioConfig.season)
                .queryParam("gid", comunioGamedayId)
                .queryParam("com", 1)
                .build()
            }
            .retrieve()
            .body<GameResponse>()
            ?: throw IllegalStateException("Could not fetch lineup!")
    }

    data class MemberLineupOutput(
        val comunioGamedayId: Long,
        val members: List<ComunioMember>,
    ) {
        data class ComunioMember(
            val memberId: Long,
            val name: String,
            val lineup: List<MemberPlayer>,
        ) {
            data class MemberPlayer(
                val playerId: Long,
                val clubId: Long,
                val position: String,
                val active: Boolean,
            )
        }
    }

    data class GameResponse(
        val season: Long,
        val gamedayId: Long,
        /**
         * Football-Player-ID to LineupDetails
         */
        val lineups: Map<Long, LineupDetails>,
        /**
         * Member-ID to Member-Name
         */
        val members: Map<Long, String>
    )

    data class LineupDetails(
        val pid: String,
        val ownerId: String,
        val active: String, // Im JSON als String "1", "-1" etc. geliefert
        val pos: String,
        val clubId: String
    )

}