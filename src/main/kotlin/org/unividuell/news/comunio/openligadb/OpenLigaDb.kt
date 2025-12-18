package org.unividuell.news.comunio.openligadb

import de.openligadb.api.MatchdataApi
import de.openligadb.model.Match
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Component
class OpenLigaDb {

    private val logger = KotlinLogging.logger {  }

    private val zoneBerlin = ZoneId.of("Europe/Berlin")

    private val client = RestClient.builder()
        .baseUrl("https://api.openligadb.de/")
        .requestInterceptor { request, bytes, execution ->
            logger.debug { ">>> ${request.method} ${request.uri}" }
            execution.execute(request, bytes)
        }
        .build()

    val matches: List<Match> = getMatchDataApi()

    val kickOffInstants = matches.mapNotNull { it.matchDateTimeUTC }.toSet().sorted()

    val kickOffsByMatchGroup = matches
        .groupBy { it.group?.groupOrderID }
        .mapValues { it.value.map { it.matchDateTimeUTC }.toSet() }
        .mapValues { it.value.mapNotNull { it?.atZoneSameInstant(zoneBerlin) } }

    val kickOffsGroupStartEnd = kickOffsByMatchGroup
        .mapValues { Pair(it.value.minBy { it }, it.value.maxBy { it }) }

    fun getMatchDataApi(): List<Match> {
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