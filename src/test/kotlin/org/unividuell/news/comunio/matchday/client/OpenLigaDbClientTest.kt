package org.unividuell.news.comunio.matchday.client

import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.unividuell.news.comunio.ApplicationModuleTestBase
import java.time.OffsetDateTime

@SpringBootTest
@TestPropertySource(properties = [
    "logging.level.org.zalando.logbook=TRACE",
])
class OpenLigaDbClientTest : ApplicationModuleTestBase() {

    @Autowired
    lateinit var sut: OpenLigaDbClient

    @BeforeEach
    fun setUp() {
        sut.fetchMatches(seasonStartYear = 2025)
    }

    @Test
    fun `it should list all groups`() {
        // act
        val actual = sut.kickOffsByMatchGroup
        // assert
        println(actual.map { "${it.key.toString().padStart(2)} -> ${it.value}" }.joinToString("\n"))
        actual shouldHaveSize 34
    }

    @Test
    fun `it should provide the start and end instant of a group`() {
        // act
        val actual = sut.kickOffsGroupStartEnd
        // assert
        actual.getValue(15).run {
            first.toOffsetDateTime() shouldBe OffsetDateTime.parse("2025-12-19T20:30+01:00")
            second.toOffsetDateTime() shouldBe OffsetDateTime.parse("2025-12-21T17:30+01:00")
        }
    }

    @Nested
    inner class CurrentMatchGroup {

        @ParameterizedTest
        @CsvSource(value = [
            "2025-12-16T00:00+01:00, 14", // -3d
            "2025-12-16T23:59+01:00, 14", // -3d
            "2025-12-17T00:00+01:00, 14", // -2d
            "2025-12-17T23:59+01:00, 14", // -2d

            "2025-12-18T10:32+01:00, 15", // -1d
            "2025-12-19T20:29+01:00, 15", // -0d
            "2025-12-19T20:30+01:00, 15", // first match at 15
            "2025-12-21T17:30+01:00, 15", // last match at 15
            "2025-12-21T17:31+01:00, 15", // +0d
            "2025-12-22T17:30+01:00, 15", // +1d
            "2025-12-23T23:59+01:00, 15", // +2d
            "2025-12-24T00:00+01:00, 15", // +3d
            "2025-12-24T23:59+01:00, 15", // +3d

            "2025-12-25T00:00+01:00, null", // +4d
        ], nullValues = ["null"])
        fun `before, inside, and after match group 15`(offsetDateTime: String, expected: String?) {
            // arrange
            val relativeTo = OffsetDateTime.parse(offsetDateTime)
            // act
            val actual = sut.currentMatchGroup(relativeTo = relativeTo.toInstant())
            // assert
            actual?.groupOrderId shouldBe expected?.toInt()
        }
    }
}