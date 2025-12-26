package org.unividuell.news.comunio.initializer

import org.springframework.modulith.ApplicationModuleInitializer
import org.springframework.stereotype.Component
import org.unividuell.news.comunio.openligadb.OpenLigaDbScheduler

@Component
class MyInitializer(
    private val openLigaDbScheduler: OpenLigaDbScheduler
) : ApplicationModuleInitializer {
    override fun initialize() {
        openLigaDbScheduler.boot()
    }
}