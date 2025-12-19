package org.unividuell.news.comunio.lineup

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.modulith.test.ApplicationModuleTest
import org.springframework.test.context.TestPropertySource

@ApplicationModuleTest
@TestPropertySource(properties = [
    "logging.level.org.zalando.logbook=TRACE",
    "spring.ai.openai-sdk.api-key=FOO",
])
class LineupClientTest {

    @Autowired
    lateinit var sut: LineupClient

//    @Disabled
    @Test
    fun `it should scrape the lineup`() {
        val actual = sut.scrape(groupOrderId = 15)
        actual.forEach { println(it) }
    }

}