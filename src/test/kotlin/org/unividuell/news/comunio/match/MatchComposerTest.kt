package org.unividuell.news.comunio.match

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.modulith.test.ApplicationModuleTest
import org.springframework.test.context.TestPropertySource
import org.unividuell.news.comunio.ApplicationModuleTestBase
import org.unividuell.news.comunio.TestcontainersConfiguration
import org.unividuell.news.comunio.lineup.client.MatchLineupClient
import tools.jackson.databind.json.JsonMapper

@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.ALL_DEPENDENCIES)
@Import(TestcontainersConfiguration::class)
@TestPropertySource(
    properties = [
        "logging.level.org.zalando.logbook=DEBUG",
        "logging.level.org.apache.hc.client5.http.headers=INFO",
        "org.unividuell.news.comunio=INFO",
    ],
    locations = ["file:.env"]
)
class MatchComposerTest : ApplicationModuleTestBase() {

    @Autowired
    lateinit var sut: MatchComposer

    @Autowired
    lateinit var matchLineupClient: MatchLineupClient

    @Autowired
    lateinit var json: JsonMapper

//    @Autowired
//    lateinit var openLigaDbClient: OpenLigaDbClient

    @Test
    fun `it should compose a match`() {
        // arrange
        val groupOrderId = 15
        // act
        val actual = sut.composeMatch(groupOrderId = groupOrderId)
        // assert
        actual.forEach {
            println(json.writeValueAsString(it))
        }
    }

}