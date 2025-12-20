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

@Component
class MatchLineupClient(
    private val restClient: RestClient.Builder,
    private val htmlJsonConverter: JacksonJsonHttpMessageConverter,
) {

    private val logger = KotlinLogging.logger {  }

    /**
     * scraps:
     * 0. GET https://stats.comunio.de/matchday/2025-26/15
     * 1. GET https://stats.comunio.de/xhr/matchDetails.php?mid=7663
     * .
     * 8. GET https://stats.comunio.de/xhr/matchDetails.php?mid=7671
     */
    fun scrape(groupOrderId: Int): MatchLineupOutput {
        logger.info { "Scraping lineup for groupOrderId $groupOrderId" }
        val fetch = fetchLineUp(groupOrderId = groupOrderId)
        val parsed = parseLineup(body = fetch)
        val comunioGamedayId = selectComunioGamedayId(document = parsed)
        val matchElements = selectMatches(document = parsed)
        val matchIds = selectMatchIds(matches = matchElements)
        val matches = matchIds.map { fetchMatchId(matchId = it) }
        val lineups = matches.map { (matchId, matchDetails) ->
            MatchLineupOutput.LineupOutput(
                matchId = matchId,
                homeClub = matchDetails?.let { matchDetails ->
                    MatchLineupOutput.LineupOutput.ComunioClub(
                        cid = matchDetails.homeClubId,
                        lineup = MatchLineupOutput.LineupOutput.ComunioClub.ClubLineup(
                            details = MatchLineupOutput.LineupOutput.ComunioClub.ClubLineup.LineupDetails.byId(id = matchDetails.details),
                            players = matchDetails.homePlayers.map {
                                MatchLineupOutput.LineupOutput.ComunioClub.ClubLineup.ComunioFootballPlayer(
                                    pid = it.playerId,
                                    name = it.name,
                                    position = MatchLineupOutput.LineupOutput.ComunioClub.ClubLineup.ComunioFootballPlayer.Position.byId(
                                        it.pos
                                    ),
                                    goals = it.goals,
                                    penaltyGoals = it.pens,
                                    points = it.points,
                                    active = MatchLineupOutput.LineupOutput.ComunioClub.ClubLineup.ComunioFootballPlayer.Active.byId(
                                        it.active
                                    ),
                                    substitutedInAtMin = it.subIn,
                                    substitutedOutAtMin = it.subOut,
                                )
                            }
                        )
                    )
                },
                awayClub = matchDetails?.let { matchDetails ->
                    MatchLineupOutput.LineupOutput.ComunioClub(
                        cid = matchDetails.awayClubId,
                        lineup = MatchLineupOutput.LineupOutput.ComunioClub.ClubLineup(
                            details = MatchLineupOutput.LineupOutput.ComunioClub.ClubLineup.LineupDetails.byId(matchDetails.details),
                            players = matchDetails.awayPlayers.map {
                                MatchLineupOutput.LineupOutput.ComunioClub.ClubLineup.ComunioFootballPlayer(
                                    pid = it.playerId,
                                    name = it.name,
                                    position = MatchLineupOutput.LineupOutput.ComunioClub.ClubLineup.ComunioFootballPlayer.Position.byId(
                                        it.pos
                                    ),
                                    goals = it.goals,
                                    penaltyGoals = it.pens,
                                    points = it.points,
                                    active = MatchLineupOutput.LineupOutput.ComunioClub.ClubLineup.ComunioFootballPlayer.Active.byId(
                                        it.active
                                    ),
                                    substitutedInAtMin = it.subIn,
                                    substitutedOutAtMin = it.subOut,
                                )
                            }
                        )
                    )
                },
                state = MatchLineupOutput.LineupOutput.ComunioClub.MatchState.byId(matchDetails?.state),
            )
        }
        logger.info { "Scraped lineup for groupOrderId $groupOrderId ($comunioGamedayId)" }
        return MatchLineupOutput(comunioGamedayId = comunioGamedayId, matches = lineups)
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

    private fun selectComunioGamedayId(document: Document): Long {
        return document.selectFirst("div#comMatchday")
            ?.text()
            ?.toLongOrNull()
            ?: throw IllegalStateException("Could not fetch comunioGamedayId!")
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

    private fun fetchMatchId(matchId: Int): Pair<Int, MatchDetails?> {
        return restClient
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
    }

    data class MatchLineupOutput(
        val comunioGamedayId: Long,
        val matches: List<LineupOutput>,
    ) {
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
                        Playing(2),
                        Over(3),
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
                        val substitutedInAtMin: Int?,
                        val substitutedOutAtMin: Int?,
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