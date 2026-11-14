package dev.anpo.f1.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CareerTest {
    /** Real 2026 round 20 (Brazil) rows. */
    private val brazil: List<RaceResultEntity> by lazy {
        val text = javaClass.getResourceAsStream("/results-2026-r20.json")!!.bufferedReader().readText()
        resultRows(Json { ignoreUnknownKeys = true; coerceInputValues = true }
            .decodeFromString(JolpicaEnvelope.serializer(), text).data.races!!.races.single())
    }

    private fun inRounds(rounds: IntRange) = rounds.flatMap { r -> brazil.map { it.copy(round = r) } }

    @Test fun teammateHeadToHeadOnRealResults() {
        val h = headToHead(inRounds(18..20), "norris")!!
        assertEquals("piastri", h.teammateId)
        assertEquals(3, h.rounds)
        assertEquals(3, h.racesAhead); assertEquals(0, h.racesBehind)   // Norris P1, Piastri P6
        assertEquals(headToHead(inRounds(18..20), "piastri")!!.racesBehind, 3)
    }

    /** Stroll retired and Alonso finished: the classified driver is ahead. Bottas (W) and Cadillac teammate Perez finished 18th. */
    @Test fun classifiedFinisherBeatsRetirement() {
        val h = headToHead(brazil, "stroll")!!
        assertEquals("alonso", h.teammateId)
        assertEquals(0, h.racesAhead); assertEquals(1, h.racesBehind)
        val b = headToHead(brazil, "bottas")!!
        assertEquals(0, b.racesAhead); assertEquals(1, b.racesBehind)
    }

    @Test fun twoRetirementsSettleNothing() {
        val a = brazil.first { it.driverId == "stroll" }
        val b = a.copy(driverId = "alonso", positionText = "R", position = 20)
        val h = headToHead(listOf(a, b), "stroll")!!
        assertEquals(0, h.racesAhead + h.racesBehind)
    }

    @Test fun nobodyToCompareWithMeansNull() = assertNull(headToHead(brazil.filter { it.driverId == "norris" }, "norris"))

    @Test fun teamPointsAreSummedAcrossBothCars() {
        val pts = teamPointsByRound(brazil, "mclaren", 20)
        assertEquals(20, pts.size)
        assertEquals(25 + 8, pts.last())   // Norris won, Piastri finished sixth
        assertEquals(0, pts[0])
    }

    @Test fun lastResultsAreNewestFirst() {
        val l = lastResults(inRounds(14..20), "norris", 5)
        assertEquals(listOf(20, 19, 18, 17, 16), l.map { it.round })
    }

    private fun row(season: Int, pos: Int) = ArchiveStandingEntity("$season", KIND_DRIVER, "x", pos, 0.0, 0, "X", "", "", 0L)

    @Test fun titlesCountFirstPlacesOnly() = assertEquals(2, titles(listOf(row(2020, 1), row(2019, 2), row(2018, 1), row(2017, 3))))

    @Test fun missingSeasonsSkipsCurrentSavedAndPreConstructorsYears() {
        val career = listOf(2026, 2025, 2024, 1957, 1950)
        assertEquals(listOf(2024, 1957, 1950), missingSeasons(KIND_DRIVER, career, setOf(2025), 2026))
        assertEquals(listOf(2024), missingSeasons(KIND_TEAM, career, setOf(2025), 2026))
    }
}
