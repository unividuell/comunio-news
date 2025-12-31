package org.unividuell.news.comunio.lineup.client

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.unividuell.news.comunio.ApplicationModuleTestBase
import tools.jackson.databind.json.JsonMapper

@SpringBootTest
@TestPropertySource(properties = [
    "logging.level.org.zalando.logbook=TRACE",
])
class MatchLineupClientTest : ApplicationModuleTestBase() {

    @Autowired
    lateinit var sut: MatchLineupClient

    @Autowired
    lateinit var json: JsonMapper

    @Test
    fun `it should scrape the match lineup`() {
        // act
        val actual = sut.scrapeMatchLineup(matchId = 7663)
        // assert
        println(json.writeValueAsString(actual))
    }

}