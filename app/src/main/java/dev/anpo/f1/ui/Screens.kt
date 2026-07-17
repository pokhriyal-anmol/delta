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
fun HomeScreen(repo: Repo, onDriver: (String) -> Unit, onStandings: () -> Unit) {
    val p = LocalPalette.current
    val drivers by repo.drivers().collectAsState(emptyList())
    val teams by repo.teams().collectAsState(emptyList())
    val races by repo.races().collectAsState(emptyList())
    val err by repo.error.collectAsState()

    LaunchedEffect(Unit) { repo.refreshRaces(); repo.refreshStandings() }

    val next = races.firstOrNull { r -> raceInstant(r)?.isAfter(Instant.now()) == true }
    val d = Duration.between(Instant.now(), raceInstant(next ?: return@HomeScreen) ?: Instant.now())

    LazyColumn(Modifier.fillMaxSize().background(p.canvas), contentPadding = PaddingValues(bottom = 110.dp)) {
        item {
            Column(Modifier.padding(horizontal = F1Dim.screenPad).padding(top = 60.dp)) {
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
    LazyColumn(Modifier.fillMaxSize().background(p.canvas), contentPadding = PaddingValues(bottom = 110.dp)) {
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
    LaunchedEffect(driverId) { s = repo.driver(driverId) }
    val d = s ?: return
    val col = teamColour(repo.teamColour(d.teamId))
    val key = repo.headshotKey(d.driverId)

    Column(Modifier.fillMaxSize().background(p.canvas).verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(430.dp)) {
            CarbonNumber("${d.number ?: 0}", col, Modifier.fillMaxWidth().height(280.dp).align(Alignment.TopCenter).padding(top = 40.dp))
            if (key != null) {
                // Original colour. Team identity comes from the surroundings, not the person.
                AsyncImage(
                    model = Net.headshot(key), contentDescription = "${d.given} ${d.family}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.width(310.dp).height(352.dp).align(Alignment.BottomCenter).padding(bottom = 44.dp)
                )
            }
            Column(Modifier.align(Alignment.CenterStart).padding(start = 14.dp).width(66.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("%02d".format(d.position), style = F1Type.display.copy(color = p.txt, fontSize = 42.sp))
                Text("POS", style = F1Type.label.copy(color = p.sec))
            }
            Column(Modifier.align(Alignment.CenterEnd).padding(end = 14.dp).width(66.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${d.points}", style = F1Type.display.copy(color = p.txt, fontSize = 42.sp))
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
        F1Card(Modifier.padding(horizontal = 20.dp).padding(top = 22.dp), radius = 26) {
            Row(Modifier.padding(vertical = 17.dp, horizontal = 10.dp)) {
                StatBlock("%02d".format(d.wins), "WINS", Modifier.weight(1f))
                StatBlock("%02d".format(d.position), "POS", Modifier.weight(1f))
                StatBlock("${d.points}", "PTS", Modifier.weight(1f))
                StatBlock(d.code ?: "-", "CODE", Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(120.dp))
    }
}
