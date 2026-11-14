package dev.anpo.f1.ui

import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.anpo.f1.R
import dev.anpo.f1.data.*
import coil.compose.AsyncImage
import java.time.*
import java.time.format.DateTimeFormatter

private fun raceInstant(r: RaceEntity): Instant? = utcMillis(r.date, r.time)?.let(Instant::ofEpochMilli)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(repo: Repo, onDriver: (String) -> Unit, onStandings: () -> Unit,
               onWeekend: () -> Unit, onCalendar: () -> Unit, onConstructor: (String) -> Unit) {
    val p = LocalPalette.current
    val drivers by repo.drivers().collectAsState(emptyList())
    val teams by repo.teams().collectAsState(emptyList())
    val races by repo.races().collectAsState(emptyList())
    val err by repo.error.collectAsState()

    LaunchedEffect(Unit) { repo.refreshRaces(); repo.refreshStandings() }

    val next = races.firstOrNull { r -> raceInstant(r)?.isAfter(Instant.now()) == true }
    val d = next?.let { Duration.between(Instant.now(), raceInstant(it) ?: Instant.now()) }

    var geo by remember(next?.circuitId) { mutableStateOf<CircuitGeometryEntity?>(null) }
    LaunchedEffect(next?.circuitId) { next?.let { repo.refreshCircuit(it.circuitId, it.season.toInt()); geo = repo.circuit(it.circuitId) } }

    val ctx = LocalContext.current
    var wanted by remember { mutableStateOf(Reminders.enabled(ctx)) }
    var allowed by remember { mutableStateOf(Reminders.canNotify(ctx)) }
    val reminderOn = wanted && allowed
    val tick = rememberTick()
    var note by remember { mutableStateOf<String?>(null) }
    var lastNote by remember { mutableStateOf("") }
    LaunchedEffect(note) { if (note != null) { lastNote = note.orEmpty(); delay(1600); note = null } }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pull = rememberPullToRefreshState()
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        allowed = ok; wanted = ok; Reminders.setEnabled(ctx, ok)
    }
    // The bell says "on" by default, so ask for notifications once, the first time the next race shows.
    LaunchedEffect(next?.id) {
        if (next != null && wanted && !allowed && !Reminders.asked(ctx)) { Reminders.markAsked(ctx); ask.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }
    LaunchedEffect(next?.id, reminderOn) {
        if (next != null && reminderOn) Reminders.schedule(ctx, next.name, raceInstant(next)?.toEpochMilli()) else Reminders.cancel(ctx)
    }
    if (races.isEmpty()) return   // nothing is drawn until the calendar arrives, as before

    Box(Modifier.fillMaxSize().background(p.canvas)) {
    PullToRefreshBox(
        isRefreshing = refreshing, state = pull, modifier = Modifier.fillMaxSize(),
        onRefresh = { tick(); scope.launch { refreshing = true; repo.refreshRaces(force = true); repo.refreshStandings(force = true); refreshing = false } },
        indicator = {
            PullToRefreshDefaults.Indicator(state = pull, isRefreshing = refreshing, modifier = Modifier.align(Alignment.TopCenter).padding(top = 36.dp),
                containerColor = p.nav, color = p.accent)
        }
    ) {
    LazyColumn(Modifier.fillMaxSize().ambient(p.isDark), contentPadding = PaddingValues(bottom = 140.dp)) {
        item {
            Box(Modifier.fillMaxWidth()) {
                // The next circuit bleeds off the right, behind the title (ref home: right:-44, top:120).
                geo?.let { g ->
                    Circuit3D(g, Modifier.size(230.dp).align(Alignment.TopEnd).offset(x = 46.dp, y = 100.dp))
                }
                Column {
                    Row(Modifier.fillMaxWidth().padding(horizontal = F1Dim.screenPad).padding(top = 60.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Formula 1", style = F1Type.body.copy(color = p.sec))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (next != null) ReminderBell(reminderOn) {
                                when {
                                    reminderOn -> { wanted = false; Reminders.setEnabled(ctx, false); tick(); note = "Reminder off" }
                                    !allowed -> ask.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    else -> { wanted = true; Reminders.setEnabled(ctx, true); tick(); note = "Reminder on, 1 h before lights out" }
                                }
                            }
                            Box(Modifier.heightIn(min = 48.dp).pressable(role = Role.Button, onClickLabel = "Open the calendar") { onCalendar() },
                                contentAlignment = Alignment.Center) {
                                Row(Modifier.clip(CircleShape).background(p.nav)
                                    .padding(start = 13.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(races.firstOrNull()?.season.orEmpty(), style = F1Type.body.copy(color = p.txt, fontSize = 12.sp))
                                    Icon(painterResource(R.drawable.ic_chevron_down), contentDescription = "Calendar",
                                        tint = p.sec, modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                    }
                    if (next != null && d != null) Column(Modifier.clickable { onWeekend() }
                        .padding(horizontal = F1Dim.screenPad).padding(top = 18.dp)) {
                        Text("Round ${next.round} · Next up", style = F1Type.meta.copy(color = p.sec))
                        Spacer(Modifier.height(8.dp))
                        // Poster type. Wraps on purpose; width capped so it clears the circuit.
                        Text(next.name.replace(" Grand Prix", "\nGrand Prix"),
                            style = F1Type.display.copy(color = p.txt), lineHeight = 41.sp,
                            modifier = Modifier.fillMaxWidth(0.62f))
                        Spacer(Modifier.height(8.dp))
                        Text(next.circuitName, style = F1Type.body.copy(color = p.accent, fontSize = 16.sp))
                        Spacer(Modifier.height(28.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                            CountBlock("%02d".format(d.toDays()), "DAYS")
                            CountBlock("%02d".format(d.toHours() % 24), "HOURS")
                            CountBlock("%02d".format(d.toMinutes() % 60), "MINS")
                        }
                        Spacer(Modifier.height(15.dp))
                        Text("Lights out ${next.date}", style = F1Type.meta.copy(color = p.sec, fontSize = 11.sp))
                    } else if (races.isNotEmpty()) SeasonComplete(races, onCalendar)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(F1Dim.screenPad).padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("Drivers", style = F1Type.section.copy(color = p.txt))
                Text("View all", style = F1Type.meta.copy(color = p.accent, fontSize = 12.sp),
                    modifier = Modifier.pressable(role = Role.Button) { onStandings() })
            }
        }
        item {
            F1Card(Modifier.padding(horizontal = 20.dp)) {
                drivers.take(5).forEachIndexed { i, s ->
                    DriverRow(repo, s, onDriver, showDivider = i < 4)
                }
            }
        }
        item {
            Text("Constructors", style = F1Type.section.copy(color = p.txt),
                modifier = Modifier.padding(F1Dim.screenPad).padding(top = 10.dp))
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                teams.take(3).forEach { t ->
                    F1Card(Modifier.weight(1f).pressable { onConstructor(t.teamId) }, radius = 22) {
                        Column(Modifier.padding(14.dp)) {
                            Box(Modifier.width(22.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                                .background(teamColour(repo.teamColour(t.teamId))))
                            Spacer(Modifier.height(9.dp))
                            Text("${t.points}", style = F1Type.stat.copy(color = p.txt, fontSize = 21.sp))
                            Spacer(Modifier.height(4.dp))
                            Text(t.name, style = F1Type.label.copy(color = p.sec), maxLines = 1)
                        }
                    }
                }
            }
        }
        if (err != null) item {
            // Never a spinner that never resolves. Say what's stale and move on.
            Text("Showing cached data · $err", style = F1Type.meta.copy(color = p.sec),
                modifier = Modifier.padding(F1Dim.screenPad), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
    }
    AnimatedVisibility(note != null, Modifier.align(Alignment.BottomCenter).padding(bottom = 118.dp),
        enter = fadeIn(tween(220, easing = EaseOut)) + slideInVertically(tween(220, easing = EaseOut)) { it / 2 },
        exit = fadeOut(tween(160))) {
        Text(lastNote, style = F1Type.body.copy(color = p.canvas, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
            modifier = Modifier.clip(CircleShape).background(p.txt).padding(horizontal = 16.dp, vertical = 10.dp))
    }
    }
}

@Composable
fun WeekendScreen(repo: Repo, onCircuit: (String) -> Unit) {
    val p = LocalPalette.current
    val races by repo.races().collectAsState(emptyList())
    LaunchedEffect(Unit) { repo.refreshRaces() }
    val next = races.firstOrNull { raceInstant(it)?.isAfter(Instant.now()) == true }
    if (next == null) {
        Box(Modifier.fillMaxSize().background(p.canvas), contentAlignment = Alignment.Center) {
            Text(if (races.isEmpty()) "Loading the calendar…" else "No race weekend ahead",
                style = F1Type.meta.copy(color = p.sec))
        }
        return
    }
    var geo by remember(next.circuitId) { mutableStateOf<CircuitGeometryEntity?>(null) }
    var laps by remember(next.circuitId) { mutableStateOf<Int?>(null) }
    LaunchedEffect(next.circuitId) {
        repo.refreshCircuit(next.circuitId, next.season.toInt())
        geo = repo.circuit(next.circuitId)
        repo.refreshCircuitHistory(next.circuitId)
        laps = repo.circuitLaps(next.circuitId)
    }
    val sessions = remember(next) { weekendSessions(next) }
    Column(Modifier.fillMaxSize().background(p.canvas).ambient(p.isDark).verticalScroll(rememberScrollState())) {
        Column(Modifier.padding(F1Dim.screenPad).padding(top = 60.dp)) {
            Text("Round ${next.round} · Race weekend", style = F1Type.meta.copy(color = p.sec))
            Spacer(Modifier.height(8.dp))
            Text(next.name.replace(" Grand Prix", "\nGrand Prix"),
                style = F1Type.display.copy(color = p.txt), lineHeight = 41.sp)
            Spacer(Modifier.height(8.dp))
            Text("${next.circuitName} ›", style = F1Type.body.copy(color = p.accent, fontSize = 16.sp),
                // Padding sits inside the clickable so the spacing matches v1.1.2 (9dp to the date) while the tap area grows.
                modifier = Modifier.clickable(role = Role.Button, onClickLabel = "Open circuit") { onCircuit(next.circuitId) }
                    .padding(bottom = 6.dp))
            Text("${next.date}${next.country?.let { " · $it" } ?: ""}",
                style = F1Type.meta.copy(color = p.sec, fontSize = 11.sp),
                modifier = Modifier.padding(top = 3.dp))
        }
        val g = geo
        if (g != null) {
            Circuit3D(g, Modifier.fillMaxWidth().height(360.dp))
        } else {
            Box(Modifier.fillMaxWidth().height(360.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (repo.circuitKey(next.circuitId) == null) "Circuit map not available yet"
                    else "Loading circuit…",
                    style = F1Type.meta.copy(color = p.sec)
                )
            }
        }

        if (g != null || laps != null) {
            Column(Modifier.padding(horizontal = F1Dim.screenPad)) {
                if (g != null) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        // Derived from the outline, so it is approximate: one decimal, never three.
                        Text(java.lang.String.format(java.util.Locale.ENGLISH, "%.1f", g.lengthKm),
                            style = F1Type.hero.copy(color = p.txt, fontSize = 50.sp), maxLines = 1, softWrap = false)
                        Text("KM", style = F1Type.body.copy(color = p.sec),
                            modifier = Modifier.padding(start = 6.dp, bottom = 7.dp))
                    }
                    Text("Circuit length", style = F1Type.meta.copy(color = p.sec, fontSize = 11.sp),
                        modifier = Modifier.padding(top = 5.dp))
                    Spacer(Modifier.height(24.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    // A tile with no known value is left out rather than shown wrong.
                    laps?.let { CircuitStat("$it", "", "No. of Laps", Modifier.weight(1f)) }
                    g?.let { CircuitStat("${it.corners}", "", "Turns", Modifier.weight(1f)) }
                }
            }
        }
        if (sessions.isNotEmpty()) {
            Text("Race Weekend", style = F1Type.section.copy(color = p.txt),
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 34.dp, bottom = 6.dp))
            Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)) {
                sessions.forEach { SessionRow(it.toRow()) }
            }
        }
        Spacer(Modifier.height(140.dp))
    }
}

private data class Session(val day: String, val dow: String, val name: String, val time: String, val race: Boolean)

/** Local to the phone, like a clock: the day, weekday and start time the user will actually see. */
private fun WeekendSession.toRow(): Session {
    val at = Instant.ofEpochMilli(startMs).atZone(ZoneId.systemDefault())
    val time = "%02d:%02d".format(at.hour, at.minute)
    return Session("${at.dayOfMonth}",
        at.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH).uppercase(),
        name, if (isRace) "$time · lights out" else time, isRace)
}

@Composable
internal fun CircuitStat(value: String, unit: String, label: String, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Column(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = F1Type.stat.copy(color = p.txt, fontSize = 19.sp))
            if (unit.isNotEmpty()) Text(unit, style = F1Type.label.copy(color = p.sec, fontSize = 11.sp),
                modifier = Modifier.padding(start = 3.dp, bottom = 1.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = F1Type.label.copy(color = p.sec, fontSize = 10.sp))
    }
}

@Composable
private fun SessionRow(s: Session) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            // Active race tint is theme-aware: a dark red-black on dark, a pale pink on
            // light. A single dark value left black text on a dark slab in light mode.
            .background(if (s.race) (if (p.isDark) Color(0xFF1A1216) else Color(0xFFFFF4F4)) else p.card)
            .then(if (s.race) Modifier.border(0.7.dp, p.accent.copy(alpha = 0.35f), RoundedCornerShape(28.dp)) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(if (s.race) p.accent else p.chip),
            contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.day, style = F1Type.stat.copy(color = if (s.race) Color.White else p.txt, fontSize = 16.sp))
                Text(s.dow, style = F1Type.label.copy(
                    color = if (s.race) Color.White.copy(alpha = 0.85f) else p.sec, fontSize = 8.sp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(s.name, style = F1Type.body.copy(color = p.txt))
            Spacer(Modifier.height(2.dp))
            Text(s.time, style = F1Type.label.copy(color = if (s.race) p.accent else p.sec, fontSize = 11.sp))
        }
    }
}

/** Shown in place of the countdown once no race lies ahead (the finale has run). */
@Composable
private fun SeasonComplete(races: List<RaceEntity>, onClick: () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.clickable(onClick = onClick).padding(horizontal = F1Dim.screenPad).padding(top = 18.dp)) {
        Text("${races.first().season} season", style = F1Type.meta.copy(color = p.sec))
        Spacer(Modifier.height(8.dp))
        Text("Season\ncomplete", style = F1Type.display.copy(color = p.txt), lineHeight = 41.sp)
        Spacer(Modifier.height(8.dp))
        Text("${races.size} rounds${races.last().locality?.let { " · finale in $it" }.orEmpty()}", style = F1Type.body.copy(color = p.accent, fontSize = 16.sp))
        Spacer(Modifier.height(15.dp))
        Text("See the full calendar", style = F1Type.meta.copy(color = p.sec, fontSize = 11.sp))
    }
}

@Composable private fun CountBlock(n: String, l: String) {
    val p = LocalPalette.current
    Column {
        Text(n, style = F1Type.display.copy(color = p.txt, fontSize = 40.sp))
        Spacer(Modifier.height(5.dp))
        Text(l, style = F1Type.label.copy(color = p.sec, fontSize = 9.sp))
    }
}

@Composable
private fun DriverRow(repo: Repo, s: DriverStandingEntity, onClick: (String) -> Unit, showDivider: Boolean) {
    val p = LocalPalette.current
    Column(Modifier.pressable { onClick(s.driverId) }) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${s.position}", style = F1Type.meta.copy(color = p.sec), modifier = Modifier.width(18.dp))
            // Team colour lives HERE - in the data - never in chrome.
            Box(Modifier.width(4.dp).height(24.dp).clip(RoundedCornerShape(2.dp))
                .background(teamColour(repo.teamColour(s.teamId))))
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(s.family, style = F1Type.body.copy(color = p.txt))
                Spacer(Modifier.height(2.dp))
                Text(s.team, style = F1Type.label.copy(color = p.sec))
            }
            if (s.wins > 0) { Text("${s.wins}W", style = F1Type.label.copy(color = p.sec)); Spacer(Modifier.width(9.dp)) }
            Text("${s.points}", style = F1Type.stat.copy(color = p.txt, fontSize = 16.sp))
        }
        if (showDivider) Box(Modifier.fillMaxWidth().height(0.5.dp).background(p.divider))
    }
}

@Composable
fun DriverScreen(repo: Repo, driverId: String, onTeam: (String) -> Unit, onRace: (Int) -> Unit) {
    val p = LocalPalette.current
    var s by remember { mutableStateOf<DriverStandingEntity?>(null) }
    var stat by remember { mutableStateOf<DriverSeasonStat?>(null) }
    LaunchedEffect(driverId) {
        s = repo.driver(driverId)
        repo.refreshResults()
        stat = repo.driverStat(driverId)
    }
    val d = s ?: return
    val col = teamColour(repo.teamColour(d.teamId))
    val key = repo.headshotKey(d.driverId)

    Column(Modifier.fillMaxSize().background(p.canvas).verticalScroll(rememberScrollState())) {
        // Draw order is load-bearing: carbon number, then photo, then the flanking
        // stats, then the name LAST so it lands on the masked, translucent bleed.
        // The figure is lifted (offset) so the name block sits over the FADED tail of
        // the photo, not the solid chest - otherwise the season line is unreadable.
        Box(Modifier.fillMaxWidth().height(520.dp).glow(col)) {
            // The number fills the whole hero, self-sizes to ~95% width, and sits low so
            // the tops bleed behind the head and the base drops into the name.
            CarbonNumber("${d.number ?: 0}", col, Modifier.matchParentSize())
            if (key != null) {
                // OPAQUE - no per-photo fade. The photo must fully occlude the number
                // behind it, or the number bleeds through the translucent suit and reads
                // as a low-opacity overlay ON the driver. The bottom is dissolved by the
                // name-fade below instead. Square box: chest sits at the box's bottom.
                AsyncImage(
                    model = fadeModel(LocalContext.current, Net.headshot(key)), contentDescription = "${d.given} ${d.family}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(300.dp).align(Alignment.BottomCenter).offset(y = (-90).dp)
                )
            }
            // Name-fade: dissolves BOTH the photo's bottom edge and the number's base into
            // the canvas. The canvas plateau (from ~55%) fully covers the number where it
            // sits behind the lower figure; the fade-in blends the driver's chest. This is
            // the single bottom dissolve - it replaces the photo's own DstIn mask.
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(210.dp)
                .background(Brush.verticalGradient(0f to Color.Transparent, 0.55f to p.canvas, 1f to p.canvas)))
            // Pinned to ~46% of the hero (relative to the Box, NOT the number) so they
            // stay at the shoulder line when the number drops. zIndex keeps them above
            // the carbon, which now reaches up into this band.
            Column(Modifier.align(BiasAlignment(-1f, -0.08f)).zIndex(1f).padding(start = 14.dp).wrapContentWidth(),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("%02d".format(d.position), style = F1Type.display.copy(color = p.txt, fontSize = 42.sp),
                    maxLines = 1, softWrap = false)
                Text("POS", style = F1Type.label.copy(color = p.sec))
            }
            Column(Modifier.align(BiasAlignment(1f, -0.08f)).zIndex(1f).padding(end = 14.dp).wrapContentWidth(),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${d.points}", style = F1Type.display.copy(color = p.txt, fontSize = 42.sp),
                    maxLines = 1, softWrap = false)
                Text("PTS", style = F1Type.label.copy(color = p.sec))
            }
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${d.season} SEASON · ${d.team.uppercase()}", style = F1Type.label.copy(color = p.sec, fontSize = 11.sp))
                Spacer(Modifier.height(5.dp))
                Text(d.given.split(" ").last(), style = F1Type.name.copy(color = p.txt))
                Text(d.family, style = F1Type.name.copy(color = col))
            }
        }
        // Not POS/PTS again - those already flank the figure. Derived from /results.
        F1Card(Modifier.padding(horizontal = 20.dp).padding(top = 22.dp), radius = 26) {
            Row(Modifier.padding(vertical = 17.dp, horizontal = 10.dp)) {
                StatBlock("%02d".format(d.wins), "WINS", Modifier.weight(1f))
                StatBlock("%02d".format(stat?.podiums ?: 0), "PODIUMS", Modifier.weight(1f))
                StatBlock("%02d".format(stat?.poles ?: 0), "POLES", Modifier.weight(1f))
                StatBlock("%02d".format(stat?.dnfs ?: 0), "DNFs", Modifier.weight(1f))
            }
        }
        stat?.roundPoints?.let { csv ->
            Text("Results", style = F1Type.section.copy(color = p.txt),
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 13.dp))
            PointsByRound(csv, col)
        }
        DriverDepth(repo, d, onTeam, onRace)
    }
}

/** Points per round as a bar chart (ported from f1-tab-complete.html). Team colour,
 *  height and opacity both scaled by value so a big haul reads loud and a blank reads
 *  faint. Zero-point rounds still get a hairline so the round grid stays legible. */
@Composable
internal fun PointsByRound(csv: String, col: Color) {
    val p = LocalPalette.current
    val pts = csv.split(",").map { it.toIntOrNull() ?: 0 }
    if (pts.isEmpty()) return
    val max = (pts.maxOrNull() ?: 0).coerceAtLeast(1)
    F1Card(Modifier.padding(horizontal = 20.dp), radius = 26) {
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            pts.forEachIndexed { i, v ->
                val frac = v.toFloat() / max
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier.fillMaxWidth()
                                // Zero scores get a 2dp stub, not a hidden 0dp line - "scored
                                // nothing" must read differently from "no data".
                                .height((64.dp * frac).coerceAtLeast(2.dp))
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(col.copy(alpha = if (v == 0) 0.28f else 0.40f + 0.60f * frac))
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    // Every second round, on one line: two-digit numbers used to wrap into "1 0".
                    if ((i + 1) % 2 == 0) Text("${i + 1}", style = F1Type.label.copy(color = p.sec, fontSize = 9.sp), maxLines = 1, softWrap = false)
                    else Text(" ", style = F1Type.label.copy(fontSize = 9.sp))
                }
            }
        }
    }
}

/* ---------- Calendar (F-09) ---------- */

@Composable
fun CalendarScreen(repo: Repo, onRace: (Int) -> Unit, onWeekend: () -> Unit, onCircuit: (String) -> Unit) {
    val p = LocalPalette.current
    val races by repo.races().collectAsState(emptyList())
    LaunchedEffect(Unit) { repo.refreshRaces() }
    val now = Instant.now()
    val nextRound = races.firstOrNull { raceInstant(it)?.isAfter(now) == true }?.round
    val listState = rememberLazyListState()
    // Compose clamps a restored scroll position to the item count, so the list is not composed until the
    // rows exist. Otherwise Back from a race result lands at the top of the calendar.
    if (races.isEmpty()) { Box(Modifier.fillMaxSize().background(p.canvas)); return }
    val collapsed by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 120 } }
    Box(Modifier.fillMaxSize().background(p.canvas)) {
    LazyColumn(Modifier.fillMaxSize().ambient(p.isDark), listState,
        contentPadding = PaddingValues(top = 60.dp, bottom = 140.dp)) {
        item {
            Column(Modifier.padding(F1Dim.screenPad)) {
                Text("${races.size} rounds", style = F1Type.meta.copy(color = p.sec))
                Spacer(Modifier.height(8.dp))
                Text("${races.firstOrNull()?.season.orEmpty()}\nCalendar", style = F1Type.display.copy(color = p.txt, fontSize = 42.sp), lineHeight = 43.sp)
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
        items(races) { r ->
            val done = raceInstant(r)?.isBefore(now) == true
            val isNext = r.round == nextRound
            // Past rounds open the result, the next one its weekend, later ones the circuit.
            CalendarRow(r, done, isNext) { if (done) onRace(r.round) else if (isNext) onWeekend() else onCircuit(r.circuitId) }
        }
    }
    CompactTitleBar("Calendar", collapsed, Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun CalendarRow(r: RaceEntity, done: Boolean, isNext: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val bg = if (isNext) (if (p.isDark) Color(0xFF1A1216) else Color(0xFFFFF4F4)) else p.card
    val date = runCatching { LocalDate.parse(r.date).format(DateTimeFormatter.ofPattern("d MMM")) }.getOrDefault(r.date)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(24.dp)).background(bg)
            .pressable(role = Role.Button, onClick = onClick)
            .then(if (isNext) Modifier.border(0.7.dp, p.accent.copy(alpha = 0.35f), RoundedCornerShape(24.dp)) else Modifier)
            .alpha(if (done) 0.55f else 1f)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(if (isNext) p.accent else p.chip),
            contentAlignment = Alignment.Center) {
            Text("${r.round}", style = F1Type.stat.copy(color = if (isNext) Color.White else p.txt, fontSize = 13.sp))
        }
        Column(Modifier.weight(1f)) {
            Text(r.name.replace(" Grand Prix", " GP"), style = F1Type.body.copy(color = p.txt), maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(r.locality ?: r.circuitName, maxLines = 1,
                style = F1Type.label.copy(color = if (isNext) p.accent else p.sec, fontSize = 10.sp))
        }
        Text(date, style = F1Type.label.copy(color = p.sec, fontSize = 11.sp))
    }
}

/* ---------- Constructor (F-13) ---------- */

@Composable
fun ConstructorScreen(repo: Repo, teamId: String, onDriver: (String) -> Unit) {
    val p = LocalPalette.current
    val teams by repo.teams().collectAsState(emptyList())
    val allDrivers by repo.drivers().collectAsState(emptyList())
    val team = teams.firstOrNull { it.teamId == teamId } ?: return
    val position = teams.indexOfFirst { it.teamId == teamId } + 1
    val col = teamColour(repo.teamColour(teamId))
    val info = repo.constructorInfo(teamId)
    val teamDrivers = allDrivers.filter { it.teamId == teamId }.take(2)
    val wins = teamDrivers.sumOf { it.wins }

    var podiums by remember { mutableStateOf(0) }
    var poles by remember { mutableStateOf(0) }
    var dnfs by remember { mutableStateOf(0) }
    LaunchedEffect(teamId, teamDrivers.map { it.driverId }) {
        repo.refreshResults()
        var pod = 0; var pol = 0; var dnf = 0
        teamDrivers.forEach { d -> repo.driverStat(d.driverId)?.let { pod += it.podiums; pol += it.poles; dnf += it.dnfs } }
        podiums = pod; poles = pol; dnfs = dnf
    }

    Column(Modifier.fillMaxSize().background(p.canvas).verticalScroll(rememberScrollState())) {
        TeamHero(
            HeroTeam(team.name, "${team.season}", position, "${team.points}", "${info.champs}", repo.carFolder(teamId)),
            teamDrivers.map { HeroDriver(it.driverId, it.family, repo.headshotKey(it.driverId)) }, col, onDriver,
        )
        F1Card(Modifier.padding(horizontal = 20.dp).padding(top = 22.dp), radius = 26) {
            Row(Modifier.padding(vertical = 17.dp, horizontal = 10.dp)) {
                StatBlock("%02d".format(wins), "WINS", Modifier.weight(1f))
                StatBlock("%02d".format(podiums), "PODIUMS", Modifier.weight(1f))
                StatBlock("%02d".format(poles), "POLES", Modifier.weight(1f))
                StatBlock("%02d".format(dnfs), "DNFs", Modifier.weight(1f))
            }
        }
        F1Card(Modifier.padding(horizontal = 20.dp).padding(top = 14.dp), radius = 26) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                MetaRow("Full name", info.full)
                MetaRow("Base", info.base)
                MetaRow("Power unit", info.pu)
                MetaRow("Entered F1", "${info.since}")
                MetaRow("Constructors' titles", "${info.champs}", last = true)
            }
        }
        TeamDepth(repo, team, col)
    }
}

@Composable
private fun MetaRow(k: String, v: String, last: Boolean = false) {
    val p = LocalPalette.current
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(k, style = F1Type.meta.copy(color = p.sec), modifier = Modifier.weight(1f))
            Text(v, style = F1Type.body.copy(color = p.txt, fontSize = 13.sp))
        }
        if (!last) Box(Modifier.fillMaxWidth().height(0.5.dp).background(p.divider))
    }
}
