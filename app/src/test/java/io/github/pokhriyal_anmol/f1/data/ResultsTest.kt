package io.github.pokhriyal_anmol.f1.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
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
}
