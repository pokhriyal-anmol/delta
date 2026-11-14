package dev.anpo.f1.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RedditTest {
    private fun posts() = parseReddit("formula1", javaClass.getResourceAsStream("/feeds/reddit-formula1-hot.xml")!!.bufferedReader().readText())

    /** Real r/formula1 hot feed saved 2026-11-14, trimmed to eight entries. */
    @Test fun realFeedBecomesPlainPostsInHotOrder() {
        val p = posts()
        assertEquals(8, p.size)
        assertEquals((0..7).toList(), p.map { it.rank })
        for (x in p) {
            assertEquals("r/formula1", x.source)
            assertTrue(x.url.startsWith("https://www.reddit.com/r/formula1/comments/"))
            assertTrue(x.title.isNotBlank() && !x.title.contains('<') && !x.snippet.contains('<') && !x.snippet.contains("&lt;"))
            assertTrue(x.snippet.length <= MAX_SNIPPET + 1)
            assertTrue(x.publishedAt != null)
        }
    }

    @Test fun textPostKeepsItsOpeningAndLinkPostKeepsNone() {
        val p = posts()
        assertTrue(p.any { it.snippet.startsWith("Welcome to the Day after Debrief") })
        assertTrue(p.any { it.snippet.isEmpty() })
    }

    @Test fun onlyRedditHttpsLinksSurvive() {
        fun entry(href: String) = """<entry><title>t $href</title><link href="$href"/><published>2026-11-14T10:00:00+00:00</published></entry>"""
        val xml = """<feed>${entry("https://evil.example/r/x")}${entry("javascript:alert(1)")}${entry("http://www.reddit.com/r/x")}${entry("https://www.reddit.com/r/formula1/comments/a/b/")}${entry("https://redd.it/abc")}</feed>"""
        assertEquals(listOf("https://www.reddit.com/r/formula1/comments/a/b/", "https://redd.it/abc"), parseReddit("formula1", xml).map { it.url })
    }

    @Test fun onlyListedSubredditsAreFetchable() {
        assertEquals(listOf("formula1", "formuladank", "f1discussion"), SUBREDDITS)
        assertTrue(SUBREDDITS.all { redditUrl(it).startsWith("https://www.reddit.com/r/$it/hot/.rss") })
    }
}
