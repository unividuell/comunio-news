package org.unividuell.news.comunio.openligadb

import io.github.oshai.kotlinlogging.KotlinLogging
import org.quartz.*
import org.springframework.context.ApplicationEventPublisher
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionalEventListener
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import java.time.Instant

@Service
class OpenLigaDbScheduler(
    private val openLigaDbClient: OpenLigaDbClient,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val scheduler: Scheduler,
    private val json: JsonMapper,
) {

    private val logger = KotlinLogging.logger {  }

    fun boot() {
        openLigaDbClient.fetchMatches()
    }

    @Scheduled(cron = "0 0 8 * * *")
    fun updateMatches() {
        logger.info { "Updating OpenLigaDb matches" }
        openLigaDbClient.fetchMatches()
    }

    data class CurrentMatchGroup(val groupOrderId: Int)

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

        val jobDetail = JobBuilder.newJob(RunningMatchJob::class.java)
            .withIdentity("match-${match.matchId}")
            .usingJobData("match", json.writeValueAsString(match))
            .build()

        val trigger = TriggerBuilder.newTrigger()
            .startAt(executionTime)
            .build()

        scheduler.scheduleJob(jobDetail, trigger)
    }

}

class RunningMatchJob(
    private val json: JsonMapper
) : Job {
    private val logger = KotlinLogging.logger {  }
    override fun execute(context: JobExecutionContext) {
        val match = json.readValue<OpenLigaDbClient.OpenLigaDbMatch>(context.jobDetail.jobDataMap["match"] as String)
        logger.info { "Executing running match job for ${match.homeTeam?.name} vs ${match.awayTeam?.name}" }

        val scheduler = context.scheduler
        val refreshJob = JobBuilder.newJob(RefreshRunningMatchJob::class.java)
            .withIdentity("refresh-running-match-${match.matchId}")
            .usingJobData(context.jobDetail.jobDataMap)
            .build()

        val intervalMin = 2
        val jobDurationMin = 45 + 15 + 45 + 20
        val trigger = TriggerBuilder.newTrigger()
            .withSchedule(
                SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInMinutes(2)
                    .withRepeatCount(jobDurationMin / intervalMin)
            )
            .build()
        scheduler.scheduleJob(refreshJob, trigger)
    }
}

class RefreshRunningMatchJob(
    private val json: JsonMapper,
) : Job {

    private val logger = KotlinLogging.logger {  }
    override fun execute(context: JobExecutionContext) {
        val match = json.readValue<OpenLigaDbClient.OpenLigaDbMatch>(context.jobDetail.jobDataMap["match"] as String)
        logger.info { "Executing refresh running match job for ${match.homeTeam?.name} vs ${match.awayTeam?.name}" }
    }
}