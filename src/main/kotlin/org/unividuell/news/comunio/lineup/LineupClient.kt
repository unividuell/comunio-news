package org.unividuell.news.comunio.lineup

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.unividuell.news.comunio.openligadb.OpenLigaDb

@Component
class LineupClient {

    private val logger = KotlinLogging.logger {  }

    private val restClient = RestClient.builder()
        .defaultHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:147.0) Gecko/20100101 Firefox/147.0")
        .baseUrl("https://stats.comunio.de")
        .requestInterceptor { request, bytes, execution ->
            val response = execution.execute(request, bytes)
            logger.debug {
                """|
                    |>>> ${request.method} ${request.uri}
                    |<<< ${response.statusCode}
                """.trimMargin()
            }
            return@requestInterceptor response
        }

    fun scrape(matchGroup: OpenLigaDb.MatchGroup): List<MatchDetails?>? {
        return fetchLineUp(matchGroup)
            ?.let { parseLineup(it) }
            ?.let { selectMatches(it) }
            ?.let { selectMatchIds(it) }
            ?.map { fetchMatchId(it) }
    }

    private fun fetchLineUp(matchGroup: OpenLigaDb.MatchGroup): String? {
        val response = restClient
            .build()
            .get()
            .uri("matchday/2025-26/{matchGroupOrderId}", matchGroup.groupOrderId)
            .accept(MediaType.TEXT_HTML)
            .retrieve()
            .body<String>()
        return response
    }

    private fun parseLineup(body: String): Document {
        return Jsoup.parse(body)
    }

    private fun selectMatches(document: Document): Elements? {
        return document.select("div#content > div.matches > div.match")
    }

    private fun selectMatchIds(matches: Elements): List<Int> {
        return matches.mapNotNull { match ->
            val details = match.selectFirst("div[id^=matchDetails_]")
            details?.id()?.replace("matchDetails_", "")?.toInt()
        }
    }

    // comunio lies about the content-type (`text/html` but it is `application/json`)!
    private val converter = JacksonJsonHttpMessageConverter().apply {
        supportedMediaTypes = listOf(MediaType.APPLICATION_JSON, MediaType.TEXT_HTML)
    }


    private fun fetchMatchId(matchId: Int): MatchDetails? {
        return restClient
            .configureMessageConverters { converters ->
                converters.addCustomConverter(converter)
            }
            .build()
            .get()
            .uri { uriBuilder -> uriBuilder
                .path("xhr/matchDetails.php")
                .queryParam("mid", matchId)
                .build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body<MatchDetails>()
    }

    data class MatchDetails(
        val matchId: Int,
        val details: Int,
        val homeClubId: Int,
        val awayClubId: Int,
        val state: String?,
        val goals: List<Any> = emptyList(),
        val homePlayers: List<Player> = emptyList(),
        val awayPlayers: List<Player> = emptyList(),
        val homeStats: TeamStats?,
        val awayStats: TeamStats?,
        val homeTeamStats: Any? = null,
        val awayTeamStats: Any? = null,
        val disOpts: DisplayOptions?
    )

    data class Player(
        val playerId: Int,
        val name: String,
        val pos: String, // z.B. "t", "a", "m", "s"
        val goals: Int,
        val ownGoals: Int,
        val pens: Int,
        val pensSaved: Int,
        val pensMissed: Int,
        val assists: Int,
        val yellow: Int,
        val yellowRed: Int,
        val red: Int,
        val points: Int?,
        val rating: String?, // Double
        val subIn: Int?,
        val subOut: Int?,
        val motm: Boolean?,
        val cleanSheet: Boolean?,
        val active: Int,
        val trending: Int,
        val rising: Int,
        val xgoals: String?, // Double
        val stats: DetailedStats?,
        val ratingLevel: Int?
    )

    data class DetailedStats(
        val shots: Int?,
        val shotsOnGoal: Int?,
        val foulsDrawn: Int?,
        val foulsCommitted: Int?,
        val passingRate: Int?,
        val duelRate: Int?,
        val minutesPlayed: Int?
    )

    data class TeamStats(
        val shots: Int?,
        val shotsOnGoal: Int?,
        val foulsDrawn: Int?,
        val foulsCommitted: Int?,
        val passingRate: Int?,
        val duelRate: Int?
    )

    data class DisplayOptions(
        @JsonProperty("showXG") val showXG: Boolean,
        val showAssists: Boolean,
        val showPensSaved: Boolean,
        val showCleanSheet: Boolean,
        val showOwnGoals: Boolean,
        val showPensMissed: Boolean
    )

}