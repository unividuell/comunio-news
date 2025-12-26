package org.unividuell.news.comunio.lineup

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.modulith.test.ApplicationModuleTest
import org.springframework.test.context.TestPropertySource
import org.unividuell.news.comunio.ApplicationModuleTestBase
import tools.jackson.databind.json.JsonMapper

@ApplicationModuleTest
@TestPropertySource(properties = [
    "logging.level.org.zalando.logbook=TRACE",
])
class MatchLineupClientTest : ApplicationModuleTestBase() {

    @Autowired
    lateinit var sut: MatchLineupClient

    @Autowired
    lateinit var json: JsonMapper

    @Test
    fun `it should scrape the lineup`() {
        // act
        val actual = sut.scrape(groupOrderId = 15)
        // assert
        actual.comunioGamedayId shouldBe 395809
        actual.matches.forEach { println(json.writeValueAsString(it)) }
    }

}