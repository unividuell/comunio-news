package org.unividuell.news.comunio.matchday

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.unividuell.news.comunio.AppConfig
import org.unividuell.news.comunio.OpenligaDbConfig
import org.unividuell.news.comunio.matchday.client.ComunioClient
import org.unividuell.news.comunio.matchday.client.OpenLigaDbClient
import java.time.Instant

@Service
class MatchdayService(
    private val openLigaDbClient: OpenLigaDbClient,
    private val comunioClient: ComunioClient,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val openligaDbConfig: OpenligaDbConfig,
    private val appConfig: AppConfig,
) {

    // only IDs - these don't get stale over time
    private val matchGroupCacheByGroupOrderId = mutableMapOf<Int, MatchGroup>()

    @Transactional
    fun updateMatchdaysOfSeason() {
        openLigaDbClient.fetchMatches(seasonStartYear = openligaDbConfig.season)
        applicationEventPublisher.publishEvent(OpenLigaDbFetched())
    }

    fun currentMatchGroup(relativeTo: Instant): MatchGroup? {
        val openLiga = openLigaDbClient
            .currentMatchGroup(relativeTo)
            ?: return null
        if (matchGroupCacheByGroupOrderId.containsKey(openLiga.groupOrderId)) {
            return matchGroupCacheByGroupOrderId[openLiga.groupOrderId]
        }

        val comunio = comunioClient.scrapeMatchIds(groupOrderId = openLiga.groupOrderId)
        val matchGroup = MatchGroup(
            openLigaGroupOrderId = openLiga.groupOrderId,
            comunioGamedayId = comunio.comunioGamedayId,
            comunioMatchIds = comunio.matchIds,
        )
        matchGroupCacheByGroupOrderId[openLiga.groupOrderId] = matchGroup
        return matchGroup
    }

    fun matchGroup(groupOrderId: Int): MatchGroup? {
        if (matchGroupCacheByGroupOrderId.containsKey(groupOrderId)) {
            return matchGroupCacheByGroupOrderId[groupOrderId]
        }
        val comunio = comunioClient.scrapeMatchIds(groupOrderId = groupOrderId)
        val matchGroup = MatchGroup(
            openLigaGroupOrderId = groupOrderId,
            comunioGamedayId = comunio.comunioGamedayId,
            comunioMatchIds = comunio.matchIds,
        )
        matchGroupCacheByGroupOrderId[groupOrderId] = matchGroup
        return matchGroup
    }

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
                        groupOrderId = groupOrderId,
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