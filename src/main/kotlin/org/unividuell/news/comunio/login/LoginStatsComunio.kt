package org.unividuell.news.comunio.login

import org.jsoup.Jsoup
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.unividuell.news.comunio.ComunioConfig

@Component
class LoginStatsComunio(
    private val comunioConfig: ComunioConfig,
    restClientBuilder: RestClient.Builder,
) {

    private val defaultClient = restClientBuilder.build()

    fun ensureLoggedIn(body: String) {
        val doc = Jsoup.parse(body)
        val errorNotLoggedIn = doc.selectFirst("div#content > div.warning > span#errorMsg")?.text()
        if (errorNotLoggedIn != null && errorNotLoggedIn.contains("logged")) {
            login()
        }
    }


    private fun login() {
        val formData = LinkedMultiValueMap<String, String>().apply {
            add("name", comunioConfig.stats.credentials.username)
            add("pw", comunioConfig.stats.credentials.password)
            add("stayLoggedIn", "stayLoggedIn")
        }
        val body = defaultClient
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

}