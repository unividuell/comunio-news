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
                val manager: String?,
                val position: String,
                val points: Int?,
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
                                myLeague.find { manager -> manager.lineup.any { it.pid == pid } }?.username
                            }
                        )
                    },
                    awayClub = lineup.awayClub!!.let { club ->
                        composeClub(
                            clubName = appConfig.clubIdMapping.first { it.cid == club.cid }.name,
                            lineup = club.lineup,
                            manager = { pid ->
                                myLeague.find { manager -> manager.lineup.any { it.pid == pid } }?.username
                            }
                        )
                    }
                )
            }
    }

    private fun composeClub(
        clubName: String,
        lineup: LineupClient.LineupOutput.ComunioClub.ClubLineup,
        manager: (Long) -> String?,
    ): Match.AiClub = Match.AiClub(
        name = clubName,
        lineup = lineup.players.map { player ->
            Match.AiClub.Player(
                name = player.name,
                manager = manager(player.pid),
                position = player.position.name,
                points = player.points,
            )
        }
    )
}