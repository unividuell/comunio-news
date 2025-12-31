package org.unividuell.news.comunio.lineup

import org.springframework.stereotype.Service
import org.unividuell.news.comunio.lineup.client.MatchLineupClient

@Service
class LineupService(
    private val matchLineupClient: MatchLineupClient,
) {

    fun scrape(groupOrderId: Int): MatchLineupOutput {
        val matchIds = matchLineupClient.scrapeMatchIds(groupOrderId = groupOrderId)
        val matches = matchIds.matchIds.map {
            matchLineupClient.scrapeMatchLineup(matchId = it)
        }
        return MatchLineupOutput(comunioGamedayId = matchIds.comunioGamedayId, matches = matches)
    }

}