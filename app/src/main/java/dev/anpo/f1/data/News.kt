package dev.anpo.f1.data

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URI
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Everything in a feed is text written by a stranger. These limits keep one bad feed from filling the cache. */
const val MAX_FEED_BYTES = 2_000_000
const val MAX_ITEMS_PER_FEED = 30
const val MAX_TITLE = 200
const val MAX_SNIPPET = 160
const val MAX_URL = 2048

data class FeedSource(val id: String, val name: String, val url: String, val titleSuffix: String? = null)

/** Eight feeds that returned real items over https on 2026-11-14. Spec: .scratch/f1-v2/spec.md, "News". */
val FEEDS = listOf(
    FeedSource("bbc", "BBC Sport", "https://feeds.bbci.co.uk/sport/formula1/rss.xml"),
    FeedSource("skysports", "Sky Sports", "https://www.skysports.com/rss/12433"),
    FeedSource("guardian", "The Guardian", "https://www.theguardian.com/sport/formulaone/rss"),
    FeedSource("autosport", "Autosport", "https://www.autosport.com/rss/f1/news/"),
    FeedSource("therace", "The Race", "https://www.the-race.com/feed/"),
    FeedSource("racefans", "RaceFans", "https://www.racefans.net/feed/", titleSuffix = " | Formula 1"),
    FeedSource("espn", "ESPN", "https://www.espn.com/espn/rss/f1/news"),
    FeedSource("f1official", "Formula 1", "https://www.formula1.com/content/fom-website/en/latest/all.xml"),
)

/** One story, the same shape whatever feed it came from. publishedAt is null when the feed gives no date. */
data class NewsItem(val source: String, val title: String, val snippet: String, val imageUrl: String?,
                    val publishedAt: Long?, val url: String, val rank: Int)

private class Raw {
    var title = ""; var link = ""; var guid = ""; var guidIsLink = false
    var description = ""; var content = ""; var date = ""
    val images = mutableListOf<Pair<String, Int>>()   // url to a width used only to pick the widest
}

/**
 * Parses an RSS 2.0 feed into plain, capped items. The parser is Android's pull parser: it does not fetch
 * external entities. A feed that is not well-formed throws, and the caller treats that as "this source failed".
 */
fun parseFeed(source: FeedSource, xml: String): List<NewsItem> {
    val p = XmlPullParserFactory.newInstance().newPullParser().apply { setInput(StringReader(xml)) }
    val out = mutableListOf<NewsItem>()
    while (p.next() != XmlPullParser.END_DOCUMENT && out.size < MAX_ITEMS_PER_FEED) {
        if (p.eventType == XmlPullParser.START_TAG && p.name == "item") {
            toItem(source, readItem(p), out.size)?.let { out += it }
        }
    }
    return out
}

private fun readItem(p: XmlPullParser): Raw {
    val r = Raw()
    val depth = p.depth
    while (!(p.next() == XmlPullParser.END_TAG && p.depth == depth)) {
        if (p.eventType != XmlPullParser.START_TAG) continue
        when (p.name) {
            "title" -> r.title = p.nextText()
            "link" -> r.link = p.nextText()
            "guid" -> { r.guidIsLink = p.getAttributeValue(null, "isPermaLink") != "false"; r.guid = p.nextText() }
            "description" -> r.description = p.nextText()
            "content:encoded" -> r.content = p.nextText()
            "pubDate", "dc:date" -> r.date = p.nextText()
            "enclosure" -> if (p.getAttributeValue(null, "type").orEmpty().startsWith("image"))
                p.getAttributeValue(null, "url")?.let { r.images += it to (p.getAttributeValue(null, "width")?.toIntOrNull() ?: 800) }
            "media:content" -> if (isImage(p.getAttributeValue(null, "medium"), p.getAttributeValue(null, "type")))
                p.getAttributeValue(null, "url")?.let { r.images += it to (p.getAttributeValue(null, "width")?.toIntOrNull() ?: 800) }
            "media:thumbnail" -> p.getAttributeValue(null, "url")?.let { r.images += it to (p.getAttributeValue(null, "width")?.toIntOrNull() ?: 100) }
        }
    }
    return r
}

private fun isImage(medium: String?, type: String?) = medium == "image" || type.orEmpty().startsWith("image") || (medium == null && type == null)

private fun toItem(source: FeedSource, r: Raw, rank: Int): NewsItem? {
    val title = plainText(r.title).removeSuffix(source.titleSuffix.orEmpty()).trim().take(MAX_TITLE)
    val url = httpsUrl(r.link.trim().ifEmpty { if (r.guidIsLink) r.guid.trim() else "" }) ?: return null
    if (title.isEmpty()) return null
    val html = r.content.ifEmpty { r.description }
    val snippet = plainText(r.description.ifEmpty { r.content }).let { if (startsWithTitle(it, title)) "" else cut(it, MAX_SNIPPET) }
    val image = (r.images + listOfNotNull(firstImg(html)?.let { it to 800 })).mapNotNull { (u, w) -> httpsUrl(u)?.let { it to w } }
        .maxByOrNull { it.second }?.first
    return NewsItem(source.name, title, snippet, image, parseDate(r.date), url, rank)
}

/** Only absolute https links with a host, and no longer than a browser would accept. */
fun httpsUrl(raw: String): String? {
    val s = raw.trim()
    if (s.length > MAX_URL || !s.startsWith("https://", ignoreCase = true)) return null
    val u = runCatching { URI(s) }.getOrNull() ?: return null
    return if (u.scheme.equals("https", true) && !u.host.isNullOrEmpty()) s else null
}

private fun firstImg(html: String): String? =
    Regex("""<img[^>]+src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(decodeEntities(html))?.groupValues?.get(1)

private val INVISIBLE = Regex("[⁠​‌‍­﻿]")

private val COMMENT = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
private val SCRIPT = Regex("<(script|style)[^>]*>.*?</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val LIST_END = Regex("</li\\s*>", RegexOption.IGNORE_CASE)
private val BREAKS = Regex("<br\\s*/?>|</p\\s*>|</div\\s*>", RegexOption.IGNORE_CASE)
private val TAG = Regex("<[^>]*>")

private fun stripMarkup(s: String) = s.replace(COMMENT, " ").replace(SCRIPT, " ").replace(LIST_END, " · ").replace(BREAKS, " ").replace(TAG, " ")

/**
 * Strips every tag and decodes entities, so a title or snippet is only ever plain text. Feeds often carry
 * escaped markup (&lt;p&gt;), which only turns into tags after the first decode, so the strip runs twice.
 */
fun plainText(html: String): String =
    decodeEntities(stripMarkup(decodeEntities(stripMarkup(html))))
        .replace(INVISIBLE, "").replace(Regex("\\s+"), " ").trim().trim('·').trim()

private val NAMED = mapOf("amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
    "rsquo" to "’", "lsquo" to "‘", "rdquo" to "”", "ldquo" to "“", "hellip" to "…", "ndash" to "–", "mdash" to "—")

fun decodeEntities(s: String): String = Regex("&(#x?[0-9a-fA-F]+|[a-zA-Z]+);").replace(s) { m ->
    val e = m.groupValues[1]
    when {
        e.startsWith("#x") || e.startsWith("#X") -> e.drop(2).toIntOrNull(16)?.let { cp(it) }
        e.startsWith("#") -> e.drop(1).toIntOrNull()?.let { cp(it) }
        else -> NAMED[e]
    } ?: m.value
}

private fun cp(code: Int): String? = if (code in 1..0x10FFFF && code !in 0xD800..0xDFFF) String(Character.toChars(code)) else null

private fun startsWithTitle(snippet: String, title: String): Boolean {
    fun norm(x: String) = x.lowercase().filter { it.isLetterOrDigit() }
    return norm(title).isNotEmpty() && norm(snippet).startsWith(norm(title))
}

/** Cuts at a word boundary and marks the cut. */
fun cut(s: String, max: Int): String {
    if (s.length <= max) return s
    val head = s.take(max)
    val at = head.lastIndexOf(' ')
    return (if (at > max / 2) head.take(at) else head).trimEnd(',', ';', ':', '.', ' ') + "…"
}

private val DATE_PATTERNS = listOf("EEE, dd MMM yyyy HH:mm:ss zzz", "EEE, dd MMM yyyy HH:mm:ss Z", "dd MMM yyyy HH:mm:ss zzz")

/** Lenient on purpose: a trailing newline, "EST" for a zone, or ISO 8601. Null when nothing parses, never a guess. */
fun parseDate(raw: String): Long? {
    val s = raw.trim()
    if (s.isEmpty()) return null
    runCatching { return ZonedDateTime.parse(s, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() }
    runCatching { return OffsetDateTime.parse(s).toInstant().toEpochMilli() }
    for (pattern in DATE_PATTERNS) {
        runCatching { return SimpleDateFormat(pattern, Locale.ENGLISH).apply { isLenient = false }.parse(s)!!.time }
    }
    return null
}

private fun tokens(title: String): Set<String> =
    title.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.length > 2 }.toSet()

/** Near-identical headlines from different sources are the same story. Same story worded differently is not caught. */
fun sameStory(a: String, b: String): Boolean {
    val x = tokens(a); val y = tokens(b)
    if (x.size < 4 || y.size < 4) return a.equals(b, ignoreCase = true)
    return x.intersect(y).size.toDouble() / x.union(y).size >= 0.8
}

/** Newest first, undated stories last in feed order, and a repeated story kept once (the one with a picture). */
fun mergeNews(items: List<NewsItem>): List<NewsItem> {
    val sorted = items.sortedWith(compareByDescending<NewsItem> { it.publishedAt != null }
        .thenByDescending { it.publishedAt ?: 0L }.thenBy { it.rank })
    val kept = mutableListOf<NewsItem>()
    for (it in sorted) {
        val i = kept.indexOfFirst { k -> sameStory(k.title, it.title) }
        if (i < 0) kept += it else if (kept[i].imageUrl == null && it.imageUrl != null) kept[i] = it.copy(publishedAt = kept[i].publishedAt ?: it.publishedAt)
    }
    return kept
}
