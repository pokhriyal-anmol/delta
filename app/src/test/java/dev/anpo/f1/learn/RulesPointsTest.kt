package dev.anpo.f1.learn

import org.junit.Assert.assertEquals
import org.junit.Test

// Expected values are typed from the FIA-A A2.2.1 table text (content.md lesson 05), not read from Rules.kt.
class RulesPointsTest {
    private val full = listOf(25, 18, 15, 12, 10, 8, 6, 4, 2, 1)

    // Each row: share inside the column, then points for positions 1..10 (0 where nothing is paid).
    private val columns = listOf(
        0.10 to listOf(6, 4, 3, 2, 1, 0, 0, 0, 0, 0),
        0.30 to listOf(13, 10, 8, 6, 5, 4, 3, 2, 1, 0),
        0.60 to listOf(19, 14, 12, 10, 8, 6, 4, 3, 2, 1),
        0.90 to full,
        1.00 to full,
    )

    @Test fun everyCellOfTheCutShortTable() {
        for ((share, row) in columns) {
            row.forEachIndexed { i, pts -> assertEquals("p${i + 1} at $share", pts, racePoints(i + 1, share)) }
            assertEquals("p11 at $share", 0, racePoints(11, share))
        }
    }

    @Test fun boundariesLandInTheColumnTheFiaTextNames() {
        assertEquals(13, racePoints(1, 0.25)) // 25% to 50% starts at 25
        assertEquals(6, racePoints(1, 0.2499))
        assertEquals(19, racePoints(1, 0.5)) // 50% to 75% starts at 50
        assertEquals(13, racePoints(1, 0.4999))
        assertEquals(25, racePoints(1, 0.75)) // 75% or more starts at 75
        assertEquals(19, racePoints(1, 0.7499))
    }

    @Test fun twoLapFloorIsAShareOfZero() {
        assertEquals(0, racePoints(1, 0.0))
    }

    @Test fun sprintPaysEightToOneOnlyFromHalfDistance() {
        listOf(8, 7, 6, 5, 4, 3, 2, 1).forEachIndexed { i, pts -> assertEquals(pts, sprintPoints(i + 1, 0.5)) }
        assertEquals(0, sprintPoints(9, 1.0))
        assertEquals(0, sprintPoints(1, 0.4999))
        assertEquals(8, sprintPoints(1, 1.0))
    }
}
