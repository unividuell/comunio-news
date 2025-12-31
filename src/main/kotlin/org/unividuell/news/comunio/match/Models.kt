package org.unividuell.news.comunio.match

data class AiClub(
    val name: String,
    val lineup: List<Player>,
)

data class Player(
    val name: String,
    val member: String?,
    val usedByMember: Boolean?,
    val position: String,
    val goals: Int,
    val penaltyGoals: Int,
    val points: Int?,
    val clubLineupStatus: ClubLineupStatus,
    val substitutedInAtMin: Int?,
    val substitutedOutAtMin: Int?,
) {
    enum class ClubLineupStatus {
        StartOnField,
        StartOnBench,
        NotInSquad,
    }
}

data class MatchComposerOutput(
    val groupOrderId: Int,
    val matchId: Int,
    val homeClub: AiClub,
    val awayClub: AiClub,
) {
}