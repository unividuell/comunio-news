package org.unividuell.news.comunio.matchday.client

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.unividuell.news.comunio.ApplicationModuleTestBase

@SpringBootTest
@TestPropertySource(properties = [
    "logging.level.org.zalando.logbook=TRACE",
])
class ComunioClientTest : ApplicationModuleTestBase() {

    @Autowired
    lateinit var sut: ComunioClient

    @Test
    fun `it should scrape the match ids`() {
        // act
        val actual = sut.scrapeMatchIds(groupOrderId = 15)
        // assert
        actual.comunioGamedayId shouldBe 395809
        actual.matchIds shouldHaveSize 9
        actual.matchIds shouldContain 7663
    }

}