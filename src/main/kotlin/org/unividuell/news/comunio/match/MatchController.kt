package org.unividuell.news.comunio.match

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import java.time.LocalDateTime


@Controller
class MatchController(
    private val matchComposer: MatchComposer
) {

    @GetMapping("/")
    fun index(model: Model): String {
        val groupOrderId = 15
        val matches = matchComposer.composeMatch(groupOrderId = groupOrderId)
        model.addAttribute("groupOrderId", groupOrderId)
        model.addAttribute("matches", matches)
        return "matches"
    }

    @PostMapping("/clicked")
    fun clicked(model: Model): String {
        model.addAttribute("now", LocalDateTime.now().toString())
        return "clicked :: result"
    }
}