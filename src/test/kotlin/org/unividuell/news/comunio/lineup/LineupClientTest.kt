package org.unividuell.news.comunio.lineup

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.ai.model.openaisdk.autoconfigure.OpenAiSdkChatAutoConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.PropertySource
import org.springframework.modulith.test.ApplicationModuleTest
import org.springframework.test.context.TestPropertySource
import org.unividuell.news.comunio.openligadb.OpenLigaDb

@ApplicationModuleTest
@EnableAutoConfiguration(exclude=[ OpenAiSdkChatAutoConfiguration::class ])
@TestPropertySource(properties = ["logging.level.org.unividuell.news.comunio.lineup=DEBUG"])
class LineupClientTest {

    @Autowired
    lateinit var sut: LineupClient

    @Disabled
    @Test
    fun `it should scrape the lineup`() {
        val actual = sut.scrape(OpenLigaDb.MatchGroup(groupOrderId = 15))
        actual?.forEach { println(it) }
    }

}