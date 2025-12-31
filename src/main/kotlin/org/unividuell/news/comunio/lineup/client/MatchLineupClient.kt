package org.unividuell.news.comunio.lineup.client

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.unividuell.news.comunio.lineup.MatchLineupOutput

@Component
class MatchLineupClient(
    restClientBuilder: RestClient.Builder,
    private val htmlJsonConverter: JacksonJsonHttpMessageConverter,
) {

    private val logger = KotlinLogging.logger {  }

    private val defaultClient = restClientBuilder.build()

    /**
     * scraps:
     *
     * 1. GET https://stats.comunio.de/xhr/matchDetails.php?mid=7663
     */
    fun scrapeMatchLineup(matchId: Int): MatchLineupOutput.LineupOutput {
        val (matchId, matchDetails) = fetchMatch(matchId = matchId)
        val lineup =
            MatchLineupOutput.LineupOutput(
                matchId = matchId,
                homeClub = matchDetails?.let { matchDetails ->
                    MatchLineupOutput.LineupOutput.ComunioClub(
                        cid = matchDetails.homeClubId,
                        lineup = MatchLineupOutput.LineupOutput.ComunioClub.ClubLineup(
                            details = MatchLineupOutput.LineupOutput.ComunioClub.ClubLineup.LineupDetails.byId(id = matchDetails.details),
                            players = matchDetails.homePlayers.map {
                                mapPlayer(player = it)
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
                                mapPlayer(player = it)
                            }
                        )
                    )
                },
                state = MatchLineupOutput.LineupOutput.ComunioClub.MatchState.byId(matchDetails?.state),
            )
        logger.info { "Scraped lineup for match $matchId" }
        return lineup
    }

    private fun mapPlayer(player: Player): MatchLineupOutput.LineupOutput.ComunioClub.ClubLineup.ComunioFootballPlayer {
        return MatchLineupOutput.LineupOutput.ComunioClub.ClubLineup.ComunioFootballPlayer(
            pid = player.playerId,
            name = player.name,
            position = MatchLineupOutput.LineupOutput.ComunioClub.ClubLineup.ComunioFootballPlayer.Position.byId(
                player.pos
            ),
            goals = player.goals,
            penaltyGoals = player.pens,
            points = player.points,
            active = MatchLineupOutput.LineupOutput.ComunioClub.ClubLineup.ComunioFootballPlayer.Active.byId(
                player.active
            ),
            substitutedInAtMin = player.subIn,
            substitutedOutAtMin = player.subOut,
        )
    }

    private fun fetchMatch(matchId: Int): Pair<Int, MatchDetails?> {
        return defaultClient
            .mutate()
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