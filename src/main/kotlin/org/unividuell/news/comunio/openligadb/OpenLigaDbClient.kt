package org.unividuell.news.comunio.openligadb

import de.openligadb.api.MatchdataApi
import de.openligadb.model.Match
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.zalando.logbook.Logbook
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

@Component
class OpenLigaDbClient(
    logbook: Logbook
) {

    private val logger = KotlinLogging.logger {  }

    private val zoneBerlin = ZoneId.of("Europe/Berlin")

    private val client = RestClient.builder()
        .baseUrl("https://api.openligadb.de/")
        .requestInterceptor(LogbookClientHttpRequestInterceptor(logbook))
        .build()

    data class OpenLigaDbMatch(
        val group: OpenLigaDbMatchGroup,
        val matchDateTimeUTC: OffsetDateTime,
    ) {
        data class OpenLigaDbMatchGroup(
            val groupOrderId: Int
        )
    }

    val matches: List<OpenLigaDbMatch> = getMatchDataApi()
        .map { http ->
            OpenLigaDbMatch(
                group = http.group!!.let { httpGroup ->
                    OpenLigaDbMatch.OpenLigaDbMatchGroup(
                        groupOrderId = httpGroup.groupOrderID!!
                    )
                },
                matchDateTimeUTC = http.matchDateTimeUTC!!
            )
        }

    val kickOffInstants = matches.map { it.matchDateTimeUTC }.toSet().sorted()

    val kickOffsByMatchGroup = matches
        .groupBy { it.group.groupOrderId }
        .mapValues { it.value.map { match -> match.matchDateTimeUTC }.toSet() }
        .mapValues { it.value.mapNotNull { kickoff -> kickoff.atZoneSameInstant(zoneBerlin) } }

    val kickOffsGroupStartEnd = kickOffsByMatchGroup
        .mapValues { entry -> Pair(entry.value.minBy { it }, entry.value.maxBy { it }) }

    val matchesByGroupOrderId = matches.groupBy { it.group.groupOrderId }

    private fun getMatchDataApi(): List<Match> {
        return MatchdataApi(client)
            .getmatchdataLeagueShortcutLeagueSeasonGet(leagueShortcut = "bl1", leagueSeason = 2025)
    }

    data class MatchGroup(
        val groupOrderId: Int,
    )

    fun currentMatchGroup(relativeTo: Instant): MatchGroup? {
        return kickOffsGroupStartEnd
            .mapValues {
                // extend the range of the match group by -1 and +3 days (at start of day)
                // caution: new end of match day n wins over new start of match day n+1!
                // aka: we are iterating from day 1 to 34 and trying to find the first match
                Pair(
                    it.value.first.toOffsetDateTime().toLocalDate().minusDays(1).atStartOfDay(zoneBerlin).toInstant(),
                    it.value.second.toOffsetDateTime().toLocalDate().plusDays(4).atStartOfDay(zoneBerlin).toInstant()
                )
            }
            .entries
            .firstOrNull { relativeTo >= it.value.first && relativeTo < it.value.second }
            ?.also { logger.debug { "current group for ${relativeTo}: ${it.key} (${it.value})" } }
            ?.key?.let { MatchGroup(groupOrderId = it) }
    }

}