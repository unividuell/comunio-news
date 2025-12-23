package org.unividuell.news.comunio.league

import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.modulith.test.ApplicationModuleTest
import org.springframework.test.context.TestPropertySource
import org.unividuell.news.comunio.TestcontainersConfiguration

@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
@Import(TestcontainersConfiguration::class)
@TestPropertySource(
    properties = [
        // request header `COOKIE` is set by underlying httpclient - logback does not see this
        // note: `logbook-httpclient5` does not work also
        "logging.level.org.zalando.logbook=TRACE",
        // use the build in logging :)
        // "logging.level.org.apache.hc.client5.http.headers=DEBUG",
        "spring.ai.openai-sdk.api-key=FOO",
    ],
    locations = ["file:.env"]
)
class MyLeagueClientTest {

    @Autowired
    lateinit var sut: MyLeagueClient

    @Test
    fun `it should scrape the member lineup`() {
        // act
        val actual = sut.scrapeMemberLineup()
        // assert
        actual.memberLineups.forEach { println("${it.username.padStart(12)}: ${it.lineup.joinToString(", ") { "${it.name} (${it.matchActive})" }}") }
        actual.memberLineups shouldHaveSize 10
    }

    @Test
    fun `it should scrape the member table`() {
        // act
        val actual = sut.scrapeMemberTable(groupOrderId = 15)
        // assert
        actual.table.forEach { println(it.toString()) }
        actual.table shouldHaveSize 10
    }

}