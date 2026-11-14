package io.github.pokhriyal_anmol.f1.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pokhriyal_anmol.f1.R
import io.github.pokhriyal_anmol.f1.data.*
import kotlinx.coroutines.flow.flowOf

/** One table row, whichever table and season it came from. */
private data class Line(
    val id: String, val position: Int, val title: String, val subtitle: String,
    val wins: Int, val points: String, val teamId: String,
    val opens: Boolean,        // a past driver or team has no page of its own until it is in the current season
    val champion: Boolean,
)

/**
 * The World Championship: a Drivers and Constructors toggle, a season selector back to 1950, and rows
 * that open the driver or team page. The current season is live; every earlier one is a final table
 * fetched once and kept forever. Toggle and season survive switching tabs.
 */
@Composable
fun ChampionshipScreen(repo: Repo, onDriver: (String) -> Unit, onTeam: (String) -> Unit) {
    val p = LocalPalette.current
    val drivers by repo.drivers().collectAsState(emptyList())
    val teams by repo.teams().collectAsState(emptyList())
    val races by repo.races().collectAsState(emptyList())
    val current = races.firstOrNull()?.season ?: drivers.firstOrNull()?.season
    var picked by rememberSaveable { mutableStateOf<String?>(null) }   // null means the current season
    var tab by rememberSaveable { mutableStateOf(KIND_DRIVER) }
    var menu by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val season = picked ?: current
    val live = picked == null || picked == current

    LaunchedEffect(live) { if (live) repo.refreshStandings() }
    LaunchedEffect(season, live) {
        if (!live && season != null) { loading = true; repo.refreshArchive(season); loading = false }
    }

    val archive by remember(season, tab, live) {
        if (!live && season != null) repo.archive(season, tab) else flowOf(emptyList())
    }.collectAsState(emptyList())

    val round = drivers.firstOrNull()?.round?.toIntOrNull()
    val final = if (live) isSeasonFinal(round, races.size, races.lastOrNull()?.let { utcMillis(it.date, it.time) },
        System.currentTimeMillis()) else true
    val lines: List<Line> = when {
        live && tab == KIND_DRIVER -> drivers.map {
            Line(it.driverId, it.position, it.family, it.team, it.wins, "${it.points}", it.teamId, true, final && it.position == 1)
        }
        live -> teams.map { t ->
            Line(t.teamId, t.position, t.name, drivers.filter { it.teamId == t.teamId }.joinToString(", ") { it.family },
                t.wins, "${t.points}", t.teamId, true, final && t.position == 1)
        }
        else -> {
            val known = if (tab == KIND_DRIVER) drivers.map { it.driverId }.toSet() else teams.map { it.teamId }.toSet()
            archive.map { Line(it.entityId, it.position, it.title, it.subtitle, it.wins, formatPoints(it.points),
                it.teamId.ifEmpty { it.entityId }, it.entityId in known, it.position == 1) }
        }
    }
    val subtitle = when {
        !live || final -> "Final standings"
        round != null -> "After round $round of ${races.size}"
        else -> ""
    }

    LazyColumn(Modifier.fillMaxSize().background(p.canvas).ambient(p.isDark), contentPadding = PaddingValues(bottom = 140.dp)) {
        item {
            Column(Modifier.padding(F1Dim.screenPad).padding(top = 60.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(subtitle, style = F1Type.meta.copy(color = p.sec))
                    Box {
                        SeasonChip(season.orEmpty()) { menu = true }
                        SeasonMenu(menu, current?.toIntOrNull(), season, { menu = false }) { year ->
                            picked = if (year.toString() == current) null else year.toString()
                            menu = false
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("World\nChampionship", style = F1Type.display.copy(color = p.txt, fontSize = 42.sp), lineHeight = 43.sp)
                Spacer(Modifier.height(20.dp))
                Toggle(tab) { tab = it }
            }
        }
        item {
            F1Card(Modifier.padding(horizontal = 20.dp)) {
                if (lines.isEmpty()) {
                    // While the first frame has no rows, the card is kept tall: Compose clamps a restored scroll
                    // offset to the content height, so a short card sent the list back to the top after a tab switch.
                    val holdScroll = if (live && season == null) 1800.dp else 0.dp
                    Text(emptyMessage(season, tab, live, loading), textAlign = TextAlign.Center,
                        style = F1Type.meta.copy(color = p.sec),
                        modifier = Modifier.fillMaxWidth().heightIn(min = holdScroll).padding(horizontal = 24.dp, vertical = 40.dp))
                }
                lines.forEachIndexed { i, l ->
                    StandingLine(repo, l, if (tab == KIND_DRIVER) onDriver else onTeam, showDivider = i < lines.lastIndex)
                }
            }
        }
    }
}

private fun emptyMessage(season: String?, tab: Int, live: Boolean, loading: Boolean): String = when {
    loading || (live && season == null) -> "Loading the standings…"
    tab == KIND_TEAM && (season?.toIntOrNull() ?: Int.MAX_VALUE) < FIRST_CONSTRUCTORS_SEASON ->
        "There was no constructors' championship before $FIRST_CONSTRUCTORS_SEASON."
    live -> "No standings yet this season."
    else -> "Couldn't load $season. Check your connection and try again."
}

/** Same look as the year chip on Home. */
@Composable
private fun SeasonChip(season: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.clip(CircleShape).background(p.nav).clickable(role = Role.Button, onClickLabel = "Choose season", onClick = onClick)
        .padding(start = 13.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(season, style = F1Type.body.copy(color = p.txt, fontSize = 12.sp))
        Icon(painterResource(R.drawable.ic_chevron_down), contentDescription = null, tint = p.sec, modifier = Modifier.size(13.dp))
    }
}

@Composable
private fun SeasonMenu(open: Boolean, newest: Int?, selected: String?, onClose: () -> Unit, onPick: (Int) -> Unit) {
    val p = LocalPalette.current
    DropdownMenu(open, onClose, Modifier.background(p.card).heightIn(max = 360.dp)) {
        for (year in (newest ?: FIRST_SEASON) downTo FIRST_SEASON) {
            DropdownMenuItem(
                text = { Text("$year", style = F1Type.body.copy(color = if (year.toString() == selected) p.accent else p.txt)) },
                onClick = { onPick(year) })
        }
    }
}

/** Two equal segments in a pill. The selected one is the darker inset, as the bottom bar is. */
@Composable
private fun Toggle(selected: Int, onSelect: (Int) -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().clip(CircleShape).background(p.chip).padding(4.dp)) {
        listOf(KIND_DRIVER to "Drivers", KIND_TEAM to "Constructors").forEach { (kind, label) ->
            val on = kind == selected
            val bg by animateColorAsState(if (on) p.nav else p.chip, tween(160), label = "seg-bg")
            val fg by animateColorAsState(if (on) p.txt else p.sec, tween(160), label = "seg-fg")
            Box(Modifier.weight(1f).clip(CircleShape).background(bg)
                .clickable(role = Role.Tab, onClickLabel = label) { onSelect(kind) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center) {
                Text(label, style = F1Type.body.copy(color = fg, fontSize = 13.sp))
            }
        }
    }
}

/** Mirrors the Home driver row so the table reads as the same family. Team colour is data, never chrome. */
@Composable
private fun StandingLine(repo: Repo, l: Line, onOpen: (String) -> Unit, showDivider: Boolean) {
    val p = LocalPalette.current
    Column(Modifier.clickable(enabled = l.opens) { onOpen(l.id) }) {
        Row(Modifier.heightIn(min = 64.dp).padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${l.position}", style = F1Type.meta.copy(color = p.sec), modifier = Modifier.width(18.dp))
            Box(Modifier.width(4.dp).height(24.dp).clip(RoundedCornerShape(2.dp))
                .background(teamColour(repo.teamColour(l.teamId))))
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(l.title, style = F1Type.body.copy(color = p.txt), maxLines = 1)
                if (l.subtitle.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(l.subtitle, style = F1Type.label.copy(color = p.sec), maxLines = 1)
                }
            }
            if (l.champion) { Text("CHAMPION", style = F1Type.label.copy(color = p.accent)); Spacer(Modifier.width(9.dp)) }
            if (l.wins > 0) { Text("${l.wins}W", style = F1Type.label.copy(color = p.sec)); Spacer(Modifier.width(9.dp)) }
            Text(l.points, style = F1Type.stat.copy(color = p.txt, fontSize = 16.sp))
        }
        if (showDivider) Box(Modifier.fillMaxWidth().height(0.5.dp).background(p.divider))
    }
}
