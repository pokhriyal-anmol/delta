package dev.anpo.f1.learn

import org.junit.Assert.assertEquals
import org.junit.Test

class KnockoutTest {
    @Test fun twentyCars() = assertEquals(listOf(5, 5), knockout(20))
    @Test fun twentyTwoCars() = assertEquals(listOf(6, 6), knockout(22))
    @Test fun twentyFourCars() = assertEquals(listOf(7, 7), knockout(24))
}
