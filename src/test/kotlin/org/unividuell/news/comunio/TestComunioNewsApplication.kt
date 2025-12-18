package org.unividuell.news.comunio

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
    fromApplication<ComunioNewsApplication>().with(TestcontainersConfiguration::class).run(*args)
}
