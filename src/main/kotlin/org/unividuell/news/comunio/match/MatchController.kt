package org.unividuell.news.comunio.match

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import java.time.LocalDateTime


@Controller
class MatchController {

    @GetMapping("/")
    fun index() = "index"

    @PostMapping("/clicked")
    fun clicked(model: Model): String {
        model.addAttribute("now", LocalDateTime.now().toString())
        return "clicked :: result"
    }
}