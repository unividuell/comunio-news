package org.unividuell.news.comunio.openligadb

import de.openligadb.api.MatchdataApi
import de.openligadb.model.Match
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.ZoneId

@Component
class OpenLigaDb {

    private val client = RestClient.builder().baseUrl("https://api.openligadb.de/").build()

    val matches: List<Match> = getMatchDataApi()

    val kickOffInstants = matches.mapNotNull { it.matchDateTimeUTC }.toSet().sorted()

    val kickOffsByMatchGroup = matches
        .groupBy { it.group?.groupOrderID }
        .mapValues { it.value.map { it.matchDateTimeUTC }.toSet() }
        .mapValues { it.value.map { it?.atZoneSameInstant(ZoneId.of("Europe/Berlin")) } }

    fun getMatchDataApi(): List<Match> {
        return MatchdataApi(client)
            .getmatchdataLeagueShortcutLeagueSeasonGet(leagueShortcut = "bl1", leagueSeason = 2025)
    }

}