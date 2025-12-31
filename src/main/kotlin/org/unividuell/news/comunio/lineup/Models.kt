package org.unividuell.news.comunio.lineup

data class MatchLineupOutput(
    val comunioGamedayId: Long,
    val matches: List<LineupOutput>,
) {
    data class LineupOutput(
        val matchId: Int,
        val homeClub: ComunioClub?,
        val awayClub: ComunioClub?,
        val state: ComunioClub.MatchState,
    ) {
        data class ComunioClub(
            val cid: Int,
            val lineup: ClubLineup,
        ) {
            data class ClubLineup(
                val details: LineupDetails,
                val players: List<ComunioFootballPlayer>,
            ) {
                enum class LineupDetails(val id: Int) {
                    Expected(1),
                    Playing(2),
                    Over(3),
                    Unknow(-1);

                    companion object {
                        fun byId(id: Int): LineupDetails {
                            return entries.firstOrNull { it.id == id } ?: Unknow
                        }
                    }
                }

                data class ComunioFootballPlayer(
                    val pid: Long,
                    val name: String,
                    val position: Position,
                    val goals: Int,
                    val penaltyGoals: Int,
                    val points: Int?,
                    val active: Active,
                    val substitutedInAtMin: Int?,
                    val substitutedOutAtMin: Int?,
                ) {
                    enum class Position(val id: String) {
                        // "t", "a", "m", "s"
                        Goalkeeper("t"),
                        Defender("a"),
                        Midfielder("m"),
                        Forward("s");

                        companion object {
                            fun byId(id: String?): Position {
                                return entries.first { it.id == id }
                            }
                        }
                    }

                    enum class Active(val id: Int) {
                        ProperlyActive(-1),
                        Active(1),
                        Bench(-2),
                        Unknow(-1000);

                        companion object {
                            fun byId(id: Int): Active = entries.firstOrNull { it.id == id } ?: Unknow
                        }
                    }
                }
            }

            enum class MatchState(val id: String) {
                FirstHalf("1st"),
                HalfTime("HT"),
                SecondHalf("2nd"),
                FullTime("FT"),
                Unknow("");

                companion object {
                    fun byId(id: String?): MatchState {
                        return entries.firstOrNull { it.id == id } ?: Unknow
                    }
                }
            }
        }
    }
}