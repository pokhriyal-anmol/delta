package dev.anpo.f1.learn

import org.junit.Assert.assertEquals
import org.junit.Test

class GridAfterPenaltyTest {
    private val field = 22

    @Test fun fivePlacesFromThirteenthIsEighteenth() = assertEquals(18, gridAfterPenalty(13, 5, field))
    @Test fun fifteenPlacesFromThirteenthIsLast() = assertEquals(22, gridAfterPenalty(13, 15, field))
    @Test fun sixteenPlacesFromThirteenthIsLast() = assertEquals(22, gridAfterPenalty(13, 16, field))
    @Test fun eightyPlacesFromThirteenthIsLast() = assertEquals(22, gridAfterPenalty(13, 80, field))
    @Test fun threePlacesFromFifthIsEighth() = assertEquals(8, gridAfterPenalty(5, 3, field))

    // 16 places from 2nd must go to last; 15 places from 2nd is 17th. Separates the two branches.
    @Test fun fifteenAndSixteenDiffer() {
        assertEquals(17, gridAfterPenalty(2, 15, field))
        assertEquals(22, gridAfterPenalty(2, 16, field))
    }
}
