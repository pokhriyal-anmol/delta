package dev.anpo.f1.learn

private val RACE_FULL = intArrayOf(25, 18, 15, 12, 10, 8, 6, 4, 2, 1)
private val RACE_75 = intArrayOf(19, 14, 12, 10, 8, 6, 4, 3, 2, 1)
private val RACE_50 = intArrayOf(13, 10, 8, 6, 5, 4, 3, 2, 1)
private val RACE_25 = intArrayOf(6, 4, 3, 2, 1)
private val SPRINT = intArrayOf(8, 7, 6, 5, 4, 3, 2, 1)

// distanceShare is the leader's share of the race distance. The two-lap floor (FIA-A A2.2.1)
// is the caller's job: pass 0.0 (or less) when the leader did not complete two laps, and nothing scores.
fun racePoints(position: Int, distanceShare: Double): Int {
    val table = when {
        distanceShare <= 0.0 -> return 0
        distanceShare < 0.25 -> RACE_25
        distanceShare < 0.5 -> RACE_50
        distanceShare < 0.75 -> RACE_75
        else -> RACE_FULL
    }
    return table.getOrElse(position - 1) { 0 }
}

// A sprint scores only once half the distance is done (FIA-A A2.2.2).
fun sprintPoints(position: Int, distanceShare: Double): Int =
    if (distanceShare < 0.5) 0 else SPRINT.getOrElse(position - 1) { 0 }

/** Points on today's 25-18-15-12-10-8-6-4-2-1 table for finishing position texts. R, D, W and anything outside the top ten score zero. */
fun rescore(positions: List<String>): Int =
    positions.sumOf { p -> p.toIntOrNull()?.let { RACE_FULL.getOrNull(it - 1) } ?: 0 }
