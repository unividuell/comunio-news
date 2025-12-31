package org.unividuell.news.comunio.lineup.repository

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.unividuell.news.comunio.lineup.MatchLineupOutput

@Repository
interface LineupRepository : CrudRepository<LineupEntity, Int> {

    fun findByIdAndHash(id: Int, hash: String): LineupEntity?

}

@Table("LINEUPS")
data class LineupEntity(
    @Id val id: Int,
    @Version val version: Int? = null,
    val hash: String?,
    val json: MatchLineupOutput.LineupOutput,
)
