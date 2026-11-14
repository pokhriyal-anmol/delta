package dev.anpo.f1.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class NewsTest {
    private fun feed(id: String): List<NewsItem> {
        val xml = javaClass.getResourceAsStream("/feeds/$id.xml")!!.bufferedReader().readText()
        return parseFeed(FEEDS.first { it.id == id }, xml)
    }

    /** Real payloads saved 2026-11-14. Whatever the source, an item is plain text with an https link. */
    @Test fun everyRealFeedParsesIntoPlainCappedItems() {
        for (src in FEEDS) {
            val items = feed(src.id)
            assertTrue("${src.id} gave no items", items.isNotEmpty())
            assertTrue(items.size <= MAX_ITEMS_PER_FEED)
            for (i in items) {
                assertTrue(i.url.startsWith("https://"))
                assertTrue(i.title.isNotBlank() && i.title.length <= MAX_TITLE)
                assertTrue("tag left in ${src.id}: ${i.title} / ${i.snippet}", !i.title.contains('<') && !i.snippet.contains('<') && !i.snippet.contains("&lt;"))
                assertTrue(i.snippet.length <= MAX_SNIPPET + 1)
                i.imageUrl?.let { assertTrue(it.startsWith("https://")) }
            }
        }
    }

    @Test fun imagesComeFromTheRightPlaceOrNowhere() {
        assertTrue(feed("bbc").any { it.imageUrl != null })        // media:thumbnail
        assertTrue(feed("skysports").any { it.imageUrl != null })  // enclosure
        assertTrue(feed("autosport").any { it.imageUrl != null })
        assertTrue(feed("espn").all { it.imageUrl == null })
        assertTrue(feed("racefans").all { it.imageUrl == null })
        assertTrue(feed("f1official").all { it.imageUrl == null })
    }

    @Test fun guardianTakesTheWidestPictureOffered() {
        val img = feed("guardian").first { it.imageUrl != null }.imageUrl!!
        assertTrue(img, img.contains("width=700"))
    }

    @Test fun raceFansTitleLosesItsSuffix() = assertTrue(feed("racefans").none { it.title.endsWith("| Formula 1") })

    @Test fun bbcSnippetIsDroppedWhenItRepeatsTheTitle() {
        val raw = javaClass.getResourceAsStream("/feeds/bbc.xml")!!.bufferedReader().readText()
        val items = parseFeed(FEEDS.first { it.id == "bbc" }, raw)
        assertTrue(items.none { it.snippet.isNotEmpty() && it.snippet.lowercase().startsWith(it.title.lowercase()) })
    }

    @Test fun formula1HasNoDatesSoItGetsNone() = assertTrue(feed("f1official").all { it.publishedAt == null })

    @Test fun dateFormatsTheFeedsActuallyUse() {
        assertEquals(Instant.parse("2026-11-13T13:45:36Z").toEpochMilli(), parseDate("Fri, 13 Nov 2026 08:45:36 EST"))
        assertEquals(Instant.parse("2026-11-14T10:00:10Z").toEpochMilli(), parseDate("Sat, 14 Nov 2026 10:00:10 +0000"))
        assertEquals(Instant.parse("2026-11-14T10:00:10Z").toEpochMilli(), parseDate("Sat, 14 Nov 2026 10:00:10 GMT\n"))
        assertEquals(Instant.parse("2026-11-14T10:00:10Z").toEpochMilli(), parseDate("2026-11-14T10:00:10+00:00"))
        assertNull(parseDate("")); assertNull(parseDate("last Tuesday"))
    }

    @Test fun markupAndEntitiesBecomePlainText() {
        assertEquals("Fish & chips one · It’s “fine”", plainText("<p>Fish &amp; chips</p><ul><li>one</li><li>It&#8217;s &ldquo;fine&rdquo;</li></ul>"))
        assertEquals("a b", plainText("&lt;p&gt;a&lt;/p&gt;&lt;script&gt;alert(1)&lt;/script&gt;<b>b</b>"))
        assertEquals("ab", plainText("a⁠​b"))
    }

    @Test fun cutsOnAWordBoundary() {
        val s = "one two three four five six seven eight nine ten"
        assertEquals("one two three…", cut(s, 15))
        assertEquals(s, cut(s, 80))
    }

    private fun rss(vararg items: String) = """<rss version="2.0"><channel>${items.joinToString("")}</channel></rss>"""
    private val src = FeedSource("t", "Test", "https://t.example/feed")

    @Test fun nonHttpsAndMalformedLinksAreRejected() {
        val xml = rss("<item><title>a one</title><link>http://t.example/a</link></item>",
            "<item><title>b</title><link>javascript:alert(1)</link></item>",
            "<item><title>c</title><link>intent://x#Intent;end</link></item>",
            "<item><title>d</title><link>file:///etc/passwd</link></item>",
            "<item><title>e</title><link>https://</link></item>",
            "<item><title>keeper</title><link>https://t.example/ok</link></item>")
        assertEquals(listOf("keeper"), parseFeed(src, xml).map { it.title })
    }

    @Test fun oversizeTextAndItemCountsAreCapped() {
        val big = "x".repeat(5000)
        val one = parseFeed(src, rss("<item><title>$big</title><link>https://t.example/1</link><description>$big</description></item>")).single()
        assertEquals(MAX_TITLE, one.title.length); assertTrue(one.snippet.length <= MAX_SNIPPET + 1)
        val many = (1..100).joinToString("") { "<item><title>t$it</title><link>https://t.example/$it</link></item>" }
        assertEquals(MAX_ITEMS_PER_FEED, parseFeed(src, rss(many)).size)
    }

    /** An entity-bomb DOCTYPE must neither expand nor fetch anything, and must not take the app down. */
    @Test fun doctypeEntitiesAreNotExpanded() {
        val xml = """<?xml version="1.0"?><!DOCTYPE r [<!ENTITY a "AAAAAAAAAA"><!ENTITY b "&a;&a;&a;&a;&a;&a;">]>
            <rss version="2.0"><channel><item><title>safe &b;</title><link>https://t.example/1</link></item></channel></rss>"""
        val result = runCatching { parseFeed(src, xml) }
        assertTrue(result.isFailure || result.getOrThrow().all { !it.title.contains("AAAA") })
    }

    @Test fun sameStoryIsKeptOnceAndTheOneWithAPictureWins() {
        fun item(source: String, title: String, image: String?, at: Long?) = NewsItem(source, title, "", image, at, "https://x.example/$source", 0)
        val merged = mergeNews(listOf(
            item("A", "Hamilton wins the Las Vegas Grand Prix after late safety car", null, 2000L),
            item("B", "Hamilton wins the Las Vegas Grand Prix after a late safety car", "https://img.example/1.jpg", 1000L),
            item("C", "Verstappen extends his contract", null, 3000L),
            item("D", "Undated official story", null, null)))
        assertEquals(listOf("C", "B", "D"), merged.map { it.source })
        assertNotNull(merged.first { it.source == "B" }.imageUrl)
        assertFalse(merged.any { it.source == "A" })
    }
}
