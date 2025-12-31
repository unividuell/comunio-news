package org.unividuell.news.comunio.lineup

import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.converter.GenericConverter
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.unividuell.news.comunio.H2JsonReadingConverter
import org.unividuell.news.comunio.H2JsonWritingConverter
import org.unividuell.news.comunio.lineup.repository.LineupRepository

@Configuration
@EnableJdbcRepositories(basePackageClasses = [LineupRepository::class])
class Configuration(
    private val applicationContext: ApplicationContext
) {

    @Bean
    fun converters(): List<Converter<*, *>> = listOf(
        // empty
    )

    @Bean
    fun genericConverters(): List<GenericConverter> = listOf(
        H2JsonWritingConverter(
            sourceClazz = MatchLineupOutput.LineupOutput::class.java,
            applicationContext = applicationContext
        ),
        H2JsonReadingConverter(
            targetClazz = MatchLineupOutput.LineupOutput::class.java,
            applicationContext = applicationContext
        ),
    )
}