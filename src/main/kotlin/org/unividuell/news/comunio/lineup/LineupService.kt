package org.unividuell.news.comunio.lineup

import org.springframework.stereotype.Service
import org.unividuell.news.comunio.lineup.client.MatchLineupClient
import org.unividuell.news.comunio.lineup.client.MemberLineupClient

@Service
class LineupService(
    private val matchLineupClient: MatchLineupClient,
    private val memberLineupClient: MemberLineupClient,
) {

    fun scrapeMatches(groupOrderId: Int): MatchLineupOutput {
        val matchIds = matchLineupClient.scrapeMatchIds(groupOrderId = groupOrderId)
        val matches = matchIds.matchIds.map {
            matchLineupClient.scrapeMatchLineup(matchId = it)
        }
        return MatchLineupOutput(comunioGamedayId = matchIds.comunioGamedayId, matches = matches)
    }

    fun scrapeMembers(comunioGamedayId: Int): MemberLineupOutput {
        return memberLineupClient.scrape(comunioGamedayId = comunioGamedayId)
    }

}