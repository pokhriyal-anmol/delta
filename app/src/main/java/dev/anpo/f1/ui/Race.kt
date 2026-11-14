package dev.anpo.f1.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anpo.f1.data.*
import java.util.Locale

private const val WINNERS_SHOWN = 10

/** Full classification of one round of the current season. */
@Composable
fun ResultScreen(repo: Repo, round: Int, onDriver: (String) -> Unit, onCircuit: (String) -> Unit) {
    val p = LocalPalette.current
    val races by repo.races().collectAsState(emptyList())
    val drivers by repo.drivers().collectAsState(emptyList())
    val race = races.firstOrNull { it.round == round }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { repo.refreshResults(); loading = false }
    if (race == null) { Loading("Loading the calendar…"); return }
    val rows by remember(race.id) { repo.raceResult(race.season, race.round) }.collectAsState(emptyList())
    val known = remember(drivers) { drivers.map { it.driverId }.toSet() }

    Column(Modifier.fillMaxSize().background(p.canvas).ambient(p.isDark).verticalScroll(rememberScrollState())) {
        Column(Modifier.padding(F1Dim.screenPad).padding(top = 60.dp)) {
            Text("Round ${race.round} · ${race.date}", style = F1Type.meta.copy(color = p.sec))
            Spacer(Modifier.height(8.dp))
            Text(race.name.replace(" Grand Prix", "\nGrand Prix"), style = F1Type.display.copy(color = p.txt), lineHeight = 41.sp)
            Spacer(Modifier.height(8.dp))
            Text("${race.circuitName} ›", style = F1Type.body.copy(color = p.accent, fontSize = 16.sp),
                modifier = Modifier.clickable(role = Role.Button, onClickLabel = "Open circuit") { onCircuit(race.circuitId) }
                    .heightIn(min = 48.dp).wrapContentHeight(Alignment.CenterVertically))
        }
        Spacer(Modifier.height(12.dp))
        F1Card(Modifier.padding(horizontal = 20.dp)) {
            if (rows.isEmpty()) {
                if (loading) SkeletonRows(10)
                else StateMessage("Results are not in yet", "They appear within a day of the race.")
            }
            val leaderLaps = rows.maxOfOrNull { it.laps } ?: 0
            rows.forEachIndexed { i, r -> ResultLine(repo, r, leaderLaps, r.driverId in known, onDriver, i < rows.lastIndex, i) }
        }
        Spacer(Modifier.height(140.dp))
    }
}

@Composable
private fun ResultLine(repo: Repo, r: RaceResultEntity, leaderLaps: Int, opens: Boolean, onOpen: (String) -> Unit, divider: Boolean, index: Int) {
    val p = LocalPalette.current
    Column(Modifier.arrive(index).pressable(enabled = opens) { onOpen(r.driverId) }) {
        Row(Modifier.heightIn(min = 64.dp).padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(r.positionText, style = F1Type.meta.copy(color = p.sec), modifier = Modifier.width(22.dp))
            Box(Modifier.width(4.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(teamColour(repo.teamColour(r.teamId))))
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(r.driverName, style = F1Type.body.copy(color = p.txt), maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text("${r.teamName} · ${if (r.grid == 0) "Pit lane" else "Grid ${r.grid}"}", style = F1Type.label.copy(color = p.sec), maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (r.points > 0) Text(formatPoints(r.points), style = F1Type.stat.copy(color = p.txt, fontSize = 16.sp))
                Text(finishText(r, leaderLaps), style = F1Type.label.copy(color = p.sec), maxLines = 1)
            }
        }
        if (divider) Box(Modifier.fillMaxWidth().height(0.5.dp).background(p.divider))
    }
}

/** One circuit: its outline, derived facts and the winner of every race held there. */
@Composable
fun CircuitScreen(repo: Repo, circuitId: String) {
    val p = LocalPalette.current
    val races by repo.races().collectAsState(emptyList())
    val race = races.firstOrNull { it.circuitId == circuitId }
    val winners by remember(circuitId) { repo.winners(circuitId) }.collectAsState(emptyList())
    var geo by remember(circuitId) { mutableStateOf<CircuitGeometryEntity?>(null) }
    var laps by remember(circuitId) { mutableStateOf<Int?>(null) }
    var all by rememberSaveable { mutableStateOf(false) }
    val year = race?.season?.toIntOrNull()
    LaunchedEffect(circuitId, year) {
        if (year != null) { repo.refreshCircuit(circuitId, year); geo = repo.circuit(circuitId) }
        repo.refreshCircuitHistory(circuitId); laps = repo.circuitLaps(circuitId)
    }
    if (race == null) { Loading("Loading the calendar…"); return }

    Column(Modifier.fillMaxSize().background(p.canvas).ambient(p.isDark).verticalScroll(rememberScrollState())) {
        Column(Modifier.padding(F1Dim.screenPad).padding(top = 60.dp)) {
            Text(race.country.orEmpty(), style = F1Type.meta.copy(color = p.sec))
            Spacer(Modifier.height(8.dp))
            Text(race.locality ?: race.circuitName, style = F1Type.display.copy(color = p.txt, fontSize = 42.sp), lineHeight = 43.sp)
            Spacer(Modifier.height(8.dp))
            Text(race.circuitName, style = F1Type.body.copy(color = p.accent, fontSize = 16.sp))
        }
        val g = geo
        if (g != null) Circuit3D(g, Modifier.fillMaxWidth().height(360.dp))
        else Box(Modifier.fillMaxWidth().height(360.dp), contentAlignment = Alignment.Center) {
            Text(if (repo.circuitKey(circuitId) == null) "Circuit map not available yet" else "Loading circuit…",
                style = F1Type.meta.copy(color = p.sec))
        }
        Column(Modifier.padding(horizontal = F1Dim.screenPad)) {
            if (g != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(String.format(Locale.ENGLISH, "%.1f", g.lengthKm),
                        style = F1Type.hero.copy(color = p.txt, fontSize = 50.sp), maxLines = 1, softWrap = false)
                    Text("KM", style = F1Type.body.copy(color = p.sec), modifier = Modifier.padding(start = 6.dp, bottom = 7.dp))
                }
                Text("Circuit length", style = F1Type.meta.copy(color = p.sec, fontSize = 11.sp), modifier = Modifier.padding(top = 5.dp))
                Spacer(Modifier.height(24.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                laps?.let { CircuitStat("$it", "", "No. of Laps", Modifier.weight(1f)) }
                g?.let { CircuitStat("${it.corners}", "", "Turns", Modifier.weight(1f)) }
                if (winners.isNotEmpty()) CircuitStat("${winners.size}", "", "Races held", Modifier.weight(1f))
            }
            mostWins(winners)?.let { (name, n) ->
                Text("Most wins: $name, $n", style = F1Type.meta.copy(color = p.sec, fontSize = 11.sp), modifier = Modifier.padding(top = 16.dp))
            }
        }
        Text("Past winners", style = F1Type.section.copy(color = p.txt),
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 34.dp, bottom = 10.dp))
        F1Card(Modifier.padding(horizontal = 20.dp)) {
            if (winners.isEmpty()) {
                Text("No race has been held here yet.", textAlign = TextAlign.Center, style = F1Type.meta.copy(color = p.sec),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp))
            }
            val shown = if (all) winners else winners.take(WINNERS_SHOWN)
            shown.forEachIndexed { i, w -> WinnerLine(repo, w, i < shown.lastIndex || winners.size > shown.size) }
            if (winners.size > WINNERS_SHOWN) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    PillButton(if (all) "Show fewer" else "Show all ${winners.size}") { all = !all }
                }
            }
        }
        Spacer(Modifier.height(140.dp))
    }
}

@Composable
private fun WinnerLine(repo: Repo, w: CircuitWinnerEntity, divider: Boolean) {
    val p = LocalPalette.current
    Column {
        Row(Modifier.heightIn(min = 56.dp).padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(w.season, style = F1Type.meta.copy(color = p.sec), modifier = Modifier.width(48.dp))
            Column(Modifier.weight(1f)) {
                Text(w.driverName, style = F1Type.body.copy(color = p.txt), maxLines = 1)
                if (w.teamName.isNotEmpty()) Text(w.teamName, style = F1Type.label.copy(color = p.sec), maxLines = 1)
            }
        }
        if (divider) Box(Modifier.fillMaxWidth().height(0.5.dp).background(p.divider))
    }
}

@Composable
private fun Loading(text: String) {
    val p = LocalPalette.current
    Box(Modifier.fillMaxSize().background(p.canvas), contentAlignment = Alignment.Center) {
        Text(text, style = F1Type.meta.copy(color = p.sec))
    }
}
