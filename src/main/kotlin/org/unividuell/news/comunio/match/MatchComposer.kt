package org.unividuell.news.comunio.match

import org.springframework.stereotype.Component
import org.unividuell.news.comunio.AppConfig
import org.unividuell.news.comunio.league.MyLeagueClient
import org.unividuell.news.comunio.lineup.LineupClient

@Component
class MatchComposer(
    private val appConfig: AppConfig,
    private val lineupClient: LineupClient,
    private val myLeagueClient: MyLeagueClient,
) {

    data class Match(
        val homeClub: AiClub,
        val awayClub: AiClub,
    ) {
        data class AiClub(
            val name: String,
            val lineup: List<Player>,
        ) {
            data class Player(
                val name: String,
                val user: String?,
                val position: String,
                val goals: Int,
                val penaltyGoals: Int,
                val points: Int?,
                val activeByClub: String,
                val activeByUser: String?,
            )
        }
    }

    fun composeMatch(groupOrderId: Int): List<Match> {
        val lineups = lineupClient.scrape(groupOrderId = groupOrderId)
        val myLeague = myLeagueClient.scrape()

        return lineups
            .filter { it.homeClub != null && it.awayClub != null }
            .map { lineup ->
                Match(
                    homeClub = lineup.homeClub!!.let { club ->
                        composeClub(
                            clubName = appConfig.clubIdMapping.first { it.cid == club.cid }.name,
                            lineup = club.lineup,
                            manager = { pid ->
                                myLeague.find { manager -> manager.lineup.any { it.pid == pid } }
                            }
                        )
                    },
                    awayClub = lineup.awayClub!!.let { club ->
                        composeClub(
                            clubName = appConfig.clubIdMapping.first { it.cid == club.cid }.name,
                            lineup = club.lineup,
                            manager = { pid ->
                                myLeague.find { manager -> manager.lineup.any { it.pid == pid } }
                            }
                        )
                    }
                )
            }
    }

    private fun composeClub(
        clubName: String,
        lineup: LineupClient.LineupOutput.ComunioClub.ClubLineup,
        manager: (Long) -> MyLeagueClient.ComunioPlayerOutput?,
    ): Match.AiClub {
        return Match.AiClub(
            name = clubName,
            lineup = lineup.players.map { player ->
                val manager = manager(player.pid)
                Match.AiClub.Player(
                    name = player.name,
                    user = manager?.username,
                    position = player.position.name,
                    goals = player.goals,
                    penaltyGoals = player.penaltyGoals,
                    points = player.points,
                    activeByClub = player.active.name,
                    activeByUser = manager?.lineup?.find { it.pid == player.pid }?.activeByUser?.name
                )
            }
        )
    }
}