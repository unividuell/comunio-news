package org.unividuell.news.comunio.match

import org.springframework.stereotype.Component
import org.unividuell.news.comunio.AppConfig
import org.unividuell.news.comunio.lineup.MatchLineupClient
import org.unividuell.news.comunio.lineup.MemberLineupClient

@Component
class MatchComposer(
    private val appConfig: AppConfig,
    private val matchLineupClient: MatchLineupClient,
    private val memberLineupClient: MemberLineupClient,
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
                val member: String?,
                val position: String,
                val goals: Int,
                val penaltyGoals: Int,
                val points: Int?,
                val linedUpByClubAt: String,
                val usedByMember: Boolean?,
            )
        }
    }

    fun composeMatch(groupOrderId: Int, comunioGameId: Int): List<Match> {
        val matchLineup = matchLineupClient.scrape(groupOrderId = groupOrderId)
        val memberLineup = memberLineupClient.scrape(comunioGamedayId = comunioGameId)

        return matchLineup
            .filter { it.homeClub != null && it.awayClub != null }
            .map { match ->
                Match(
                    homeClub = match.homeClub!!.let { club -> composeClub(club = club, memberLineup = memberLineup) },
                    awayClub = match.awayClub!!.let { club -> composeClub(club = club, memberLineup = memberLineup) }
                )
            }
    }

    private fun composeClub(
        club: MatchLineupClient.LineupOutput.ComunioClub,
        memberLineup: MemberLineupClient.MemberLineupOutput,
    ): Match.AiClub {
        return Match.AiClub(
            name = appConfig.clubIdMapping.first { it.cid == club.cid }.name,
            lineup = club.lineup.players.map { player ->
                val member = memberLineup.members.find { it.lineup.any { it.playerId == player.pid } }
                val memberPlayer = member?.lineup?.find { it.playerId == player.pid }
                Match.AiClub.Player(
                    name = player.name,
                    member = member?.name,
                    position = player.position.name,
                    goals = player.goals,
                    penaltyGoals = player.penaltyGoals,
                    points = player.points,
                    linedUpByClubAt = player.active.name,
                    usedByMember = memberPlayer?.active
                )
            }
        )
    }
}