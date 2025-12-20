package org.unividuell.news.comunio.lineup

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.unividuell.news.comunio.ComunioConfig
import org.zalando.logbook.Logbook
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor
import java.time.Duration

@Component
class LineupClient(
    comunioConfig: ComunioConfig,
    logbook: Logbook,
) {

    private val logger = KotlinLogging.logger {  }

    private val rateLimiter = RateLimiter
        .of(
            "comunio-api",
            RateLimiterConfig.custom()
                // max 3 requests during 5 seconds
                .limitForPeriod(3)
                .limitRefreshPeriod(Duration.ofSeconds(5))
                // parks thread for up to 10 seconds before throwing a RequestNotPermitted exception
                .timeoutDuration(Duration.ofSeconds(10))
                .build()
        ).also { rateLimiter ->
            rateLimiter.eventPublisher
                .onSuccess { logger.debug { "Rate Limiter: Permit acquired for '${it.rateLimiterName}'" } }
                .onFailure { logger.warn { "Rate Limiter: Request denied/timed out for '${it.rateLimiterName}'! ${it.eventType}" } }
        }

    private val restClient = RestClient.builder()
        .defaultHeader(comunioConfig.stats.userAgent)
        .baseUrl(comunioConfig.stats.baseUrl)
        .requestInterceptor(LogbookClientHttpRequestInterceptor(logbook))

    fun scrape(groupOrderId: Int): List<LineupOutput> {
        return fetchLineUp(groupOrderId = groupOrderId)
            .let { parseLineup(it) }
            .let { selectMatches(it) }
            .let { selectMatchIds(it) }
            .map { fetchMatchId(it) }
            .map { (matchId, matchDetails) ->
                LineupOutput(
                    matchId = matchId,
                    homeClub = matchDetails?.let { matchDetails ->
                        LineupOutput.ComunioClub(
                            cid = matchDetails.homeClubId,
                            lineup = LineupOutput.ComunioClub.ClubLineup(
                                details = LineupOutput.ComunioClub.ClubLineup.LineupDetails.byId(id = matchDetails.details),
                                players = matchDetails.homePlayers.map {
                                    LineupOutput.ComunioClub.ClubLineup.ComunioFootballPlayer(
                                        pid = it.playerId,
                                        name = it.name,
                                        position = LineupOutput.ComunioClub.ClubLineup.ComunioFootballPlayer.Position.byId(
                                            it.pos
                                        ),
                                        goals = it.goals,
                                        penaltyGoals = it.pens,
                                        points = it.points,
                                        active = LineupOutput.ComunioClub.ClubLineup.ComunioFootballPlayer.Active.byId(it.active),
                                    )
                                }
                            )
                        )
                    },
                    awayClub = matchDetails?.let { matchDetails ->
                        LineupOutput.ComunioClub(
                            cid = matchDetails.awayClubId,
                            lineup = LineupOutput.ComunioClub.ClubLineup(
                                details = LineupOutput.ComunioClub.ClubLineup.LineupDetails.byId(matchDetails.details),
                                players = matchDetails.awayPlayers.map {
                                    LineupOutput.ComunioClub.ClubLineup.ComunioFootballPlayer(
                                        pid = it.playerId,
                                        name = it.name,
                                        position = LineupOutput.ComunioClub.ClubLineup.ComunioFootballPlayer.Position.byId(
                                            it.pos
                                        ),
                                        goals = it.goals,
                                        penaltyGoals = it.pens,
                                        points = it.points,
                                        active = LineupOutput.ComunioClub.ClubLineup.ComunioFootballPlayer.Active.byId(it.active),
                                    )
                                }
                            )
                        )
                    },
                    state = LineupOutput.ComunioClub.MatchState.byId(matchDetails?.state)
                )
            }
    }

    private fun fetchLineUp(groupOrderId: Int): String {
        val response = restClient
            .build()
            .get()
            .uri("matchday/2025-26/{matchGroupOrderId}", groupOrderId)
            .accept(MediaType.TEXT_HTML)
            .retrieve()
            .body<String>()
            ?: throw IllegalStateException("Could not fetch lineup!")
        return response
    }

    private fun parseLineup(body: String): Document {
        return Jsoup.parse(body)
    }

    private fun selectMatches(document: Document): Elements {
        return document.select("div#content > div.matches > div.match")
    }

    private fun selectMatchIds(matches: Elements): List<Int> {
        val selector = "matchDetails_"
        return matches.mapNotNull { match ->
            val details = match.selectFirst("div[id^=$selector]")
            details?.id()?.replace(selector, "")?.toInt()
        }
    }

    // comunio lies about the content-type (`text/html` but it is `application/json`)!
    private val htmlJsonConverter = JacksonJsonHttpMessageConverter().apply {
        supportedMediaTypes = listOf(MediaType.APPLICATION_JSON, MediaType.TEXT_HTML)
    }

    private fun fetchMatchId(matchId: Int): Pair<Int, MatchDetails?> {
        return RateLimiter.decorateSupplier(rateLimiter) {
            restClient
                .configureMessageConverters { converters ->
                    converters.addCustomConverter(htmlJsonConverter)
                }
                .build()
                .get()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("xhr/matchDetails.php")
                        .queryParam("mid", matchId)
                        .build()
                }
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body<MatchDetails>()
                ?.let { matchDetails -> matchId to matchDetails } ?: (matchId to null)
        }.get()
    }

    data class LineupOutput(
        val matchId: Int,
        val homeClub: ComunioClub?,
        val awayClub: ComunioClub?,
        val state: ComunioClub.MatchState,
    ) {
        data class ComunioClub(
            val cid: Int,
            val lineup: ClubLineup,
        ) {
            data class ClubLineup(
                val details: LineupDetails,
                val players: List<ComunioFootballPlayer>,
            ) {
                enum class LineupDetails(val id: Int) {
                    Expected(1),
                    Final(2),
                    Unknow(-1);
                    companion object {
                        fun byId(id: Int): LineupDetails {
                            return entries.firstOrNull { it.id == id } ?: Unknow
                        }
                    }
                }
                data class ComunioFootballPlayer(
                    val pid: Long,
                    val name: String,
                    val position: Position,
                    val goals: Int,
                    val penaltyGoals: Int,
                    val points: Int?,
                    val active: Active,
                ) {
                    enum class Position(val id: String) {
                        // "t", "a", "m", "s"
                        Goalkeeper("t"),
                        Defender("a"),
                        Midfielder("m"),
                        Forward("s");
                        companion object {
                            fun byId(id: String?): Position {
                                return entries.first { it.id == id }
                            }
                        }
                    }
                    enum class Active(val id: Int) {
                        ProperlyActive(-1),
                        Active(1),
                        Bench(-2),
                        Unknow(-1000);
                        companion object {
                            fun byId(id: Int): Active = entries.firstOrNull { it.id == id } ?: Unknow
                        }
                    }
                }
            }
            enum class MatchState(val id: String) {
                FirstHalf("1st"),
                HalfTime("HT"),
                SecondHalf("2nd"),
                FullTime("FT"),
                Unknow("");
                companion object {
                    fun byId(id: String?): MatchState {
                        return entries.firstOrNull { it.id == id } ?: Unknow
                    }
                }
            }
        }
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
        val playerId: Long,
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