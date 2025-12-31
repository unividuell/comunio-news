package org.unividuell.news.comunio.matchday

import java.time.OffsetDateTime

data class MatchGroup(
    val groupOrderId: Int,
)

data class Match(
    val id: MatchId,
    val kickoffTime: OffsetDateTime,
) {
    data class MatchId(
        val season: Season,
        val matchdayOrderId: Int,
        val home: Club,
        val away: Club,
    ) {
        override fun toString(): String {
            return "${season.startYear.toString().takeLast(2)}/${season.endYear.toString().takeLast(2)}_${matchdayOrderId}_${home.shortName}-${away.shortName}"
        }
    }
    data class Season(
        val startYear: Int,
        val endYear: Int,
    )
    data class Club(
        val name: String,
        val shortName: String,
        val iconUrl: String,
    )
}