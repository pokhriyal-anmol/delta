package dev.anpo.f1.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeekendTest {
    private fun race(
        fp1: Long? = null, fp2: Long? = null, fp3: Long? = null,
        quali: Long? = null, sprintQuali: Long? = null, sprint: Long? = null,
    ) = RaceEntity("2026-21", "2026", 21, "Las Vegas Grand Prix", "2026-11-22", "04:00:00Z",
        "vegas", "Las Vegas Strip Street Circuit", "Las Vegas", "USA",
        fp1, fp2, fp3, quali, sprintQuali, sprint, 0L)

    private fun winner(laps: String?) = JRace("2026", "1", "x", "2026-01-01", null,
        JCircuit("c", "c", JLocation()),
        results = listOf(JResult("1", "1", "25", "1", null, laps, JDriver("d", givenName = "a", familyName = "b"))))

    @Test fun squareOutlineIsItsPerimeter() {
        val km = trackLengthKm(listOf(0f, 1000f, 1000f, 0f), listOf(0f, 0f, 1000f, 1000f))
        assertEquals(4000.0 / 9930.0, km, 1e-9)
    }

    @Test fun degenerateOutlinesHaveNoLength() {
        assertEquals(0.0, trackLengthKm(emptyList(), emptyList()), 0.0)
        assertEquals(0.0, trackLengthKm(listOf(1f, 2f), listOf(1f)), 0.0)
    }

    /** Known answer: Spa is officially 7.004 km. The real MultiViewer outline must land within 1%. */
    @Test fun spaOutlineMatchesTheOfficialLength() {
        val text = javaClass.getResourceAsStream("/spa-outline.json")!!.bufferedReader().readText()
        val c = Json { ignoreUnknownKeys = true }.decodeFromString(MvCircuit.serializer(), text)
        assertEquals(7.004, trackLengthKm(c.x, c.y), 0.07)
    }

    @Test fun sessionsComeEarliestFirstAndOnlyTheRaceIsFlagged() {
        val s = weekendSessions(race(fp1 = 100, fp2 = 200, fp3 = 300, quali = 400))
        assertEquals(listOf("Practice 1", "Practice 2", "Practice 3", "Qualifying", "Race"), s.map { it.name })
        assertEquals(listOf(false, false, false, false, true), s.map { it.isRace })
    }

    @Test fun sprintWeekendInterleavesItsExtraSessions() {
        val s = weekendSessions(race(fp1 = 100, sprintQuali = 200, sprint = 300, quali = 400))
        assertEquals(listOf("Practice 1", "Sprint Qualifying", "Sprint", "Qualifying", "Race"), s.map { it.name })
    }

    /** Order is by start time, not by session type: an older format ran Qualifying before the Sprint and before Practice. */
    @Test fun orderFollowsTheClockNotTheSessionType() {
        val s = weekendSessions(race(fp1 = 200, quali = 100))
        assertEquals(listOf("Qualifying", "Practice 1", "Race"), s.map { it.name })
    }

    @Test fun missingSessionsAreLeftOutRatherThanInvented() {
        assertEquals(listOf("Race"), weekendSessions(race()).map { it.name })
    }

    @Test fun utcTimesParseAndABadOneIsNull() {
        assertEquals(1795320000000L, utcMillis("2026-11-22", "04:00:00Z"))
        assertEquals(utcMillis("2026-11-22", "12:00:00Z"), utcMillis("2026-11-22", null))
        assertNull(utcMillis("not-a-date", "04:00:00Z"))
    }

    @Test fun lapsAreTheLargestOfTheLastFiveWinnersSoShortenedRacesDoNotCount() {
        assertEquals(53, scheduledLaps(listOf(winner("28"), winner("53"), winner("53"), winner("51"), winner("53"))))
        assertEquals(44, scheduledLaps(listOf(winner("3"), winner("44"))))
    }

    @Test fun noKnownLapsIsNullNotZero() {
        assertNull(scheduledLaps(emptyList()))
        assertNull(scheduledLaps(listOf(winner(null), winner("n/a"))))
        assertTrue(scheduledLaps(listOf(winner("57"))) == 57)
    }
}
