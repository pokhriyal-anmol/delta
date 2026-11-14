package dev.anpo.f1.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anpo.f1.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Everything the driver and team pages share below their hero: career totals and season history. */

/**
 * The sections below fill in as their data arrives, so for a moment after coming Back to the page it is
 * shorter than when the user left it, and Compose clamps the restored scroll position to the short page.
 * The sections reserve a tall minimum height until that moment has passed.
 */
@Composable
private fun rememberSettled(): Boolean {
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(400); settled = true }
    return settled
}
private val UNSETTLED_HEIGHT = 4000.dp

@Composable
private fun SectionTitle(text: String) {
    val p = LocalPalette.current
    Text(text, style = F1Type.section.copy(color = p.txt),
        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 30.dp, bottom = 13.dp))
}

/** A row that opens another page. The whole width is the tap target. */
@Composable
private fun LinkRow(label: String, value: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    F1Card(Modifier.padding(horizontal = 20.dp).padding(top = 14.dp), radius = 26) {
        Row(Modifier.fillMaxWidth().pressable(role = Role.Button, onClick = onClick).heightIn(min = 56.dp).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = F1Type.meta.copy(color = p.sec), modifier = Modifier.weight(1f))
            Text("$value ›", style = F1Type.body.copy(color = p.accent, fontSize = 14.sp))
        }
    }
}

/** Wins, podiums, poles from the career request; titles appear once every past season is saved. */
@Composable
private fun CareerCard(career: CareerEntity?, titles: Int?, poles: Boolean) {
    val p = LocalPalette.current
    F1Card(Modifier.padding(horizontal = 20.dp), radius = 26) {
        if (career == null) {
            Text("Loading career totals…", style = F1Type.meta.copy(color = p.sec), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp))
        } else Row(Modifier.padding(vertical = 17.dp, horizontal = 10.dp)) {
            StatBlock("${career.wins}", "WINS", Modifier.weight(1f))
            StatBlock("${career.podiums}", "PODIUMS", Modifier.weight(1f))
            if (poles) StatBlock("${career.poles}", "POLES", Modifier.weight(1f))
            if (titles != null) StatBlock("$titles", "TITLES", Modifier.weight(1f))
        }
    }
}

/** Past seasons, newest first. Fetching them all costs one request each, so it waits for a tap. */
@Composable
private fun HistoryCard(repo: Repo, kind: Int, id: String, career: CareerEntity?, current: String?, now: List<String>,
                        history: List<ArchiveStandingEntity>, busy: Boolean, onLoad: () -> Unit) {
    val p = LocalPalette.current
    val missing = missingSeasons(kind, careerSeasons(career), history.map { it.season.toInt() }.toSet(), current?.toIntOrNull())
    F1Card(Modifier.padding(horizontal = 20.dp), radius = 26) {
        Column {
            if (now.size == 3) HistoryLine(now[0], now[1], now[2], false, divider = history.isNotEmpty() || missing.isNotEmpty())
            history.forEachIndexed { i, h ->
                HistoryLine(h.season, if (kind == KIND_DRIVER) h.subtitle else h.title, "P${h.position} · ${formatPoints(h.points)}",
                    h.position == 1, divider = i < history.lastIndex || missing.isNotEmpty())
            }
            if (missing.isNotEmpty()) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    PillButton(if (busy) "Loading seasons…" else "Load ${missing.size} more ${if (missing.size == 1) "season" else "seasons"}" + if (missing.size > 10) " (one request each)" else "",
                        enabled = !busy, onClick = onLoad)
                }
            }
        }
    }
}

@Composable
private fun HistoryLine(season: String, name: String, result: String, champion: Boolean, divider: Boolean) {
    val p = LocalPalette.current
    Column {
        Row(Modifier.heightIn(min = 52.dp).padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(season, style = F1Type.meta.copy(color = p.sec), modifier = Modifier.width(48.dp))
            Text(name, style = F1Type.body.copy(color = p.txt, fontSize = 14.sp), maxLines = 1, modifier = Modifier.weight(1f))
            if (champion) { Text("CHAMPION", style = F1Type.label.copy(color = p.accent)); Spacer(Modifier.width(9.dp)) }
            Text(result, style = F1Type.stat.copy(color = p.txt, fontSize = 14.sp))
        }
        if (divider) Box(Modifier.fillMaxWidth().height(0.5.dp).background(p.divider))
    }
}

@Composable
private fun HeadToHeadCard(h: HeadToHead, me: String) {
    val p = LocalPalette.current
    F1Card(Modifier.padding(horizontal = 20.dp), radius = 26) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(me, style = F1Type.label.copy(color = p.sec), modifier = Modifier.weight(1f))
                Text(h.teammate.substringAfterLast(' '), style = F1Type.label.copy(color = p.sec))
            }
            H2HRow("Finished ahead", h.racesAhead, h.racesBehind)
            H2HRow("Started ahead", h.gridAhead, h.gridBehind)
            Text("Over ${h.rounds} shared ${if (h.rounds == 1) "round" else "rounds"}. Starting slot after penalties stands in for qualifying.",
                style = F1Type.label.copy(color = p.sec, fontSize = 10.sp), modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun H2HRow(label: String, mine: Int, theirs: Int) {
    val p = LocalPalette.current
    fun style(win: Boolean) = F1Type.stat.copy(color = if (win) p.txt else p.sec, fontSize = 22.sp, fontWeight = if (win) FontWeight.Bold else FontWeight.Normal)
    Column(Modifier.padding(top = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("$mine", style = style(mine > theirs), modifier = Modifier.weight(1f))
            Text("$theirs", style = style(theirs > mine))
        }
        Text(label, style = F1Type.label.copy(color = p.sec, fontSize = 10.sp))
    }
}

/** Last five finishes, each opening that race's result. */
@Composable
private fun LastResults(rows: List<RaceResultEntity>, races: List<RaceEntity>, onRace: (Int) -> Unit) {
    val p = LocalPalette.current
    F1Card(Modifier.padding(horizontal = 20.dp), radius = 26) {
        rows.forEachIndexed { i, r ->
            val race = races.firstOrNull { it.round == r.round }
            Column(Modifier.pressable(role = Role.Button) { onRace(r.round) }) {
                Row(Modifier.heightIn(min = 56.dp).padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("R${r.round}", style = F1Type.meta.copy(color = p.sec), modifier = Modifier.width(40.dp))
                    Text(race?.name?.replace(" Grand Prix", " GP") ?: "Round ${r.round}", style = F1Type.body.copy(color = p.txt, fontSize = 14.sp),
                        maxLines = 1, modifier = Modifier.weight(1f))
                    Text(r.positionText.let { if (it.toIntOrNull() != null) "P$it" else it }, style = F1Type.stat.copy(color = p.txt, fontSize = 14.sp))
                    if (r.points > 0) Text("  ${formatPoints(r.points)} pts", style = F1Type.label.copy(color = p.sec))
                }
                if (i < rows.lastIndex) Box(Modifier.fillMaxWidth().height(0.5.dp).background(p.divider))
            }
        }
    }
}

/** What sits under the driver page's stats: team link, career, teammate, recent results, history. */
@Composable
fun DriverDepth(repo: Repo, d: DriverStandingEntity, onTeam: (String) -> Unit, onRace: (Int) -> Unit) {
    val career by remember(d.driverId) { repo.career(KIND_DRIVER, d.driverId) }.collectAsState(null)
    val history by remember(d.driverId) { repo.history(KIND_DRIVER, d.driverId) }.collectAsState(emptyList())
    val results by repo.seasonResults().collectAsState(emptyList())
    val races by repo.races().collectAsState(emptyList())
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(d.driverId) { repo.refreshCareer(KIND_DRIVER, d.driverId) }

    val seasons = careerSeasons(career)
    val complete = career != null && missingSeasons(KIND_DRIVER, seasons, history.map { it.season.toInt() }.toSet(), d.season.toIntOrNull()).isEmpty()
    val settled = rememberSettled()
    Column(Modifier.heightIn(min = if (settled) 0.dp else UNSETTLED_HEIGHT)) {
    LinkRow("Team", d.team) { onTeam(d.teamId) }
    SectionTitle("Career")
    CareerCard(career, if (complete) titles(history) else null, poles = true)
    headToHead(results, d.driverId)?.let { h ->
        SectionTitle("Against ${h.teammate.substringAfterLast(' ')}")
        HeadToHeadCard(h, d.family)
    }
    val last = lastResults(results, d.driverId)
    if (last.isNotEmpty()) { SectionTitle("Recent results"); LastResults(last, races, onRace) }
    SectionTitle("Seasons")
    HistoryCard(repo, KIND_DRIVER, d.driverId, career, d.season, listOf(d.season, d.team, "P${d.position} · ${d.points}"), history, busy) {
        scope.launch { busy = true; repo.refreshHistory(KIND_DRIVER, d.driverId); busy = false }
    }
    Spacer(Modifier.height(140.dp))
    }
}

/** Under the team page's stats: both drivers' points by round summed, career and history. */
@Composable
fun TeamDepth(repo: Repo, t: ConstructorStandingEntity, col: Color) {
    val career by remember(t.teamId) { repo.career(KIND_TEAM, t.teamId) }.collectAsState(null)
    val history by remember(t.teamId) { repo.history(KIND_TEAM, t.teamId) }.collectAsState(emptyList())
    val results by repo.seasonResults().collectAsState(emptyList())
    val races by repo.races().collectAsState(emptyList())
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(t.teamId) { repo.refreshCareer(KIND_TEAM, t.teamId) }

    val settled = rememberSettled()
    Column(Modifier.heightIn(min = if (settled) 0.dp else UNSETTLED_HEIGHT)) {
    val rounds = results.maxOfOrNull { it.round } ?: 0
    if (rounds > 0) { SectionTitle("Points by round"); PointsByRound(teamPointsByRound(results, t.teamId, rounds).joinToString(","), col) }
    SectionTitle("Career")
    CareerCard(career, null, poles = false)
    SectionTitle("Seasons")
    HistoryCard(repo, KIND_TEAM, t.teamId, career, t.season, listOf(t.season, t.name, "P${t.position} · ${t.points}"), history, busy) {
        scope.launch { busy = true; repo.refreshHistory(KIND_TEAM, t.teamId); busy = false }
    }
    Spacer(Modifier.height(140.dp))
    }
}
