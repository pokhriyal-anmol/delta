package dev.anpo.f1.ui

import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.border
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.animateDpAsState
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
import dev.anpo.f1.R
import dev.anpo.f1.data.*
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
@OptIn(ExperimentalMaterial3Api::class)
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
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val tick = rememberTick()
    val listState = rememberLazyListState()
    val collapsed by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 160 } }
    val pull = rememberPullToRefreshState()
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

    Box(Modifier.fillMaxSize().background(p.canvas)) {
        PullToRefreshBox(
            isRefreshing = refreshing, state = pull, modifier = Modifier.fillMaxSize(),
            onRefresh = {
                tick()
                scope.launch {
                    refreshing = true
                    if (live) repo.refreshStandings(force = true) else if (season != null) repo.refreshArchive(season)
                    refreshing = false
                }
            },
            indicator = {
                PullToRefreshDefaults.Indicator(state = pull, isRefreshing = refreshing, modifier = Modifier.align(Alignment.TopCenter).padding(top = 36.dp),
                    containerColor = p.nav, color = p.accent)
            }
        ) {
            LazyColumn(Modifier.fillMaxSize().ambient(p.isDark), listState, contentPadding = PaddingValues(bottom = 140.dp)) {
                item {
                    Column(Modifier.padding(F1Dim.screenPad).padding(top = 60.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(subtitle, style = F1Type.meta.copy(color = p.sec))
                            SeasonChip(season.orEmpty()) { menu = true }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("World\nChampionship", style = F1Type.display.copy(color = p.txt, fontSize = 42.sp), lineHeight = 43.sp)
                        Spacer(Modifier.height(20.dp))
                        Toggle(tab) { tab = it }
                    }
                }
                item {
                    F1Card(Modifier.padding(horizontal = 20.dp)) {
                        val early = tab == KIND_TEAM && (season?.toIntOrNull() ?: Int.MAX_VALUE) < FIRST_CONSTRUCTORS_SEASON
                        when {
                            lines.isNotEmpty() -> lines.forEachIndexed { i, l ->
                                StandingLine(repo, l, if (tab == KIND_DRIVER) onDriver else onTeam, showDivider = i < lines.lastIndex, index = i)
                            }
                            loading || (live && season == null) ->
                                // While the first frame has no rows the card is kept tall: Compose clamps a restored scroll
                                // offset to the content height, so a short card sent the list back to the top after a tab switch.
                                Column(Modifier.heightIn(min = if (live && season == null) 1800.dp else 0.dp)) { SkeletonRows(8) }
                            early -> StateMessage("No constructors' championship", "There was none before $FIRST_CONSTRUCTORS_SEASON.")
                            live -> StateMessage("No standings yet", "They appear after the first race.")
                            else -> StateMessage("Couldn't load $season", "Check your connection.", "Try again") {
                                scope.launch { loading = true; repo.refreshArchive(season!!); loading = false }
                            }
                        }
                    }
                }
            }
        }
        CompactTitleBar("World Championship", collapsed, Modifier.align(Alignment.TopCenter))
    }
    if (menu) SeasonSheet(current?.toIntOrNull(), season, { menu = false }) { year ->
        tick()
        picked = if (year.toString() == current) null else year.toString()
        menu = false
    }
}

/** Same look as the year chip on Home. The tap area is 48 dp tall, taller than the pill. */
@Composable
private fun SeasonChip(season: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(Modifier.heightIn(min = 48.dp).pressable(role = Role.Button, onClickLabel = "Choose season", onClick = onClick), contentAlignment = Alignment.Center) {
        Row(Modifier.clip(CircleShape).background(p.nav).padding(start = 13.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(season, style = F1Type.body.copy(color = p.txt, fontSize = 12.sp))
            Icon(painterResource(R.drawable.ic_chevron_down), contentDescription = null, tint = p.sec, modifier = Modifier.size(13.dp))
        }
    }
}

/** Every season from the newest back to 1950 as a grid in a bottom sheet. Drag it down to dismiss. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeasonSheet(newest: Int?, selected: String?, onClose: () -> Unit, onPick: (Int) -> Unit) {
    val p = LocalPalette.current
    val years = remember(newest) { ((newest ?: FIRST_SEASON) downTo FIRST_SEASON).toList() }
    ModalBottomSheet(
        onDismissRequest = onClose, containerColor = p.card, scrimColor = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = { Box(Modifier.padding(vertical = 12.dp).size(width = 36.dp, height = 4.dp).clip(CircleShape).background(p.chip)) }
    ) {
        Text("Season", style = F1Type.section.copy(color = p.txt), modifier = Modifier.padding(start = 20.dp, bottom = 12.dp))
        LazyVerticalGrid(GridCells.Fixed(4), Modifier.fillMaxWidth().heightIn(max = 420.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(years) { y ->
                val on = y.toString() == selected
                Box(Modifier.height(48.dp).clip(RoundedCornerShape(14.dp)).background(if (on) p.accent else p.chip).pressable { onPick(y) },
                    contentAlignment = Alignment.Center) {
                    Text("$y", style = F1Type.body.copy(color = if (on) Color.White else p.txt, fontSize = 13.sp))
                }
            }
        }
    }
}

@Composable
private fun Toggle(selected: Int, onSelect: (Int) -> Unit) = Segments(listOf("Drivers", "Constructors"), selected, onSelect)

/** Equal segments on a frosted track. A raised thumb slides between them with a small overshoot. */
@Composable
internal fun Segments(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val p = LocalPalette.current
    val motion = rememberMotionEnabled()
    val tick = rememberTick()
    BoxWithConstraints(Modifier.fillMaxWidth().height(48.dp).clip(CircleShape).background(p.chip.copy(alpha = 0.7f))) {
        val w = maxWidth / options.size
        val x by animateDpAsState(w * selected, if (motion) spring(dampingRatio = 0.72f, stiffness = 400f) else snap(), label = "thumb")
        Box(Modifier.offset(x = x).width(w).fillMaxHeight().padding(4.dp).clip(CircleShape).background(p.nav)
            .border(0.5.dp, litEdge(p.isDark), CircleShape))
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, label ->
                val on = i == selected
                val fg by animateColorAsState(if (on) p.txt else p.sec, tween(160, easing = EaseOut), label = "seg-fg")
                Box(Modifier.weight(1f).fillMaxHeight().clickable(role = Role.Tab, onClickLabel = label) { if (!on) tick(); onSelect(i) },
                    contentAlignment = Alignment.Center) {
                    Text(label, style = F1Type.body.copy(color = fg, fontSize = 13.sp))
                }
            }
        }
    }
}

/** Mirrors the Home driver row so the table reads as the same family. Team colour is data, never chrome. */
@Composable
private fun StandingLine(repo: Repo, l: Line, onOpen: (String) -> Unit, showDivider: Boolean, index: Int) {
    val p = LocalPalette.current
    Column(Modifier.arrive(index).pressable(enabled = l.opens) { onOpen(l.id) }) {
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
