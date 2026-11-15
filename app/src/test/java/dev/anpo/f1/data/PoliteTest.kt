package dev.anpo.f1.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PoliteTest {
    @Test fun retryAfterIsSecondsCappedAndNothingElse() {
        assertEquals(5_000L, retryAfterMs("5"))
        assertEquals(5_000L, retryAfterMs(" 5 "))
        assertEquals(10_000L, retryAfterMs("120"))
        assertEquals(0L, retryAfterMs("0"))
        assertNull(retryAfterMs("-1"))
        assertNull(retryAfterMs("Wed, 21 Oct 2026 07:28:00 GMT"))
        assertNull(retryAfterMs(null))
    }

    @Test fun userAgentNamesTheAppAndWhereToFindIt() {
        assertEquals(true, USER_AGENT.startsWith("Delta/") && USER_AGENT.contains("github.com/pokhriyal-anmol/delta"))
    }

    @Test fun theFeedGivesTheNewestPlainTagAndNothingElse() {
        val atom = """<feed><link rel="alternate" href="https://github.com/pokhriyal-anmol/delta/releases"/>
            <entry><link href="https://github.com/pokhriyal-anmol/delta/releases/tag/v1.4.0"/></entry>
            <entry><link href="https://github.com/pokhriyal-anmol/delta/releases/tag/v1.3.2"/></entry></feed>"""
        assertEquals("v1.4.0", latestTag(atom))
        assertNull(latestTag("<feed/>"))
        assertNull(latestTag("""<a href="/releases/tag/evil.example/x"/>"""))
    }

    @Test fun onlyAHigherVersionIsNewer() {
        assertEquals(true, isNewer("v1.4.0", "1.3.2"))
        assertEquals(true, isNewer("v1.3.10", "1.3.9"))
        assertEquals(true, isNewer("v2.0", "1.9.9"))
        assertEquals(false, isNewer("v1.3.2", "1.3.2"))
        assertEquals(false, isNewer("v1.3.2", "1.4.0"))
        assertEquals(false, isNewer("v1.3.2", "1.3.2-debug"))
        assertEquals(false, isNewer("vx", "1.3.2"))
    }
}
