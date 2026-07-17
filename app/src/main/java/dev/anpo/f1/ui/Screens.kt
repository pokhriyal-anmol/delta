package dev.anpo.f1.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anpo.f1.data.*
import coil.compose.AsyncImage
import java.time.*
import java.time.format.DateTimeFormatter

private fun raceInstant(r: RaceEntity): Instant? = runCatching {
    val t = (r.time ?: "12:00:00Z").removeSuffix("Z")
    LocalDateTime.parse("${r.date}T$t").toInstant(ZoneOffset.UTC)
}.getOrNull()

@Composable
fun HomeScreen(repo: Repo, onDriver: (String) -> Unit, onStandings: () -> Unit, onWeekend: () -> Unit) {
    val p = LocalPalette.current
    val drivers by repo.drivers().collectAsState(emptyList())
    val teams by repo.teams().collectAsState(emptyList())
    val races by repo.races().collectAsState(emptyList())
    val err by repo.error.collectAsState()

    LaunchedEffect(Unit) { repo.refreshRaces(); repo.refreshStandings() }

    val next = races.firstOrNull { r -> raceInstant(r)?.isAfter(Instant.now()) == true }
    val d = Duration.between(Instant.now(), raceInstant(next ?: return@HomeScreen) ?: Instant.now())

    LazyColumn(Modifier.fillMaxSize().background(p.canvas).ambient(p.isDark), contentPadding = PaddingValues(bottom = 140.dp)) {
        item {
            Column(Modifier.clickable { onWeekend() }
                .padding(horizontal = F1Dim.screenPad).padding(top = 60.dp)) {
                Text("Formula 1", style = F1Type.body.copy(color = p.sec))
                Spacer(Modifier.height(18.dp))
                Text("Round ${next.round} · Next up", style = F1Type.meta.copy(color = p.sec))
                Spacer(Modifier.height(8.dp))
                // Poster type. Wraps on purpose - a short name gets BIGGER, not narrower.
                Text(next.name.replace(" Grand Prix", "\nGrand Prix"),
                    style = F1Type.display.copy(color = p.txt), lineHeight = 41.sp)
                Spacer(Modifier.height(8.dp))
                Text(next.circuitName, style = F1Type.body.copy(color = p.accent, fontSize = 16.sp))
                Spacer(Modifier.height(28.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                    CountBlock("%02d".format(d.toDays()), "DAYS")
                    CountBlock("%02d".format(d.toHours() % 24), "HOURS")
                    CountBlock("%02d".format(d.toMinutes() % 60), "MINS")
                }
                Spacer(Modifier.height(15.dp))
                Text("Lights out ${next.date} · reminder on", style = F1Type.meta.copy(color = p.sec, fontSize = 11.sp))
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(F1Dim.screenPad).padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("Drivers", style = F1Type.section.copy(color = p.txt))
                Text("View all", style = F1Type.meta.copy(color = p.accent, fontSize = 12.sp),
                    modifier = Modifier.clickable { onStandings() })
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
                    F1Card(Modifier.weight(1f), radius = 22) {
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

@Composable
fun WeekendScreen(repo: Repo) {
    val p = LocalPalette.current
    val races by repo.races().collectAsState(emptyList())
    LaunchedEffect(Unit) { repo.refreshRaces() }
    val next = races.firstOrNull { raceInstant(it)?.isAfter(Instant.now()) == true } ?: return
    var geo by remember(next.circuitId) { mutableStateOf<CircuitGeometryEntity?>(null) }
    LaunchedEffect(next.circuitId) {
        repo.refreshCircuit(next.circuitId)
        geo = repo.circuit(next.circuitId)
    }
    val facts = repo.circuitFacts(next.circuitId)
    Column(Modifier.fillMaxSize().background(p.canvas).ambient(p.isDark).verticalScroll(rememberScrollState())) {
        Column(Modifier.padding(F1Dim.screenPad).padding(top = 60.dp)) {
            Text("Round ${next.round} · Race weekend", style = F1Type.meta.copy(color = p.sec))
            Spacer(Modifier.height(8.dp))
            Text(next.name.replace(" Grand Prix", "\nGrand Prix"),
                style = F1Type.display.copy(color = p.txt), lineHeight = 41.sp)
            Spacer(Modifier.height(8.dp))
            Text(next.circuitName, style = F1Type.body.copy(color = p.accent, fontSize = 16.sp))
            Text("${next.date}${next.country?.let { " · $it" } ?: ""}",
                style = F1Type.meta.copy(color = p.sec, fontSize = 11.sp),
                modifier = Modifier.padding(top = 9.dp))
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

        if (facts != null) {
            Column(Modifier.padding(horizontal = F1Dim.screenPad)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(facts.lengthKm, style = F1Type.hero.copy(color = p.txt, fontSize = 50.sp),
                        maxLines = 1, softWrap = false)
                    Text("KM", style = F1Type.body.copy(color = p.sec),
                        modifier = Modifier.padding(start = 6.dp, bottom = 7.dp))
                }
                Text("Circuit length", style = F1Type.meta.copy(color = p.sec, fontSize = 11.sp),
                    modifier = Modifier.padding(top = 5.dp))
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    CircuitStat("${facts.laps}", "", "No. of Laps", Modifier.weight(1f))
                    CircuitStat("${g?.corners ?: 0}", "", "Turns", Modifier.weight(1f))
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    CircuitStat(facts.topSpeed, "KM/H", "Top Speed", Modifier.weight(1f))
                    CircuitStat(facts.elevation, "M", "Elevation", Modifier.weight(1f))
                }
            }
            Text("Race Weekend", style = F1Type.section.copy(color = p.txt),
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 34.dp, bottom = 6.dp))
            Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)) {
                SPA_SESSIONS.forEach { SessionRow(it) }
            }
        }
        Spacer(Modifier.height(140.dp))
    }
}

private data class Session(val day: String, val dow: String, val name: String, val time: String, val race: Boolean)

/** Belgian GP session times, pinned to match the spec. Real times live in the race's
 *  session sub-objects (FirstPractice/Qualifying/…); wire those in when other circuits land. */
private val SPA_SESSIONS = listOf(
    Session("17", "FRI", "Practice 1", "12:30 – 13:30", false),
    Session("17", "FRI", "Practice 2", "16:00 – 17:00", false),
    Session("18", "SAT", "Practice 3", "11:30 – 12:30", false),
    Session("18", "SAT", "Qualifying", "15:00 – 16:00", false),
    Session("19", "SUN", "Race", "15:00 · lights out", true),
)

@Composable
private fun CircuitStat(value: String, unit: String, label: String, modifier: Modifier = Modifier) {
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
    Column(Modifier.clickable { onClick(s.driverId) }) {
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
fun StandingsScreen(repo: Repo, onDriver: (String) -> Unit) {
    val p = LocalPalette.current
    val drivers by repo.drivers().collectAsState(emptyList())
    LazyColumn(Modifier.fillMaxSize().background(p.canvas).ambient(p.isDark), contentPadding = PaddingValues(bottom = 140.dp)) {
        item {
            Column(Modifier.padding(F1Dim.screenPad).padding(top = 60.dp)) {
                Text("2026 · Standings", style = F1Type.meta.copy(color = p.sec))
                Spacer(Modifier.height(8.dp))
                Text("Standings", style = F1Type.display.copy(color = p.txt))
            }
        }
        item {
            F1Card(Modifier.padding(horizontal = 20.dp)) {
                drivers.forEachIndexed { i, s -> DriverRow(repo, s, onDriver, i < drivers.lastIndex) }
            }
        }
    }
}

@Composable
fun DriverScreen(repo: Repo, driverId: String) {
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
            // Number runs BIG (250sp) so it spills past the head silhouette on both
            // sides - that overspill is what reads as "behind the figure", not the
            // photo's position. Canvas pulled up (top 20dp) to sit above the face.
            CarbonNumber("${d.number ?: 0}", col,
                Modifier.fillMaxWidth().height(300.dp).align(Alignment.TopCenter).padding(top = 20.dp),
                sizeSp = 250f)
            if (key != null) {
                // The mask goes on a WRAPPER, not on AsyncImage: Coil composites its
                // painter in its own layer, so a DstIn rect on the image's own modifier
                // never touches its pixels. As the Box's child, the photo is pulled into
                // the offscreen layer by drawContent() and the fade actually bites. Square
                // box so the square headshot fills it with the chest at the box's bottom
                // (no letterbox), then the whole thing is masked and lifted 90dp.
                Box(Modifier.size(300.dp).align(Alignment.BottomCenter)
                    .offset(y = (-90).dp).fadeBottom()) {
                    // Original colour. Team identity comes from the surroundings, not the person.
                    AsyncImage(
                        model = Net.headshot(key), contentDescription = "${d.given} ${d.family}",
                        contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()
                    )
                }
            }
            // Pinned to ~46% of the hero (not centre, which is mid-torso in a 520dp
            // box) so POS/PTS sit at the shoulder line, flanking the face.
            Column(Modifier.align(BiasAlignment(-1f, -0.08f)).padding(start = 14.dp).wrapContentWidth(),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("%02d".format(d.position), style = F1Type.display.copy(color = p.txt, fontSize = 42.sp),
                    maxLines = 1, softWrap = false)
                Text("POS", style = F1Type.label.copy(color = p.sec))
            }
            Column(Modifier.align(BiasAlignment(1f, -0.08f)).padding(end = 14.dp).wrapContentWidth(),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${d.points}", style = F1Type.display.copy(color = p.txt, fontSize = 42.sp),
                    maxLines = 1, softWrap = false)
                Text("PTS", style = F1Type.label.copy(color = p.sec))
            }
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("2026 SEASON · ${d.team.uppercase()}", style = F1Type.label.copy(color = p.sec, fontSize = 11.sp))
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
        Spacer(Modifier.height(24.dp))
    }
}

/** Points per round as a bar chart (ported from f1-tab-complete.html). Team colour,
 *  height and opacity both scaled by value so a big haul reads loud and a blank reads
 *  faint. Zero-point rounds still get a hairline so the round grid stays legible. */
@Composable
private fun PointsByRound(csv: String, col: Color) {
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
                    Text("${i + 1}", style = F1Type.label.copy(color = p.sec, fontSize = 9.sp))
                }
            }
        }
    }
}
