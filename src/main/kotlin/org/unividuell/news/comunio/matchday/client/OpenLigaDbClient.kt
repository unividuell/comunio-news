package org.unividuell.news.comunio.matchday.client

import de.openligadb.api.MatchdataApi
import de.openligadb.model.Match
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.unividuell.news.comunio.matchday.MatchGroup
import org.zalando.logbook.Logbook
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime


data class OpenLigaDbMatch(
    val matchId: Int,
    val group: OpenLigaDbMatchGroup,
    val matchDateTimeUTC: OffsetDateTime,
    val homeTeam: OpenLigaDbTeam,
    val awayTeam: OpenLigaDbTeam,
) {
    data class OpenLigaDbMatchGroup(
        val groupOrderId: Int
    )

    data class OpenLigaDbTeam(val teamId: Int, val name: String, val shortName: String, val iconUrl: String)
}

@Component
class OpenLigaDbClient(
    logbook: Logbook,
) {

    private val logger = KotlinLogging.logger {  }

    private val zoneBerlin = ZoneId.of("Europe/Berlin")

    private val client = RestClient.builder()
        .baseUrl("https://api.openligadb.de/")
        .requestInterceptor(LogbookClientHttpRequestInterceptor(logbook))
        .build()

    lateinit var matches: List<OpenLigaDbMatch>
    lateinit var matchesByGroupOrderId: Map<Int, List<OpenLigaDbMatch>>
    lateinit var kickOffsGroupStartEnd: Map<Int, Pair<ZonedDateTime, ZonedDateTime>>
    lateinit var kickOffInstants: List<OffsetDateTime>
    lateinit var kickOffsByMatchGroup: Map<Int, List<ZonedDateTime>>

    fun fetchMatches(seasonStartYear: Int) {
        matches = getMatchDataApi(seasonStartYear = seasonStartYear)
            .map { http ->
                OpenLigaDbMatch(
                    matchId = http.matchID!!,
                    group = http.group!!.let { httpGroup ->
                        OpenLigaDbMatch.OpenLigaDbMatchGroup(
                            groupOrderId = httpGroup.groupOrderID!!
                        )
                    },
                    matchDateTimeUTC = http.matchDateTimeUTC!!,
                    homeTeam = http.team1!!.let {
                        OpenLigaDbMatch.OpenLigaDbTeam(
                            teamId = it.teamId!!,
                            name = it.teamName!!,
                            shortName = it.shortName!!,
                            iconUrl = it.teamIconUrl!!,
                        )
                    },
                    awayTeam = http.team2!!.let {
                        OpenLigaDbMatch.OpenLigaDbTeam(
                            teamId = it.teamId!!,
                            name = it.teamName!!,
                            shortName = it.shortName!!,
                            iconUrl = it.teamIconUrl!!,
                        )
                    },
                )
            }
        kickOffInstants = matches.map { it.matchDateTimeUTC }.toSet().sorted()
        kickOffsByMatchGroup = matches
            .groupBy { it.group.groupOrderId }
            .mapValues { it.value.map { match -> match.matchDateTimeUTC }.toSet() }
            .mapValues { it.value.mapNotNull { kickoff -> kickoff.atZoneSameInstant(zoneBerlin) } }
        kickOffsGroupStartEnd = kickOffsByMatchGroup
            .mapValues { entry -> Pair(entry.value.minBy { it }, entry.value.maxBy { it }) }
        matchesByGroupOrderId = matches.groupBy { it.group.groupOrderId }
    }

    private fun getMatchDataApi(seasonStartYear: Int): List<Match> {
        return MatchdataApi(client)
            .getmatchdataLeagueShortcutLeagueSeasonGet(leagueShortcut = "bl1", leagueSeason = seasonStartYear)
    }

    fun currentMatchGroup(relativeTo: Instant): MatchGroup? {
        return kickOffsGroupStartEnd
            .mapValues {
                // extend the range of the matchgroup by -1 and +3 days (at start of day)
                // caution: new end of matchgroup n wins over new start of matchgroup n+1!
                // aka: we are iterating from matchgroup 1 to 34 and trying to find the first match
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