package org.unividuell.news.comunio

import org.springframework.test.context.TestPropertySource

@TestPropertySource(
    properties = [
        "spring.ai.openai-sdk.api-key=FOO",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    ]
)
abstract class ApplicationModuleTestBase