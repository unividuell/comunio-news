package org.unividuell.news.comunio.league

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.unividuell.news.comunio.ComunioConfig

@Deprecated("Use MemberLineupClient instead")
@Component
class MyLeagueClient(
    private val comunioConfig: ComunioConfig,
    private val restClient: RestClient.Builder,
) {

    private val logger = KotlinLogging.logger {  }

    /**
     * scrapes:
     *  0. GET https://stats.comunio.de/my-league
     *  1. POST https://stats.comunio.de/cslogin
     *  2. GET https://stats.comunio.de/my-league_async.php?cid=13742756
     *  3. GET https://stats.comunio.de/xhr/playerAction.php?action=rgd&i=13095928
     *  . <all comunio-members>
     * 13. GET https://stats.comunio.de/xhr/playerAction.php?action=rgd&i=8555052
     */
    fun scrape(): List<ComunioPlayerOutput> {
        logger.info { "Start scraping my league" }
        val body = fetchMyLeague()
        ensureLoggedIn(body)
        return fetchMyLeagueAsync()
            .also { logger.info { "Finished scraping my league" } }
    }

    private fun fetchMyLeague(): String {
        return restClient.build()
            .get()
            .uri("my-league")
            .accept(MediaType.TEXT_HTML)
            .retrieve()
            .body<String>()
            ?: throw IllegalStateException("Could not fetch league page!")
    }

    private fun ensureLoggedIn(body: String) {
        val doc = Jsoup.parse(body)
        val errorNotLoggedIn = doc.selectFirst("div#content > div.warning > span#errorMsg")?.text()
        if (errorNotLoggedIn != null && errorNotLoggedIn.contains("logged")) {
            login()
        }
    }

    private fun fetchMyLeagueAsync(): List<ComunioPlayerOutput> {
        val body = restClient.build()
            .get()
            .uri { uriBuilder -> uriBuilder
                .path("my-league_async.php")
                .queryParam("cid", comunioConfig.cid)
                .build()
            }
            .retrieve()
            .body<String>()
            ?: throw IllegalStateException("No response body from async request!")
        val doc = Jsoup.parse(body)
        return selectPlayerIds(document = doc)
    }

    private fun login() {
        val formData = LinkedMultiValueMap<String, String>().apply {
            add("name", comunioConfig.stats.credentials.username)
            add("pw", comunioConfig.stats.credentials.password)
            add("stayLoggedIn", "stayLoggedIn")
        }
        val body = restClient.build()
            .post()
            .uri("cslogin")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .retrieve()
            .body<String>()
            ?: throw IllegalStateException("No response body from login request!")
        val loginDoc = Jsoup.parse(body)
        val errorNotLoggedIn = loginDoc.selectFirst("div.site > div#menu > div.warning > span#errorMsg")
        if (errorNotLoggedIn != null) {
            throw IllegalStateException("Could not login w/ credentials! ${errorNotLoggedIn.text()}")
        }
    }

    private fun selectPlayerIds(document: Document): List<ComunioPlayerOutput> {
        val selector = "//www.comunio.de/users/"
        val playerLinks = document.select("a[href^=$selector]")
        return playerLinks
            .map {
                val userId = it.attr("href").replace(selector, "")
                val username = it.text()
                ComunioPlayer(userId = userId, username = username)
            }
            .map {
                val lineup = loadPlayerLineup(comunioPlayer = it)
                ComunioPlayerOutput(
                    userId = it.userId.toLong(),
                    username = it.username,
                    lineup = lineup.players.map { player ->
                        ComunioPlayerOutput.ComunioFootballPlayer(
                            pid = player.pid.toLong(),
                            name = player.name,
                            activeByUser = ComunioPlayerOutput.UserActive.byId(id = player.active),
                            matchActive = ComunioPlayerOutput.MatchActive.byId(player.matchActive),
                            valued = player.valued == 1
                        )
                    }
                )
            }
    }

    // comunio lies about the content-type (`text/html` but it is `application/json`)!
    private val htmlJsonConverter = JacksonJsonHttpMessageConverter().apply {
        supportedMediaTypes = listOf(MediaType.APPLICATION_JSON, MediaType.TEXT_HTML)
    }

    private fun loadPlayerLineup(comunioPlayer: ComunioPlayer): ApiResponse {
        return restClient
            .configureMessageConverters { converters ->
                converters.addCustomConverter(htmlJsonConverter)
            }
            .build()
            .get()
            .uri { uriBuilder -> uriBuilder
                .path("/xhr/playerAction.php")
                .queryParam("action", "rgd")
                .queryParam("i", comunioPlayer.userId)
                .build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body<ApiResponse>()
            ?: throw IllegalStateException("Could not fetch lineup for player ${comunioPlayer.username}!")
    }

    data class ComunioPlayer(val userId: String, val username: String)

    data class ComunioPlayerOutput(
        val userId: Long,
        val username: String,
        val lineup: List<ComunioFootballPlayer>
    ) {
        data class ComunioFootballPlayer(
            val pid: Long,
            val name: String,
            val activeByUser: UserActive,
            val matchActive: MatchActive,
            val valued: Boolean,
        )
        enum class UserActive(val id: Int) {
            Active(1),
            Inactive(0),
            Unknown(-100);
            companion object {
                fun byId(id: Int?): UserActive {
                    return entries.firstOrNull { it.id == id } ?: Unknown
                }
            }
        }
        enum class MatchActive(val id: Int) {
            Active(1),
            Bench(-2),
            NotInSquad(-3),
            Unknown(-100);
            companion object {
                fun byId(id: Int?): MatchActive {
                    return entries.firstOrNull { it.id == id } ?: Unknown
                }
            }
        }
    }

    data class ApiResponse(
        val cid: Long,
        val players: List<Player>,
        val disOpts: DisOpts,
        val budgetState: Any? = null,
        val minus: Int,
        val missingPlayers: Int,
        val playersRem: Int,
        val pointsOld: Int? = null,
        val pointsNow: Int,
        val pointsNew: Int,
        val formation: String
    )

    data class Player(
        val pid: String,
        val active: Int,
        val matchActive: Int?,
        val name: String,
        val goals: Int?,
        val ownGoals: Int?,
        val pens: Int?,
        val pensSaved: Int?,
        val pensMissed: Int?,
        val assists: Int?,
        val yellow: Int?,
        val yellowred: Int?,
        val red: Int?,
        val subIn: Int?,
        val subOut: Int?,
        val rating: String?,
        val points: Int?,
        val clubId: Int,
        val clubName: String,
        val place: String?,
        val pos: String,
        val pre: Any?,
        val trending: Int,
        val rising: Int,
        val ratingLevel: Int?,
        val status: Int,
        val valued: Int,
        val clubMatchInfo: ClubMatchInfo
    )

    data class ClubMatchInfo(
        val oppClubId: Int,
        val oppClubName: String,
        val kickoff: Kickoff,
        val kickoffTime: String,
        val kickoffWeekday: Int,
        val details: Int,
        val state: String,
        val place: String
    )

    data class Kickoff(
        val date: String,
        @JsonProperty("timezone_type")
        val timezoneType: Int,
        val timezone: String
    )

    data class DisOpts(
        val showXG: Boolean,
        val showAssists: Boolean,
        val showPensSaved: Boolean,
        val showCleanSheet: Boolean,
        val showOwnGoals: Boolean,
        val showPensMissed: Boolean
    )
}