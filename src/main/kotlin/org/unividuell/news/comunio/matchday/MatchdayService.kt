package org.unividuell.news.comunio.matchday

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.unividuell.news.comunio.AppConfig
import org.unividuell.news.comunio.OpenligaDbConfig
import org.unividuell.news.comunio.matchday.client.OpenLigaDbClient
import java.time.Instant

@Service
class MatchdayService(
    private val openLigaDbClient: OpenLigaDbClient,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val openligaDbConfig: OpenligaDbConfig,
    private val appConfig: AppConfig,
) {

    @Transactional
    fun updateMatchdaysOfSeason() {
        openLigaDbClient.fetchMatches(seasonStartYear = openligaDbConfig.season)
        applicationEventPublisher.publishEvent(OpenLigaDbFetched())
    }

    fun currentMatchGroup(relativeTo: Instant) = openLigaDbClient.currentMatchGroup(relativeTo)

    fun matchesByGroupOrderId(groupOrderId: Int): List<Match>? {
        return openLigaDbClient
            .matchesByGroupOrderId[groupOrderId]
            ?.map { client ->
                Match(
                    id = Match.MatchId(
                        season = Match.Season(
                            startYear = appConfig.season.start,
                            endYear = appConfig.season.end,
                        ),
                        matchdayOrderId = groupOrderId,
                        home = Match.Club(
                            name = client.homeTeam.name,
                            shortName = client.homeTeam.shortName,
                            iconUrl = client.homeTeam.iconUrl,
                        ),
                        away = Match.Club(
                            name = client.awayTeam.name,
                            shortName = client.awayTeam.shortName,
                            iconUrl = client.awayTeam.iconUrl,
                        )
                    ),
                    kickoffTime = client.matchDateTimeUTC
                )
            }
    }
}