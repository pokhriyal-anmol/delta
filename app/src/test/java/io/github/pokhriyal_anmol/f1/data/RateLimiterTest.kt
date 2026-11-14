package io.github.pokhriyal_anmol.f1.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RateLimiterTest {
    private val hour = 3_600_000L

    @Test fun tokensRefillWithTimeUpToCapacity() {
        assertEquals(110.0, refilled(10.0, 0L, hour / 2, 200, 200), 1e-9)
        assertEquals(200.0, refilled(10.0, 0L, 5 * hour, 200, 200), 1e-9)
    }

    /** The bug: last = Dec 7, now = Nov 14. Used to give about -109,000 tokens and block everything. */
    @Test fun aClockSetBackwardsNeverTakesTokensAway() {
        val dec7 = 1_796_625_001_366L
        val nov14 = 1_794_659_568_000L
        assertEquals(197.0, refilled(197.0, dec7, nov14, 200, 200), 1e-9)
    }
}
