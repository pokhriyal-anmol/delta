package dev.anpo.f1.ui

import kotlinx.coroutines.launch
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.anpo.f1.data.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val PRESS = 0
private const val REDDIT = 1

/** Only https links leave the app, and only to the browser (or the Reddit app) through a plain view intent. */
private fun open(ctx: Context, url: String) {
    if (httpsUrl(url) == null) return
    try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: ActivityNotFoundException) { }
}

/** "now", "12m", "3h", "2d", then a date. Null when the feed gave no date. */
private fun ago(at: Long?, now: Long): String? {
    if (at == null) return null
    val min = ((now - at) / 60_000).coerceAtLeast(0)
    return when {
        min < 1 -> "now"; min < 60 -> "${min}m"; min < 60 * 24 -> "${min / 60}h"; min < 60 * 24 * 7 -> "${min / (60 * 24)}d"
        else -> Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))
    }
}

/** Press headlines from eight feeds in a lead-and-wire layout, and a Reddit tab for one followed subreddit. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(repo: Repo) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(PRESS) }
    var sub by rememberSaveable { mutableStateOf(repo.redditChoice()) }
    val press by repo.news().collectAsState(emptyList())
    val posts by remember(sub) { repo.reddit(sub) }.collectAsState(emptyList())
    val failed by repo.newsFailed.collectAsState()
    val problem by repo.redditProblem.collectAsState()
    val loading by repo.newsLoading.collectAsState()
    val now = remember(press, posts) { System.currentTimeMillis() }
    val scope = rememberCoroutineScope()
    val tick = rememberTick()
    var refreshing by remember { mutableStateOf(false) }
    val pull = rememberPullToRefreshState()

    LaunchedEffect(tab, sub) { if (tab == PRESS) repo.refreshNews() else repo.refreshReddit(sub) }
    val refresh: () -> Unit = {
        scope.launch {
            refreshing = true
            if (tab == PRESS) repo.refreshNews(force = true) else repo.refreshReddit(sub, force = true)
            refreshing = false
        }
    }

    val list = rememberLazyListState()
    val collapsed by remember { derivedStateOf { list.firstVisibleItemIndex > 0 || list.firstVisibleItemScrollOffset > 160 } }
    Box(Modifier.fillMaxSize().background(p.canvas)) {
        PullToRefreshBox(
            isRefreshing = refreshing, state = pull, modifier = Modifier.fillMaxSize(),
            onRefresh = { tick(); refresh() },
            indicator = {
                PullToRefreshDefaults.Indicator(state = pull, isRefreshing = refreshing, modifier = Modifier.align(Alignment.TopCenter).padding(top = 36.dp),
                    containerColor = p.nav, color = p.accent)
            }
        ) {
            LazyColumn(Modifier.fillMaxSize().ambient(p.isDark), list, contentPadding = PaddingValues(bottom = 140.dp)) {
                item {
                    Column(Modifier.padding(F1Dim.screenPad).padding(top = 60.dp)) {
                        Text(if (tab == PRESS) "${press.size} stories" else "${posts.size} posts", style = F1Type.meta.copy(color = p.sec))
                        Spacer(Modifier.height(8.dp))
                        Text("News", style = F1Type.display.copy(color = p.txt, fontSize = 42.sp), lineHeight = 43.sp)
                        Spacer(Modifier.height(20.dp))
                        Segments(listOf("Press", "Reddit"), tab) { tab = it }
                        if (tab == REDDIT) { Spacer(Modifier.height(4.dp)); SubredditPicker(sub) { tick(); sub = it; repo.setRedditChoice(it) } }
                    }
                }
                if (tab == PRESS) pressItems(press, failed, loading, now, refresh) { open(ctx, it) }
                else redditItems(posts, problem, loading, now, sub, refresh) { open(ctx, it) }
            }
        }
        CompactTitleBar("News", collapsed, Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun SubredditPicker(selected: String, onPick: (String) -> Unit) {
    val p = LocalPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SUBREDDITS.forEach { s ->
            val on = s == selected
            Box(Modifier.heightIn(min = 48.dp).pressable(role = Role.Tab, onClickLabel = "Follow r/$s") { onPick(s) }, contentAlignment = Alignment.Center) {
                Text("r/$s", style = F1Type.body.copy(color = if (on) p.txt else p.sec, fontSize = 12.sp),
                    modifier = Modifier.clip(CircleShape).background(if (on) p.nav else p.chip).padding(horizontal = 13.dp, vertical = 9.dp))
            }
        }
    }
}

private fun LazyListScope.pressItems(
    items: List<NewsItem>, failed: List<String>, loading: Boolean, now: Long, retry: () -> Unit, onOpen: (String) -> Unit) {
    val lead = items.firstOrNull { it.imageUrl != null }
    val wire = items.filter { it !== lead }
    if (items.isEmpty()) item {
        F1Card(Modifier.padding(horizontal = 20.dp)) {
            if (loading) SkeletonRows(6, 76.dp)
            else StateMessage("No headlines yet", "Check your connection.", "Try again", onAction = retry)
        }
    }
    lead?.let { item { LeadCard(it, now, onOpen) } }
    if (wire.isNotEmpty()) item {
        F1Card(Modifier.padding(horizontal = 20.dp).padding(top = if (lead != null) 14.dp else 0.dp)) {
            wire.forEachIndexed { i, n -> WireRow(n, ago(n.publishedAt, now), n.source, onOpen, i < wire.lastIndex, i) }
        }
    }
    if (failed.isNotEmpty()) item { Note("Couldn't load: ${failed.joinToString(", ")}.") }
}

private fun LazyListScope.redditItems(
    posts: List<NewsItem>, problem: String?, loading: Boolean, now: Long, sub: String, retry: () -> Unit, onOpen: (String) -> Unit) {
    if (posts.isEmpty()) item {
        F1Card(Modifier.padding(horizontal = 20.dp)) {
            if (loading) SkeletonRows(6, 76.dp)
            else StateMessage(problem ?: "Nothing from r/$sub yet", "Reddit allows about one request a minute.", "Try again", onAction = retry)
        }
    } else {
        item {
            F1Card(Modifier.padding(horizontal = 20.dp)) {
                posts.forEachIndexed { i, n -> WireRow(n, ago(n.publishedAt, now), n.source, onOpen, i < posts.lastIndex, i) }
            }
        }
        item { Note(problem ?: "Hot on r/$sub. Posts open in Reddit.") }
    }
}

@Composable
private fun Note(text: String) {
    val p = LocalPalette.current
    Text(text, style = F1Type.meta.copy(color = p.sec, fontSize = 11.sp), textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 24.dp))
}

/** The freshest story with a picture. The picture fades into the card the way the driver hero fades into the canvas. */
@Composable
private fun LeadCard(n: NewsItem, now: Long, onOpen: (String) -> Unit) {
    val p = LocalPalette.current
    F1Card(Modifier.padding(horizontal = 20.dp), radius = 28) {
        val ctx = LocalContext.current
        Column(Modifier.pressable(role = Role.Button, onClickLabel = "Open story") { onOpen(n.url) }) {
            Box(Modifier.fillMaxWidth().height(210.dp).background(p.chip)) {
                AsyncImage(model = fadeModel(ctx, n.imageUrl.orEmpty()), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to p.card)))
            }
            Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 18.dp)) {
                Meta(n.source, ago(n.publishedAt, now))
                Spacer(Modifier.height(6.dp))
                Text(n.title, style = F1Type.body.copy(color = p.txt, fontSize = 22.sp, lineHeight = 27.sp), maxLines = 4, overflow = TextOverflow.Ellipsis)
                if (n.snippet.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(n.snippet, style = F1Type.label.copy(color = p.sec, fontSize = 13.sp), maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun Meta(source: String, time: String?) {
    val p = LocalPalette.current
    Text(listOfNotNull(source.uppercase(), time).joinToString(" · "), style = F1Type.label.copy(color = p.accent, fontSize = 10.sp))
}

/** One dense wire row: source and time, then the headline, then a line of text if the post has one. */
@Composable
private fun WireRow(n: NewsItem, time: String?, source: String, onOpen: (String) -> Unit, divider: Boolean, index: Int) {
    val p = LocalPalette.current
    Column(Modifier.arrive(index).pressable(role = Role.Button, onClickLabel = "Open story") { onOpen(n.url) }) {
        Column(Modifier.heightIn(min = 64.dp).padding(horizontal = 18.dp, vertical = 14.dp)) {
            Meta(source, time)
            Spacer(Modifier.height(5.dp))
            Text(n.title, style = F1Type.body.copy(color = p.txt, fontSize = 15.sp, lineHeight = 20.sp), maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (n.snippet.isNotEmpty() && source.startsWith(REDDIT_PREFIX)) {
                Spacer(Modifier.height(4.dp))
                Text(n.snippet, style = F1Type.label.copy(color = p.sec, fontSize = 12.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (divider) Box(Modifier.fillMaxWidth().height(0.5.dp).background(p.divider))
    }
}
