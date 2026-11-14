package io.github.pokhriyal_anmol.f1.data

import java.util.Locale

const val KIND_DRIVER = 0
const val KIND_TEAM = 1
const val FIRST_SEASON = 1950
/** Measured 2026-11-14: Jolpica returns no constructors' table for 1957 and one from 1958. */
const val FIRST_CONSTRUCTORS_SEASON = 1958

/** Whole numbers without a decimal point ("30"), shared drives with one ("4.5"). */
fun formatPoints(p: Double): String =
    if (p % 1.0 == 0.0) p.toLong().toString() else String.format(Locale.ENGLISH, "%.1f", p)

/**
 * A season is over when its standings are after the last round and that round has been run.
 * The owner chose to take the champion from final standings, never to compute a clinch.
 */
fun isSeasonFinal(standingsRound: Int?, rounds: Int, lastRaceMs: Long?, nowMs: Long): Boolean =
    standingsRound != null && rounds > 0 && standingsRound == rounds && lastRaceMs != null && lastRaceMs < nowMs

/**
 * Ranked drivers only. Historical tables list every entrant, and the ones who never scored come back
 * with positionText "-" and no `position` field at all (59 of 81 rows in 1950).
 */
fun driverArchive(list: StandingsList, now: Long): List<ArchiveStandingEntity> =
    list.drivers.mapNotNull { d ->
        val pos = d.position?.toIntOrNull() ?: return@mapNotNull null
        val team = d.teams.firstOrNull()
        ArchiveStandingEntity(list.season, KIND_DRIVER, d.driver.driverId, pos, d.points.toDoubleOrNull() ?: 0.0,
            d.wins.toIntOrNull() ?: 0, d.driver.familyName, team?.name.orEmpty(), team?.constructorId.orEmpty(), now)
    }

fun teamArchive(list: StandingsList, now: Long): List<ArchiveStandingEntity> =
    list.constructors.mapNotNull { c ->
        val pos = c.position?.toIntOrNull() ?: return@mapNotNull null
        ArchiveStandingEntity(list.season, KIND_TEAM, c.team.constructorId, pos, c.points.toDoubleOrNull() ?: 0.0,
            c.wins.toIntOrNull() ?: 0, c.team.name, "", c.team.constructorId, now)
    }
