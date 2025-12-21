package org.unividuell.news.comunio.match

import io.github.oshai.kotlinlogging.KotlinLogging
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.google.genai.GoogleGenAiChatModel
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.json.JsonMapper

@RestController
class MatchAiController(
    private val googleGenAiChatModel: GoogleGenAiChatModel,
    private val matchComposer: MatchComposer,
    private val json: JsonMapper,
) {

    private val logger = KotlinLogging.logger {  }

    private val markdownParser = Parser.builder().build()
    private val htmlRenderer = HtmlRenderer.builder().build()

    @GetMapping("/ai/matches/{groupOrderId}/{matchOrderId}", produces = ["text/html"])
    fun match(
        @PathVariable groupOrderId: Int,
        @PathVariable matchOrderId: Int,
    ): String? {
        val matches = matchComposer
            .composeMatch(groupOrderId = groupOrderId)
        val match = matches.elementAt(matchOrderId)
        val context  = json.writeValueAsString(match)
        val style = listOf("locker", "jugendlich", "alte Schule", "statistikorientiert", "gefühlvoll")
            .random()
            .also {
                logger.info { "selected style: $it" }
            }

        val prompt = """
            Du bist ein Reporter einer Sport-Zeitung.
            Antworte IMMER im Markdown-Format.
            
            Die Daten behandeln ein Spiel in der Comunio "Zingler46".
            
            Hier ist der Kontext im JSON-Format: `$context`
            
            Fokussiere dich auf eine Bewertung der beteiligten Member. 
            Fasse dich kurz in max 3 Absätzen.
            Dein Schreibstil: $style
        """.trimIndent()
        val markdown = googleGenAiChatModel.call(prompt)
        val document = markdownParser.parse(markdown)
        return htmlRenderer.render(document) +
                """<div style="display:none;">writing style: $style</div>"""
    }

    @GetMapping("/ai/matches/{groupOrderId}")
    fun matches(
        @PathVariable groupOrderId: Int,
    ): String? {
        val matches = matchComposer
            .composeMatch(groupOrderId = groupOrderId)
        val context  = json.writeValueAsString(matches)

        val template = """
            Schreibe einen Bericht im Stil eines Zeitung-Sport-Reporters.
            Die Daten behandeln den Spieltag {groupOrderId} der Comunio "Zingler46".
            
            Hier ist der Kontext im JSON-Format: `{context}`
            
            Fokussiere dich auf eine Bewertung der beteiligten Member. 
            Widme jedem Manager 1 Absatz.
            Schreibe in Stil eines Reporters der alten Schule.
        """.trimIndent()
        val promptTemplate = PromptTemplate(template)
        val prompt = promptTemplate.create(mapOf(
            "groupOrderId" to groupOrderId,
            "context" to context
        ))
        return googleGenAiChatModel.call(prompt).result.output.text
    }

}