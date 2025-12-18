package org.unividuell.news.comunio.openligadb

import io.kotest.matchers.maps.shouldHaveSize
import io.micrometer.common.annotation.ValueExpressionResolver
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.ai.model.openaisdk.autoconfigure.OpenAiSdkChatAutoConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.modulith.test.ApplicationModuleTest
import java.time.ZoneId
import kotlin.collections.mapValues
import kotlin.time.Duration.Companion.minutes

@ApplicationModuleTest
@EnableAutoConfiguration(exclude=[ OpenAiSdkChatAutoConfiguration::class ])
class OpenLigaDbTest {

    @Autowired
    lateinit var sut: OpenLigaDb

    @Test
    fun `it should list all groups`() {
        // act
        val actual = sut.kickOffsByMatchGroup
        // assert
        println(actual.map { "${it.key.toString().padStart(2)} -> ${it.value}" }.joinToString("\n"))
        actual shouldHaveSize 34
    }
}