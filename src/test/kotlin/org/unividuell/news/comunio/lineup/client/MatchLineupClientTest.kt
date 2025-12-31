package org.unividuell.news.comunio.lineup.client

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
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
    fun `it should scrape the match ids`() {
        // act
        val actual = sut.scrapeMatchIds(groupOrderId = 15)
        // assert
        actual.comunioGamedayId shouldBe 395809
        actual.matchIds shouldHaveSize 9
        actual.matchIds shouldContain 7663
    }

    @Test
    fun `it should scrape the match lineup`() {
        // act
        val actual = sut.scrapeMatchLineup(matchId = 7663)
        // assert
        println(json.writeValueAsString(actual))
    }

}