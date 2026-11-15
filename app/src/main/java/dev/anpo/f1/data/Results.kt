package dev.anpo.f1.data

import java.time.LocalDate

/** One row per finisher. Race results arrive inside the season download the app already makes. */
fun resultRows(race: JRace): List<RaceResultEntity> {
    val round = race.round.toIntOrNull() ?: return emptyList()
    return race.results.map { r ->
        RaceResultEntity(
            race.season, round, r.driver.driverId,
            position = r.position.toIntOrNull() ?: 99, positionText = r.positionText,
            driverName = "${r.driver.givenName} ${r.driver.familyName}", code = r.driver.code,
            teamId = r.team?.constructorId.orEmpty(), teamName = r.team?.name.orEmpty(),
            grid = r.grid.toIntOrNull() ?: 0, laps = r.laps?.toIntOrNull() ?: 0,
            status = r.status.orEmpty(), time = r.time?.time, points = r.points.toDoubleOrNull() ?: 0.0)
    }
}

/** Each race at a circuit has one winner row (the request asks for position 1 only). */
fun winnerRows(circuitId: String, races: List<JRace>): List<CircuitWinnerEntity> =
    races.mapNotNull { race ->
        val w = race.results.firstOrNull() ?: return@mapNotNull null
        CircuitWinnerEntity(circuitId, race.season, race.round.toIntOrNull() ?: 0, race.raceName,
            w.driver.driverId, "${w.driver.givenName} ${w.driver.familyName}", w.team?.name.orEmpty(),
            w.laps?.toIntOrNull() ?: 0)
    }

/** The driver with the most wins at a circuit and how many, or null when nobody has won there. */
fun mostWins(winners: List<CircuitWinnerEntity>): Pair<String, Int>? =
    winners.groupBy { it.driverId }.maxByOrNull { it.value.size }?.value?.let { it.first().driverName to it.size }

/**
 * What the time column shows. A gap is only meaningful on the lead lap: Jolpica also gives lapped drivers a
 * time gap (+27.120 for a car two laps down), so those show laps down instead. No time means the driver
 * stopped, and the status says why.
 */
fun finishText(r: RaceResultEntity, leaderLaps: Int): String {
    val down = leaderLaps - r.laps
    return when {
        r.time == null -> r.status
        r.laps > 0 && down > 0 -> "+$down ${if (down == 1) "Lap" else "Laps"}"
        else -> r.time
    }
}

/** More missing rounds than this and the whole season is paged instead: 5 requests beat one per round. */
internal const val BULK_FROM = 4

/** Rounds whose race date is before today (UTC), so their results should exist. */
internal fun finishedRounds(races: List<RaceEntity>, today: LocalDate): List<Int> =
    races.filter { runCatching { LocalDate.parse(it.date) < today }.getOrDefault(false) }.map { it.round }

/** Finished rounds with no stored results. A finished round never changes, so these are the only ones worth asking for. */
internal fun missingRounds(finished: List<Int>, stored: Set<Int>): List<Int> = finished.filter { it !in stored }

/** Podiums, poles, DNFs and points by round per driver, derived from stored rows (a numeric positionText is a classified finish). */
internal fun seasonStats(rows: List<RaceResultEntity>, now: Long): List<DriverSeasonStat> {
    val season = rows.firstOrNull()?.season ?: return emptyList()
    val maxRound = rows.maxOf { it.round }
    return rows.groupBy { it.driverId }.map { (id, mine) ->
        val byRound = mine.associate { it.round to it.points.toInt() }
        DriverSeasonStat(id, season,
            podiums = mine.count { (it.positionText.toIntOrNull() ?: 99) <= 3 },
            poles = mine.count { it.grid == 1 },
            dnfs = mine.count { it.positionText.toIntOrNull() == null },
            roundPoints = (1..maxRound).joinToString(",") { (byRound[it] ?: 0).toString() }, fetchedAt = now)
    }
}
