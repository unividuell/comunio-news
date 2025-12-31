package org.unividuell.news.comunio.match

import io.github.oshai.kotlinlogging.KotlinLogging
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.google.genai.GoogleGenAiChatModel
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.unividuell.news.comunio.league.MyLeagueClient
import tools.jackson.databind.json.JsonMapper

@RestController
class MatchAiController(
    private val googleGenAiChatModel: GoogleGenAiChatModel,
    private val matchComposer: MatchComposer,
    private val myLeagueClient: MyLeagueClient,
    private val json: JsonMapper,
) {

    private val logger = KotlinLogging.logger {  }

    private val markdownParser = Parser.builder().build()
    private val htmlRenderer = HtmlRenderer.builder().build()

    val styles = listOf("locker", "jugendlich", "alte Schule", "statistikorientiert", "gefühlvoll")

    @GetMapping("/ai/matches/{groupOrderId}/{matchId}", produces = ["text/html"])
    fun match(
        @PathVariable groupOrderId: Int,
        @PathVariable matchId: Int,
    ): String? {
        val matches = matchComposer
            .composeMatch(groupOrderId = groupOrderId)
        val match = matches.find { it.matchId == matchId } ?: return null
        val context  = json.writeValueAsString(match)
        val style = styles
            .random()
            .also {
                logger.info { "selected style: $it" }
            }

        val prompt = """
            Du bist ein Reporter einer Sport-Zeitung.
            Antworte IMMER im Markdown-Format.
            
            Die Daten behandeln ein Spiel in der Comunio "Zingler46".
            
            Hier ist der Kontext im JSON-Format: `$context`
            
            Fokussiere dich auf eine Bewertung der beteiligten Member der Comunio. 
            Fasse dich kurz in max 3 Absätzen.
            Dein Schreibstil: $style
        """.trimIndent()
        val markdown = googleGenAiChatModel.call(prompt)
        val document = markdownParser.parse(markdown)
        return htmlRenderer.render(document) +
                """<div style="display:none;">writing style: $style</div>"""
    }

    @GetMapping("/ai/matches/{groupOrderId}", produces = ["text/html"])
    fun matches(
        @PathVariable groupOrderId: Int,
    ): String? {
        val matches = matchComposer
            .composeMatch(groupOrderId = groupOrderId)
            .let { json.writeValueAsString(it) }
        val table = myLeagueClient
            .scrapeMemberTable(groupOrderId = groupOrderId)
            .let { json.writeValueAsString(it) }
        val style = styles
            .random()
            .also {
                logger.info { "selected style: $it" }
            }

        val template = """
            Du bist ein Reporter einer Sport-Zeitung.
            Antworte IMMER im Markdown-Format.
            
            Die Daten behandeln den Spieltag {groupOrderId} der Comunio "Zingler46".
            
            Dies sind die Ergebnisse des Spieltag im JSON-Format: `{matches}`
            Dies ist die Tabelle der Comunio-Member VOR diesem Spieltag im JSON-Format: `{table}`
            
            Fokussiere dich auf eine Bewertung der beteiligten Member der Comunio.
            Widme jedem Manager 1 Absatz. Erwähne signifikante Änderungen in der Tabelle NACH diesem Spieltag für den jeweiligen Member.
            
            Erzeuge einen weiteren Absatz der sich auf erwähnenswerte Veränderungen in der Tabelle nach diesem Spieltag bezieht.
            
            Dein Schreibstil: {style}
        """.trimIndent()
        val promptTemplate = PromptTemplate(template)
        val prompt = promptTemplate.create(mapOf(
            "groupOrderId" to groupOrderId,
            "matches" to matches,
            "table" to table,
            "style" to style,
        ))
        val markdown = googleGenAiChatModel.call(prompt).result.output.text
        val document = markdownParser.parse(markdown)
        return htmlRenderer.render(document) +
                """<div style="display:none;">writing style: $style</div>"""
    }

}