package org.unividuell.news.comunio.lineup.client

import io.kotest.matchers.collections.shouldHaveSize
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
class MemberLineupClientTest : ApplicationModuleTestBase() {

    @Autowired
    lateinit var sut: MemberLineupClient

    @Autowired
    lateinit var json: JsonMapper

    @Test
    fun `it should scrape the lineup`() {
        // act
        val actual = sut.scrape(comunioGamedayId = 395809)
        // assert
        actual.members.forEach { println(json.writeValueAsString(it)) }
        actual.members shouldHaveSize 10
    }

}