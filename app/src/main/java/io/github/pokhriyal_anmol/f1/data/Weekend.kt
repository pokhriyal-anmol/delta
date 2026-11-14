package io.github.pokhriyal_anmol.f1.data

import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.hypot

/**
 * MultiViewer's outline is in units of about a tenth of a metre. Measured on 2026-11-14 against the
 * official lengths of Spa, Monza, Silverstone, Suzuka, Monaco and Interlagos: 9,800 to 9,950 units
 * per km, because the points smooth the corners slightly. 9,930 keeps the result within about 1.3%.
 * shortcut: a derived length is approximate, so the UI shows one decimal. Pin official lengths if
 * 0.1 km ever matters.
 */
private const val UNITS_PER_KM = 9930.0

/** Length of the closed outline, in km. Use the full-resolution points: decimating cuts corners. */
fun trackLengthKm(xs: List<Float>, ys: List<Float>): Double {
    if (xs.size < 2 || xs.size != ys.size) return 0.0
    var units = hypot((xs.first() - xs.last()).toDouble(), (ys.first() - ys.last()).toDouble())
    for (i in 0 until xs.lastIndex) units += hypot((xs[i + 1] - xs[i]).toDouble(), (ys[i + 1] - ys[i]).toDouble())
    return units / UNITS_PER_KM
}

/** "2026-11-22" + "04:00:00Z" to epoch millis. A missing time reads as noon UTC, as the calendar always has. */
fun utcMillis(date: String, time: String?): Long? = runCatching {
    LocalDateTime.parse("${date}T${(time ?: "12:00:00Z").removeSuffix("Z")}").toInstant(ZoneOffset.UTC).toEpochMilli()
}.getOrNull()

data class WeekendSession(val name: String, val startMs: Long, val isRace: Boolean)

/** Every session the calendar lists for the weekend, earliest first. Sprint weekends list their extra sessions. */
fun weekendSessions(r: RaceEntity): List<WeekendSession> = buildList {
    r.fp1?.let { add(WeekendSession("Practice 1", it, false)) }
    r.fp2?.let { add(WeekendSession("Practice 2", it, false)) }
    r.fp3?.let { add(WeekendSession("Practice 3", it, false)) }
    r.sprintQuali?.let { add(WeekendSession("Sprint Qualifying", it, false)) }
    r.sprint?.let { add(WeekendSession("Sprint", it, false)) }
    r.quali?.let { add(WeekendSession("Qualifying", it, false)) }
    utcMillis(r.date, r.time)?.let { add(WeekendSession("Race", it, true)) }
}.sortedBy { it.startMs }

/**
 * Scheduled laps from past winners at a circuit. Takes the largest of the last five, because a red
 * flag can shorten a race (Suzuka once ran 28 laps) but nothing makes one longer than scheduled.
 * Measured: the 305 km rule applied to the derived length matched only 16 of 21 circuits.
 * shortcut: five shortened races in a row would under-report. Unlikely, revisit if it shows up.
 */
fun scheduledLaps(winners: List<JRace>): Int? =
    winners.takeLast(5).mapNotNull { it.results.firstOrNull()?.laps?.toIntOrNull() }.maxOrNull()
