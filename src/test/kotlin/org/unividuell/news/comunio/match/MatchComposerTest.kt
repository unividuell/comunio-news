package org.unividuell.news.comunio.match

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.modulith.test.ApplicationModuleTest
import org.springframework.test.context.TestPropertySource
import org.unividuell.news.comunio.league.MyLeagueClient
import org.unividuell.news.comunio.lineup.LineupClient
import org.unividuell.news.comunio.openligadb.OpenLigaDbClient

@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
@TestPropertySource(
    properties = [
        "logging.level.org.zalando.logbook=TRACE",
        "logging.level.org.apache.hc.client5.http.headers=DEBUG",
        "org.unividuell.news.comunio=INFO",
        "spring.ai.openai-sdk.api-key=FOO",
    ],
    locations = ["file:.env"]
)
class MatchComposerTest {

    @Autowired
    lateinit var sut: MatchComposer

    @Autowired
    lateinit var lineupClient: LineupClient

    @Autowired
    lateinit var myLeagueClient: MyLeagueClient

//    @Autowired
//    lateinit var openLigaDbClient: OpenLigaDbClient

    @Test
    fun `it should compose a match`() {
        // arrange
        val groupOrderId = 15
//        val matches = openLigaDbClient.matchesByGroupOrderId[groupOrderId]!!
        // act
        val actual = sut.composeMatch(groupOrderId = groupOrderId)
        // assert
        actual.forEach {
            println("${it.homeClub.name} - ${it.awayClub.name}")
            println("HOME")
            println(it.homeClub.lineup.map { "${it.name} [${it.manager}]" }.joinToString(", "))
            println("AWAY")
            println(it.awayClub.lineup.map { "${it.name} [${it.manager}]" }.joinToString(", "))
        }
    }

}