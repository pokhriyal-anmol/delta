package dev.anpo.f1.learn.heroes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anpo.f1.learn.racePoints
import dev.anpo.f1.learn.schumacherSeasons
import dev.anpo.f1.ui.F1Type
import dev.anpo.f1.ui.LocalPalette
import java.text.NumberFormat
import java.util.Locale

/** The podium and the cut-short bars come from racePoints. The re-score numbers come from the bundled season table. */
@Composable
fun PointsHero() {
    HeroFrame {
        Podium()
        Spacer(Modifier.height(26.dp))
        CutShort()
        Spacer(Modifier.height(30.dp))
        Rescore()
    }
}

@Composable
private fun Podium() {
    val values = listOf(2, 1, 3).map { it to racePoints(it, 1.0) }
    Row(Modifier.fillMaxWidth().height(170.dp).semantics(mergeDescendants = true) {
        contentDescription = "A win is ${racePoints(1, 1.0)} points, second ${racePoints(2, 1.0)}, third ${racePoints(3, 1.0)}."
    }, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        values.forEach { (pos, pts) -> Step(pos, pts) }
    }
}

@Composable
private fun RowScope.Step(pos: Int, pts: Int) {
    val p = LocalPalette.current
    val top = pos == 1
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$pts", style = F1Type.display.copy(color = if (top) p.accent else p.txt))
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height((pts * 4.2f).dp).background(if (top) p.txt else p.chip, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)), contentAlignment = Alignment.TopCenter) {
            Text("P$pos", style = F1Type.stat.copy(color = if (top) p.canvas else p.sec), modifier = Modifier.padding(top = 8.dp))
        }
    }
}

private val CUTS = listOf("Under 25%" to 0.1, "25% to 50%" to 0.4, "50% to 75%" to 0.6, "75% or more" to 0.9)

@Composable
private fun CutShort() {
    val p = LocalPalette.current
    HeroLabel("If a race is cut short")
    Text("The winner's points, by how much of the distance the leader completed.", style = F1Type.meta.copy(color = p.sec, fontSize = 11.sp), modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
    val big = LocalDensity.current.fontScale >= 1.3f
    CUTS.forEach { (label, share) ->
        val pts = racePoints(1, share)
        val desc = "$label of the distance: $pts points for the winner"
        Column(Modifier.fillMaxWidth().padding(vertical = 5.dp).semantics(mergeDescendants = true) { contentDescription = desc }) {
            if (big) Text(label, style = F1Type.meta.copy(color = p.sec, fontSize = 12.sp))
            Row(Modifier.fillMaxWidth().heightIn(min = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!big) Text(label, style = F1Type.meta.copy(color = p.sec, fontSize = 12.sp), modifier = Modifier.width(92.dp))
                Box(Modifier.weight(1f)) { Box(Modifier.fillMaxWidth(pts / 25f).height(8.dp).background(p.txt, RoundedCornerShape(4.dp))) }
                Text("$pts", style = F1Type.stat.copy(color = p.txt, textAlign = TextAlign.End), softWrap = false, modifier = Modifier.padding(start = 10.dp))
            }
        }
    }
}

@Composable
private fun Rescore() {
    val p = LocalPalette.current
    val fmt = NumberFormat.getIntegerInstance(Locale.US)
    val today = schumacherSeasons.sumOf { it.today }
    val official = schumacherSeasons.sumOf { it.official }
    HeroLabel("What if")
    Spacer(Modifier.height(4.dp))
    Text("Schumacher, re-scored", style = F1Type.meta.copy(color = p.sec))
    Text(fmt.format(today), style = F1Type.hero.copy(color = p.txt), modifier = Modifier.padding(top = 6.dp))
    Text("points on today's table. He was credited with ${fmt.format(official)}.", style = F1Type.meta.copy(color = p.sec), modifier = Modifier.padding(top = 2.dp))
    Spacer(Modifier.height(14.dp))
    SeasonChart()
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        val s = schumacherSeasons
        Text("${s.first().year}", style = F1Type.meta.copy(color = p.sec, fontSize = 11.sp))
        Text("2002: ${s.first { it.year == 2002 }.official} becomes ${s.first { it.year == 2002 }.today}", style = F1Type.meta.copy(color = p.txt, fontSize = 11.sp, fontWeight = FontWeight.Bold))
        Text("${s.last().year}", style = F1Type.meta.copy(color = p.sec, fontSize = 11.sp))
    }
    Text("Pale bar: what he scored then. Dark bar: the same finishes today. A thought experiment: finishing positions only, no sprints, and he raced 14 to 19 times a year, not 24.",
        style = F1Type.meta.copy(color = p.sec, fontSize = 11.sp), lineHeight = 16.sp, modifier = Modifier.padding(top = 10.dp))
}

@Composable
private fun SeasonChart() {
    val p = LocalPalette.current
    val max = schumacherSeasons.maxOf { it.today }.toFloat()
    Canvas(Modifier.fillMaxWidth().height(120.dp).clearAndSetSemantics { }) {
        val group = size.width / schumacherSeasons.size
        val bar = group * 0.38f
        schumacherSeasons.forEachIndexed { i, s ->
            val x = i * group + group * 0.1f
            val ho = size.height * s.official / max; val ht = size.height * s.today / max
            drawRoundRect(p.sec.copy(alpha = 0.45f), Offset(x, size.height - ho), Size(bar, ho), CornerRadius(2.dp.toPx()))
            drawRoundRect(if (s.year == 2002) p.accent else p.txt, Offset(x + bar + 1.dp.toPx(), size.height - ht), Size(bar, ht), CornerRadius(2.dp.toPx()))
        }
    }
}
