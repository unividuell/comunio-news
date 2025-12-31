package org.unividuell.news.comunio.openligadb

import io.github.oshai.kotlinlogging.KotlinLogging
import org.quartz.*
import org.springframework.context.ApplicationEventPublisher
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionalEventListener
import org.unividuell.news.comunio.matchday.Match
import org.unividuell.news.comunio.matchday.MatchdayService
import org.unividuell.news.comunio.matchday.OpenLigaDbFetched
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import java.time.Instant

@Service
class OpenLigaDbScheduler(
    private val matchdayService: MatchdayService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val scheduler: Scheduler,
    private val json: JsonMapper,
) {

    private val logger = KotlinLogging.logger {  }

    fun boot() {
        matchdayService.updateMatchdaysOfSeason()
    }

    @Scheduled(cron = "0 0 8 * * *")
    fun updateMatches() {
        logger.info { "Updating OpenLigaDb matches" }
        matchdayService.updateMatchdaysOfSeason()
    }

    data class CurrentMatchGroup(val openLigaGroupOrderId: Int, val comunioGamedayId: Int, val matchIds: List<Int>)

    @ApplicationModuleListener
    fun on(event: OpenLigaDbFetched) {
        logger.info { "Consuming OpenLigaDbFetched event $event" }
        matchdayService
            .currentMatchGroup(relativeTo = Instant.now())
            ?.let {
                applicationEventPublisher
                    .publishEvent(CurrentMatchGroup(
                        openLigaGroupOrderId = it.openLigaGroupOrderId,
                        comunioGamedayId = it.comunioGamedayId,
                        matchIds = it.comunioMatchIds
                    ))
            }
            ?: run { logger.warn { "No current match group found" } }
    }

    @Async
    @TransactionalEventListener
    fun on(event: CurrentMatchGroup) {
        logger.info { "Consuming CurrentMatchGroup event $event" }
        matchdayService
            .matchesByGroupOrderId(event.openLigaGroupOrderId)
            ?.also { logger.info { "Found ${it.size} matches for group ${event.openLigaGroupOrderId}" } }
            ?.forEach { match ->
                scheduleMatchStartJob(executionTime = match.kickoffTime.toInstant(), match = match)
            }
    }

    fun scheduleMatchStartJob(executionTime: Instant, match: Match) {
        logger.info { "Scheduling match start job for $executionTime for match ${match.id.home.name} vs ${match.id.away.name}" }

        val jobDetail = JobBuilder.newJob(RunningMatchJob::class.java)
            .withIdentity("match-${match.id}")
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
        val match = json.readValue<Match>(context.jobDetail.jobDataMap["match"] as String)
        logger.info { "Executing running match job for ${match.id.home.shortName} vs ${match.id.away.shortName}" }

        val scheduler = context.scheduler
        val refreshJob = JobBuilder.newJob(RefreshRunningMatchJob::class.java)
            .withIdentity("refresh-running-match-${match.id}")
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
        val match = json.readValue<Match>(context.jobDetail.jobDataMap["match"] as String)
        logger.info { "Executing refresh running match job for ${match.id.home.shortName} vs ${match.id.away.shortName}" }
    }
}