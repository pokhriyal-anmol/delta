package io.github.pokhriyal_anmol.f1.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderTest {
    private val hour = 60 * 60 * 1000L

    @Test fun remindsOneHourBeforeLightsOut() = assertEquals(10 * hour, reminderTime(11 * hour, 0L))

    /** Inside the last hour the moment has gone, so there is nothing to schedule. */
    @Test fun nothingToScheduleOnceTheHourIsPast() {
        assertNull(reminderTime(11 * hour, 10 * hour + 1))
        assertNull(reminderTime(11 * hour, 10 * hour))   // exactly now is not in the future
    }

    @Test fun unknownStartMeansNoReminder() = assertNull(reminderTime(null, 0L))
}
