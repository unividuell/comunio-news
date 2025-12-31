package org.unividuell.news.comunio.lineup

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.unividuell.news.comunio.lineup.client.MatchLineupClient
import org.unividuell.news.comunio.lineup.client.MemberLineupClient
import org.unividuell.news.comunio.lineup.repository.LineupEntity
import org.unividuell.news.comunio.lineup.repository.LineupRepository
import org.unividuell.news.comunio.matchday.MatchGroup
import org.unividuell.news.comunio.toSha256
import tools.jackson.databind.json.JsonMapper

@Service
class LineupService(
    private val matchLineupClient: MatchLineupClient,
    private val memberLineupClient: MemberLineupClient,
    private val lineupRepository: LineupRepository,
    private val json: JsonMapper,
) {

    private val logger = KotlinLogging.logger {  }

    @Transactional
    fun scrapeMatches(matchGroup: MatchGroup): MatchLineupOutput {
        val matches = matchGroup.comunioMatchIds.map {
            matchLineupClient.scrapeMatchLineup(matchId = it)
        }
        matches.forEach { client ->
            val hash = json.writeValueAsString(client).toSha256()
            val contentExists = lineupRepository.findByIdAndHash(id = client.matchId, hash = hash)
            if (contentExists != null) {
                logger.debug { "Lineup for match ${client.matchId} unchanged. Skipping." }
                return@forEach
            }

            val entityExists = lineupRepository.findByIdOrNull(client.matchId)
            if (entityExists != null) {
                logger.debug { "Lineup for match ${client.matchId} already exists but has changed. Updating." }
                lineupRepository.save(entityExists.copy(hash = hash, json = client))
            } else {
                logger.debug { "Lineup for match ${client.matchId} does not exist yet. Creating." }
                lineupRepository.save(LineupEntity(id = client.matchId, hash = hash, json = client))
            }
        }
        return MatchLineupOutput(comunioGamedayId = matchGroup.comunioGamedayId, matches = matches)
    }

    @Transactional(readOnly = true)
    fun getLineup(matchId: Int): MatchLineupOutput.LineupOutput? {
        return lineupRepository.findByIdOrNull(matchId)?.json
    }

    fun scrapeMembers(comunioGamedayId: Int): MemberLineupOutput {
        return memberLineupClient.scrape(comunioGamedayId = comunioGamedayId)
    }

}