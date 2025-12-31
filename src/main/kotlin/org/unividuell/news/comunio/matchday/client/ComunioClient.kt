package org.unividuell.news.comunio.matchday.client

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class ComunioClient(
    restClientBuilder: RestClient.Builder,
) {

    private val logger = KotlinLogging.logger {  }

    private val defaultClient = restClientBuilder.build()

    /**
     * scraps:
     * 0. GET https://stats.comunio.de/matchday/2025-26/15
     */
    fun scrapeMatchIds(groupOrderId: Int): ComunioMatchIds {
        logger.info { "Scraping lineup for groupOrderId $groupOrderId" }
        val fetch = fetchLineUp(groupOrderId = groupOrderId)
        val parsed = parseLineup(body = fetch)
        val comunioGamedayId = selectComunioGamedayId(document = parsed)
        val matchElements = selectMatches(document = parsed)
        return ComunioMatchIds(comunioGamedayId = comunioGamedayId, matchIds = selectMatchIds(matches = matchElements))
    }

    private fun fetchLineUp(groupOrderId: Int): String {
        val response = defaultClient
            .get()
            .uri("matchday/2025-26/{matchGroupOrderId}", groupOrderId)
            .accept(MediaType.TEXT_HTML)
            .retrieve()
            .body<String>()
            ?: throw IllegalStateException("Could not fetch lineup!")
        return response
    }

    private fun parseLineup(body: String): Document {
        return Jsoup.parse(body)
    }

    private fun selectComunioGamedayId(document: Document): Int {
        return document.selectFirst("div#comMatchday")
            ?.text()
            ?.toIntOrNull()
            ?: throw IllegalStateException("Could not fetch comunioGamedayId!")
    }

    private fun selectMatches(document: Document): Elements {
        return document.select("div#content > div.matches > div.match")
    }

    private fun selectMatchIds(matches: Elements): List<Int> {
        val selector = "matchDetails_"
        return matches.mapNotNull { match ->
            val details = match.selectFirst("div[id^=$selector]")
            details?.id()?.replace(selector, "")?.toInt()
        }
    }

}

data class ComunioMatchIds(
    val comunioGamedayId: Int,
    val matchIds: List<Int>,
)