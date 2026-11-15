package dev.anpo.f1.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.time.LocalDate
import org.junit.Assert.assertNull
import org.junit.Test

class ResultsTest {
    private fun races(resource: String): List<JRace> {
        val text = javaClass.getResourceAsStream("/$resource.json")!!.bufferedReader().readText()
        return Json { ignoreUnknownKeys = true; coerceInputValues = true }
            .decodeFromString(JolpicaEnvelope.serializer(), text).data.races!!.races
    }

    /** Real 2026 round 20 (Brazil): 22 rows, Norris wins from pole in a McLaren, last row withdrew. */
    @Test fun brazilClassificationKeepsEveryRowInOrder() {
        val rows = resultRows(races("results-2026-r20").single())
        assertEquals(22, rows.size)
        val win = rows.first()
        assertEquals("norris", win.driverId); assertEquals("mclaren", win.teamId)
        assertEquals(1, win.position); assertEquals(1, win.grid); assertEquals(25.0, win.points, 0.0)
        val lead = rows.maxOf { it.laps }
        assertEquals(71, lead)
        assertEquals("1:35:23.504", finishText(win, lead))
        val last = rows.last()
        assertEquals(22, last.position); assertEquals("W", last.positionText)   // retirements keep a numeric position; the label is positionText
        assertEquals("bottas", last.driverId)
        assertNull(last.time)
        assertEquals(last.status, finishText(last, lead))
        // Perez finished 69 of 71 laps but Jolpica still gives him "+27.120"; that must not be shown as a gap.
        assertEquals("+2 Laps", finishText(rows.first { it.driverId == "perez" }, lead))
        assertEquals("+1 Lap", finishText(rows.first { it.driverId == "bearman" }, lead))
        assertEquals("+56.475", finishText(rows.first { it.driverId == "piastri" }, lead))   // lead lap keeps its gap
    }

    /** Real Spa history: one winner per race, 59 rows. Schumacher's six is the record (Senna and Hamilton five). */
    @Test fun spaWinnersAreOnePerRaceAndSchumacherLeads() {
        val w = winnerRows("spa", races("winners-spa"))
        assertEquals(59, w.size)
        assertEquals(w.size, w.map { it.season }.toSet().size)
        assertEquals("Michael Schumacher" to 6, mostWins(w))
    }

    @Test fun noWinnersMeansNoMostWins() = assertNull(mostWins(emptyList()))

    private fun cal(vararg dates: String) = dates.mapIndexed { i, d ->
        RaceEntity("2026-${i + 1}", "2026", i + 1, "R${i + 1}", d, null, "c", "C", null, null, null, null, null, null, null, null, 0L)
    }

    /** A race counts as finished only once its date is before today, so a race day never asks for results that are not out yet. */
    @Test fun aRoundIsFinishedOnlyAfterItsDate() {
        val calendar = cal("2026-03-01", "2026-03-15", "2026-03-16", "not a date")
        assertEquals(listOf(1, 2), finishedRounds(calendar, LocalDate.parse("2026-03-16")))
        assertEquals(listOf(1, 2, 3), finishedRounds(calendar, LocalDate.parse("2026-03-17")))
    }

    @Test fun onlyFinishedRoundsWithNoStoredRowsAreMissing() {
        assertEquals(listOf(3), missingRounds(listOf(1, 2, 3), setOf(1, 2)))
        assertEquals(emptyList<Int>(), missingRounds(listOf(1, 2), setOf(1, 2, 9)))
    }

    private fun row(round: Int, driver: String, pos: String, grid: Int, pts: Double) = RaceResultEntity(
        "2026", round, driver, pos.toIntOrNull() ?: 99, pos, driver, null, "t", "T", grid, 50, "", null, pts)

    @Test fun statsCountPodiumsPolesDnfsAndKeepZeroForRoundsWithoutAResult() {
        val stats = seasonStats(listOf(
            row(1, "a", "1", 1, 25.0), row(1, "b", "R", 3, 0.0),
            row(2, "a", "4", 2, 12.0), row(2, "b", "3", 1, 15.0),
            row(3, "b", "2", 5, 18.0)), now = 7L).associateBy { it.driverId }
        val a = stats.getValue("a"); val b = stats.getValue("b")
        assertEquals(listOf(1, 1, 0), listOf(a.podiums, a.poles, a.dnfs))
        assertEquals("25,12,0", a.roundPoints)
        assertEquals(listOf(2, 1, 1), listOf(b.podiums, b.poles, b.dnfs))
        assertEquals("0,15,18", b.roundPoints)
        assertTrue(stats.values.all { it.season == "2026" && it.fetchedAt == 7L })
    }

    /** The old code derived these figures from the response in memory. Deriving them from stored rows must give the same numbers. */
    @Test fun statsFromStoredRowsMatchTheOldInMemoryDerivationOnRealRound20() {
        val race = races("results-2026-r20").single()
        val old = HashMap<String, IntArray>()
        race.results.forEach { r ->
            val a = old.getOrPut(r.driver.driverId) { IntArray(3) }
            val finish = r.positionText.toIntOrNull()
            if (finish != null && finish <= 3) a[0]++
            if (r.grid.toIntOrNull() == 1) a[1]++
            if (finish == null) a[2]++
        }
        val now = seasonStats(resultRows(race), 0L).associateBy { it.driverId }
        assertEquals(old.keys, now.keys)
        old.forEach { (id, a) -> assertEquals(a.toList(), listOf(now.getValue(id).podiums, now.getValue(id).poles, now.getValue(id).dnfs)) }
    }
}
