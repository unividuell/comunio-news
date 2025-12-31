package org.unividuell.news.comunio.match

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.unividuell.news.comunio.AppConfig
import org.unividuell.news.comunio.lineup.LineupService
import org.unividuell.news.comunio.lineup.MatchLineupOutput
import org.unividuell.news.comunio.lineup.MatchLineupOutput.LineupOutput.ComunioClub.ClubLineup.ComunioFootballPlayer
import org.unividuell.news.comunio.lineup.MemberLineupClient
import org.unividuell.news.comunio.match.Player.ClubLineupStatus

@Component
class MatchComposer(
    private val appConfig: AppConfig,
    private val lineupService: LineupService,
    private val memberLineupClient: MemberLineupClient,
) {

    @Cacheable(value = ["matchComposer"], key = "#groupOrderId")
    fun composeMatch(groupOrderId: Int): List<MatchComposerOutput> {
        val matchLineup = lineupService.scrape(groupOrderId = groupOrderId)
        val memberLineup = memberLineupClient.scrape(comunioGamedayId = matchLineup.comunioGamedayId)

        return matchLineup
            .matches
            .filter { it.homeClub != null && it.awayClub != null }
            .map { match ->
                MatchComposerOutput(
                    groupOrderId = groupOrderId,
                    matchId = match.matchId,
                    homeClub = match.homeClub!!.let { club -> composeClub(club = club, memberLineup = memberLineup) },
                    awayClub = match.awayClub!!.let { club -> composeClub(club = club, memberLineup = memberLineup) }
                )
            }
    }

    private fun composeClub(
        club: MatchLineupOutput.LineupOutput.ComunioClub,
        memberLineup: MemberLineupClient.MemberLineupOutput,
    ): AiClub {
        return AiClub(
            name = appConfig.clubIdMapping.first { it.cid == club.cid }.name,
            lineup = club.lineup.players.map { player ->
                val member =
                    memberLineup.members.find { it.lineup.any { memberPlayer -> memberPlayer.playerId == player.pid } }
                val memberPlayer = member?.lineup?.find { it.playerId == player.pid }
                Player(
                    name = player.name,
                    member = member?.name,
                    usedByMember = memberPlayer?.active,
                    position = player.position.name,
                    goals = player.goals,
                    penaltyGoals = player.penaltyGoals,
                    points = player.points,
                    clubLineupStatus = detectClubLineupStatus(player = player),
                    substitutedInAtMin = player.substitutedInAtMin,
                    substitutedOutAtMin = player.substitutedOutAtMin,
                )
            }
        )
    }

    private fun detectClubLineupStatus(player: ComunioFootballPlayer): ClubLineupStatus {
        return when (player.active) {
            ComunioFootballPlayer.Active.Active -> {
                // active player - probably after match start
                if (player.substitutedInAtMin != null) {
                    ClubLineupStatus.StartOnBench
                } else {
                    ClubLineupStatus.StartOnField
                }
            }

            ComunioFootballPlayer.Active.ProperlyActive -> ClubLineupStatus.StartOnField
            ComunioFootballPlayer.Active.Bench -> ClubLineupStatus.StartOnBench
            ComunioFootballPlayer.Active.Unknow -> ClubLineupStatus.NotInSquad
        }

    }
}