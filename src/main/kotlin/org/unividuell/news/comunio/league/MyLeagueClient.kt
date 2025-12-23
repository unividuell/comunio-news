package org.unividuell.news.comunio.league

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.unividuell.news.comunio.ComunioConfig
import org.unividuell.news.comunio.login.LoginStatsComunio

@Component
class MyLeagueClient(
    val comunioConfig: ComunioConfig,
    restClientBuilder: RestClient.Builder,
    private val loginStatsComunio: LoginStatsComunio,
) {

    private val logger = KotlinLogging.logger {  }

    private val defaultClient = restClientBuilder.build()

    /**
     * scrapes:
     *  0. GET https://stats.comunio.de/my-league
     *  1. POST https://stats.comunio.de/cslogin
     *  2. GET https://stats.comunio.de/my-league_async.php?cid=13742756
     *  3. GET https://stats.comunio.de/xhr/playerAction.php?action=rgd&i=13095928
     *  . <all comunio-members>
     * 13. GET https://stats.comunio.de/xhr/playerAction.php?action=rgd&i=8555052
     */
    @Deprecated("Use MemberLineupClient instead")
    fun scrapeMemberLineup(): ComunioMemberLineupOutput {
        logger.info { "Start scraping my league for lineup" }
        preflight()
        return fetchMyLeagueAsync()
            .let { parseMemberLineup(document = it) }
            .let { ComunioMemberLineupOutput(memberLineups = it) }
            .also { logger.info { "Finished scraping my league for lineup" } }
    }

    /**
     * scrapes:
     *  0. GET https://stats.comunio.de/my-league
     *  1. POST https://stats.comunio.de/cslogin
     *  2. GET https://stats.comunio.de/my-league_async.php?cid=13742756
     */
    @Cacheable(value = ["scrapeMemberTable"], key = "#root.target.comunioConfig.season + '_' + #groupOrderId")
    fun scrapeMemberTable(groupOrderId: Int): ComunioMemberTableOutput {
        logger.info { "Start scraping my league for table" }
        preflight()
        return fetchMyLeagueAsync()
            .let { parseMemberTable(document = it) }
            .let { ComunioMemberTableOutput(table = it) }
            .also { logger.info { "Finished scraping my league for table" } }
    }

    private fun preflight() {
        val body = fetchMyLeague()
        loginStatsComunio.ensureLoggedIn(body)
    }

    private fun fetchMyLeague(): String {
        return defaultClient
            .get()
            .uri("my-league")
            .accept(MediaType.TEXT_HTML)
            .retrieve()
            .body<String>()
            ?: throw IllegalStateException("Could not fetch league page!")
    }

    private fun fetchMyLeagueAsync(): Document {
        val body = defaultClient
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
        return doc
    }

    private fun parseMemberTable(document: Document): List<ComunioMemberTableOutput.ComunioMemberTableItem> {
        val selector = "//www.comunio.de/users/"
        val memberTableBodyRows = document.select("table.liga tr.rowLink:gt(0)")
        return memberTableBodyRows.map { member ->
            val memberCols = member.select("> td")
            // 0: member
            val memberNameCol = memberCols[0].selectFirst("a")!!
            val memberId = memberNameCol.attr("href").replace(selector, "")
            val memberName = memberNameCol.text()
            // 1: position in table
            // 2: pre matchday points
            val memberPreMatchdayPoints = memberCols[2].text().toInt()
            // 3: remaining football players
            // 4: points current matchday
            val memberPointsCurrentMatchday = memberCols[4].text().toInt()
            // 5: icon table change (up/down/..)
            // 6: post matchday points
            val memberPostMatchdayPoints = memberCols[6].text().toInt()
            ComunioMemberTableOutput.ComunioMemberTableItem(
                memberId = memberId.toLong(),
                username = memberName,
                preMatchdayPoints = memberPreMatchdayPoints,
                pointsCurrentMatchday = memberPointsCurrentMatchday,
                postMatchdayPoints = memberPostMatchdayPoints,
            )
        }.sortedByDescending { it.preMatchdayPoints }
    }

    private fun parseMemberLineup(document: Document): List<ComunioMemberLineupOutput.ComunioMemberLineupItem> {
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
                ComunioMemberLineupOutput.ComunioMemberLineupItem(
                    userId = it.userId.toLong(),
                    username = it.username,
                    lineup = lineup.players.map { player ->
                        ComunioMemberLineupOutput.ComunioMemberLineupItem.ComunioFootballPlayer(
                            pid = player.pid.toLong(),
                            name = player.name,
                            activeByUser = ComunioMemberLineupOutput.ComunioMemberLineupItem.UserActive.byId(id = player.active),
                            matchActive = ComunioMemberLineupOutput.ComunioMemberLineupItem.MatchActive.byId(player.matchActive),
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
        return defaultClient
            .mutate()
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

    data class ComunioMemberTableOutput(
        val table: List<ComunioMemberTableItem>
    ) {
        data class ComunioMemberTableItem(
            val memberId: Long,
            val username: String,
            val preMatchdayPoints: Int,
            val pointsCurrentMatchday: Int,
            val postMatchdayPoints: Int,
        )
    }

    data class ComunioMemberLineupOutput(
        val memberLineups: List<ComunioMemberLineupItem>
    ) {
        data class ComunioMemberLineupItem(
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