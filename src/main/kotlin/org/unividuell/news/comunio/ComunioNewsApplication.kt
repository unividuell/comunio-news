package org.unividuell.news.comunio

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(ComunioConfig::class)
class ComunioNewsApplication

fun main(args: Array<String>) {
    runApplication<ComunioNewsApplication>(*args)
}
