package org.unividuell.news.comunio.openligadb

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionalEventListener
import java.time.Instant
import java.util.concurrent.ScheduledFuture

@Service
class OpenLigaDbScheduler(
    private val taskScheduler: TaskScheduler,
    private val openLigaDbClient: OpenLigaDbClient,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {

    private val logger = KotlinLogging.logger {  }

    lateinit var scheduledTask: ScheduledFuture<*>

    fun boot() {
        openLigaDbClient.fetchMatches()
    }

    @Scheduled(cron = "0 0 8 * * *")
    fun updateMatches() {
        logger.info { "Updating OpenLigaDb matches" }
        openLigaDbClient.fetchMatches()
    }

    data class CurrentMatchGroup(val groupOrderId: Int)
    data class MatchStart(val match: OpenLigaDbClient.OpenLigaDbMatch)

    @ApplicationModuleListener
    fun on(event: OpenLigaDbClient.OpenLigaDbFetched) {
        logger.info { "Consuming OpenLigaDbFetched event $event" }
        openLigaDbClient
            .currentMatchGroup(relativeTo = Instant.now())
            ?.let { applicationEventPublisher.publishEvent(CurrentMatchGroup(it.groupOrderId)) }
            ?: run { logger.warn { "No current match group found" } }
    }

    @Async
    @TransactionalEventListener
    fun on(event: CurrentMatchGroup) {
        logger.info { "Consuming CurrentMatchGroup event $event" }
        openLigaDbClient
            .matchesByGroupOrderId[event.groupOrderId]
            ?.also { logger.info { "Found ${it.size} matches for group ${event.groupOrderId}" } }
            ?.forEach { match ->
                scheduleMatchStartJob(executionTime = match.matchDateTimeUTC.toInstant(), match = match)
            }
    }

    fun scheduleMatchStartJob(executionTime: Instant, match: OpenLigaDbClient.OpenLigaDbMatch) {
        logger.info { "Scheduling match start job for $executionTime for match ${match.homeTeam?.name} vs ${match.awayTeam?.name}" }

        scheduledTask = taskScheduler.schedule(
            Runnable {
                logger.info { "Executing match start job for ${match.homeTeam?.name} vs ${match.awayTeam?.name}" }
                applicationEventPublisher.publishEvent(MatchStart(match))
            },
            executionTime
        )
    }

}