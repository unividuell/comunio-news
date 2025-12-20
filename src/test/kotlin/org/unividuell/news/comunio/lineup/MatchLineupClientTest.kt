package org.unividuell.news.comunio.lineup

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.modulith.test.ApplicationModuleTest
import org.springframework.test.context.TestPropertySource
import tools.jackson.databind.json.JsonMapper

@ApplicationModuleTest
@TestPropertySource(properties = [
    "logging.level.org.zalando.logbook=TRACE",
    "spring.ai.openai-sdk.api-key=FOO",
])
class MatchLineupClientTest {

    @Autowired
    lateinit var sut: MatchLineupClient

    @Autowired
    lateinit var json: JsonMapper

//    @Disabled
    @Test
    fun `it should scrape the lineup`() {
        val actual = sut.scrape(groupOrderId = 15)
        actual.forEach { println(json.writeValueAsString(it)) }
    }

}