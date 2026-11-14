package dev.anpo.f1.data

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URI

/**
 * The subreddits a user can follow. This fixed list is the only thing that ever reaches the request URL,
 * so a name cannot be typed in. Reddit's feeds allow about one request a minute without an account, so
 * only the selected one is fetched, and only when the tab opens.
 */
val SUBREDDITS = listOf("formula1", "formuladank", "f1discussion")
const val REDDIT_PREFIX = "r/"

fun redditUrl(sub: String) = "https://www.reddit.com/r/$sub/hot/.rss?limit=25"

private fun redditHost(url: String): Boolean {
    val host = runCatching { URI(url).host }.getOrNull()?.lowercase() ?: return false
    return host == "reddit.com" || host.endsWith(".reddit.com") || host == "redd.it"
}

private class Post { var title = ""; var link = ""; var date = ""; var content = "" }

private val SELF_TEXT = Regex("""<div class="md">(.*?)</div>\s*<!-- SC_ON -->""", RegexOption.DOT_MATCHES_ALL)

/**
 * Reddit serves an Atom feed with no scores or comment counts. Posts keep Reddit's own "hot" order as rank.
 * The snippet is the start of a text post, and a link post has none. Everything is plain text.
 */
fun parseReddit(sub: String, xml: String): List<NewsItem> {
    val p = XmlPullParserFactory.newInstance().newPullParser().apply { setInput(StringReader(xml)) }
    val out = mutableListOf<NewsItem>()
    while (p.next() != XmlPullParser.END_DOCUMENT && out.size < MAX_ITEMS_PER_FEED) {
        if (p.eventType == XmlPullParser.START_TAG && p.name == "entry") toPost(sub, readPost(p), out.size)?.let { out += it }
    }
    return out
}

private fun readPost(p: XmlPullParser): Post {
    val r = Post(); val depth = p.depth
    while (!(p.next() == XmlPullParser.END_TAG && p.depth == depth)) {
        if (p.eventType != XmlPullParser.START_TAG) continue
        when (p.name) {
            "title" -> r.title = p.nextText()
            "link" -> { r.link = p.getAttributeValue(null, "href").orEmpty(); p.nextText() }
            "published", "updated" -> if (r.date.isEmpty()) r.date = p.nextText()
            "content" -> r.content = p.nextText()
        }
    }
    return r
}

private fun toPost(sub: String, r: Post, rank: Int): NewsItem? {
    val title = plainText(r.title).take(MAX_TITLE)
    val url = httpsUrl(r.link)?.takeIf { redditHost(it) } ?: return null
    if (title.isEmpty()) return null
    val snippet = SELF_TEXT.find(r.content)?.groupValues?.get(1)?.let { cut(plainText(it), MAX_SNIPPET) }.orEmpty()
    return NewsItem("$REDDIT_PREFIX$sub", title, snippet, null, parseDate(r.date), url, rank)
}
