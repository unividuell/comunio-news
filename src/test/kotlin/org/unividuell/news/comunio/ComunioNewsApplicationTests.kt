package org.unividuell.news.comunio

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

@Import(TestcontainersConfiguration::class)
@TestPropertySource(properties = [
    "spring.ai.openai-sdk.api-key=FOO",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
])
@SpringBootTest
class ComunioNewsApplicationTests {

    @Test
    fun contextLoads() {
    }

}
