package dev.anpo.f1.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveTest {
    private fun list(resource: String): StandingsList {
        val text = javaClass.getResourceAsStream("/$resource.json")!!.bufferedReader().readText()
        val env = Json { ignoreUnknownKeys = true; coerceInputValues = true }.decodeFromString(JolpicaEnvelope.serializer(), text)
        return env.data.standings!!.lists.first()
    }

    /** Real 1950 payload: 81 entrants, 59 of them with no `position` field. Must parse, and keep only the ranked ones. */
    @Test fun historicalDriverTableParsesAndDropsUnscoredEntrants() {
        val l = list("standings-1950-drivers")
        assertEquals(81, l.drivers.size)
        assertEquals(22, l.drivers.count { it.position != null })
        val rows = driverArchive(l, 0L)
        assertEquals(22, rows.size)
        assertEquals((1..22).toList(), rows.map { it.position })
    }

    /** Known answer: Giuseppe Farina won the first championship, on 30 points. */
    @Test fun firstChampionshipWasFarinaOnThirtyPoints() {
        val top = driverArchive(list("standings-1950-drivers"), 0L).first()
        assertEquals("farina", top.entityId)
        assertEquals(30.0, top.points, 0.0)
        assertEquals("1950", top.season)
    }

    /** Known answer: McLaren took the 1988 constructors' title on 199 points. Unranked teams are dropped. */
    @Test fun mclarenWonTheNineteenEightyEightConstructorsTitle() {
        val l = list("standings-1988-constructors")
        assertEquals(18, l.constructors.size)
        val rows = teamArchive(l, 0L)
        assertEquals(10, rows.size)
        assertEquals("mclaren", rows.first().entityId)
        assertEquals(199.0, rows.first().points, 0.0)
        assertTrue(rows.none { it.entityId == "larrousse" })
    }

    @Test fun pointsPrintWithoutADecimalPointUnlessSharedDrive() {
        assertEquals("30", formatPoints(30.0))
        assertEquals("4.5", formatPoints(4.5))
        assertEquals("0", formatPoints(0.0))
    }

    @Test fun aSeasonIsFinalOnlyAfterTheLastRoundHasRun() {
        val past = 1_000L; val now = 2_000L; val future = 3_000L
        assertTrue(isSeasonFinal(23, 23, past, now))
        assertFalse(isSeasonFinal(21, 23, future, now))
        assertFalse(isSeasonFinal(23, 23, future, now))   // standings updated but the clock says the race is ahead
        assertFalse(isSeasonFinal(null, 23, past, now))
        assertFalse(isSeasonFinal(23, 0, null, now))
    }
}
