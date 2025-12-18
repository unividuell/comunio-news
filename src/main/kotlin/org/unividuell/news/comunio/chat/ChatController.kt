package org.unividuell.news.comunio.chat

import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openaisdk.OpenAiSdkChatModel
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux


@RestController
class ChatController(
    private val chatModel: OpenAiSdkChatModel
) {

    @GetMapping("/ai/generate")
    fun generate(
        @RequestParam(value = "message", defaultValue = "Tell me a joke") message: String?
    ): String? {
        return chatModel.call(message)
    }

    @GetMapping("/ai/generateStream")
    fun generateStream(
        @RequestParam(value = "message", defaultValue = "Tell me a joke") message: String
    ): Flux<ChatResponse> {
        val prompt: Prompt = Prompt(UserMessage(message))
        return chatModel.stream(prompt)
    }

}