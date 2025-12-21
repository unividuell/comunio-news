package org.unividuell.news.comunio.chat

import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.google.genai.GoogleGenAiChatModel
import org.springframework.ai.openaisdk.OpenAiSdkChatModel
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.unividuell.news.comunio.match.MatchComposer
import reactor.core.publisher.Flux
import tools.jackson.databind.json.JsonMapper


@RestController
class ChatController(
    private val openAiSdkChatModel: OpenAiSdkChatModel,
    private val googleGenAiChatModel: GoogleGenAiChatModel,
    private val matchComposer: MatchComposer,
    private val json: JsonMapper,
) {

    @GetMapping("/ai/generate")
    fun generate(
        @RequestParam(value = "message", defaultValue = "Tell me a joke") message: String?
    ): String? {
        return googleGenAiChatModel.call(message)
    }

    @GetMapping("/ai/generateStream")
    fun generateStream(
        @RequestParam(value = "message", defaultValue = "Tell me a joke") message: String
    ): Flux<ChatResponse> {
        val prompt: Prompt = Prompt(UserMessage(message))
        return openAiSdkChatModel.stream(prompt)
    }

    @GetMapping("/ai/matchdays/{groupOrderId}/{matchOrderId}")
    fun match(
        @PathVariable groupOrderId: Int,
        @PathVariable matchOrderId: Int,
    ): String? {
        val matches = matchComposer
            .composeMatch(groupOrderId = groupOrderId)
        val match = matches.elementAt(matchOrderId)
        val context  = json.writeValueAsString(match)

        val prompt = """
            Schreibe einen Bericht im Stil eines Zeitung-Sport-Reporters.
            Die Daten behandeln ein Spiel in der Comunio "Zingler46".
            
            Hier ist der Kontext im JSON-Format: `$context`
            
            Fokussiere dich auf eine Bewertung der beteiligten Member. 
            Schreibe in einem lockeren Stil. Fasse dich kurz in max 3 Absätzen.
        """.trimIndent()
        return googleGenAiChatModel.call(prompt)
    }

    @GetMapping("/ai/matchdays/{groupOrderId}")
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