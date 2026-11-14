package io.github.pokhriyal_anmol.f1.data

fun careerKey(kind: Int, id: String) = (if (kind == KIND_DRIVER) "d:" else "t:") + id
fun careerScope(kind: Int) = if (kind == KIND_DRIVER) "drivers" else "constructors"

/** Seasons are a comma list in the cache. Newest first. */
fun careerSeasons(c: CareerEntity?): List<Int> =
    c?.seasons?.split(",")?.mapNotNull { it.toIntOrNull() }?.sortedDescending().orEmpty()

/**
 * Past seasons whose final table is not saved yet. The current season is live data, so it is never asked for,
 * and a team has no table before 1958.
 */
fun missingSeasons(kind: Int, career: List<Int>, have: Set<Int>, current: Int?): List<Int> =
    career.filter { it != current && it !in have && (kind == KIND_DRIVER || it >= FIRST_CONSTRUCTORS_SEASON) }

/** Championships won, counted from the final tables. Only meaningful once every past season is saved. */
fun titles(history: List<ArchiveStandingEntity>): Int = history.count { it.position == 1 }

/** One driver against one teammate over the rounds they shared a team. */
data class HeadToHead(val teammateId: String, val teammate: String, val rounds: Int,
    val racesAhead: Int, val racesBehind: Int, val gridAhead: Int, val gridBehind: Int)

private fun classified(r: RaceResultEntity) = r.positionText.toIntOrNull() != null

/**
 * Race: the classified finisher beats a retirement, and two retirements settle nothing. Grid is the starting
 * slot (after penalties), a stand-in for qualifying, and a pit-lane start (0) is left out.
 * The teammate is whoever shared a team with the driver in the most rounds, the latest on a tie.
 */
fun headToHead(rows: List<RaceResultEntity>, driverId: String): HeadToHead? {
    val mine = rows.filter { it.driverId == driverId }
    val pairs = mine.flatMap { m -> rows.filter { it.round == m.round && it.teamId == m.teamId && it.driverId != driverId }.map { m to it } }
    val mate = pairs.groupBy { it.second.driverId }.entries
        .maxWithOrNull(compareBy({ it.value.size }, { it.value.maxOf { p -> p.first.round } }))?.value ?: return null
    var ahead = 0; var behind = 0; var gAhead = 0; var gBehind = 0
    for ((a, b) in mate) {
        when {
            classified(a) && classified(b) -> if (a.position < b.position) ahead++ else behind++
            classified(a) -> ahead++
            classified(b) -> behind++
        }
        if (a.grid > 0 && b.grid > 0 && a.grid != b.grid) if (a.grid < b.grid) gAhead++ else gBehind++
    }
    val other = mate.first().second
    return HeadToHead(other.driverId, other.driverName, mate.size, ahead, behind, gAhead, gBehind)
}

/** A team's points per round, summed over whoever drove for it that round. One entry per round, zero if absent. */
fun teamPointsByRound(rows: List<RaceResultEntity>, teamId: String, rounds: Int): List<Int> {
    val byRound = rows.filter { it.teamId == teamId }.groupBy { it.round }.mapValues { e -> e.value.sumOf { it.points } }
    return (1..rounds).map { (byRound[it] ?: 0.0).toInt() }
}

/** The driver's newest finishes first. */
fun lastResults(rows: List<RaceResultEntity>, driverId: String, n: Int = 5): List<RaceResultEntity> =
    rows.filter { it.driverId == driverId }.sortedByDescending { it.round }.take(n)
