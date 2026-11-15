package dev.anpo.f1.learn.heroes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anpo.f1.learn.knockout
import dev.anpo.f1.ui.F1Type
import dev.anpo.f1.ui.LocalPalette

private const val CARS = 22
private val ROW = 29.dp
private val CAR_W = 44.dp
private val CAR_H = 15.dp
private val TOP = 8.dp

private class Round(val name: String, val minutes: String, val first: Int, val count: Int, val note: String)

/** Where car number i (0 based) sits on the grid: two lanes, the right lane half a row further back. */
private fun carY(i: Int): Dp = TOP + ROW * (i / 2) + ROW / 2 * (i % 2)

/** The 22-car grid with the three qualifying rounds bracketed down the side. Group sizes come from knockout(22). */
@Composable
fun QualifyingHero() {
    val out = remember { knockout(CARS) }
    val q3 = CARS - out.sum()
    val rounds = remember { listOf(
        Round("Q3", "13 min", 0, q3, "top $q3"),
        Round("Q2", "15 min", q3, out[1], "${out[1]} out"),
        Round("Q1", "18 min", q3 + out[1], out[0], "${out[0]} out"),
    ) }
    val label = "Starting grid for $CARS cars. $q3 reach Q3, ${out[1]} are knocked out in Q2 and ${out[0]} in Q1."
    HeroFrame {
        Column(Modifier.semantics { contentDescription = label }) {
            Grid(q3, out, rounds)
            Spacer(Modifier.height(18.dp))
            Rule107()
        }
    }
}

@Composable
private fun Grid(q3: Int, out: List<Int>, rounds: List<Round>) {
    val p = LocalPalette.current
    BoxWithConstraints(Modifier.fillMaxWidth().height(carY(CARS) + 22.dp).clearAndSetSemantics { }) {
        val lane = maxWidth - 78.dp
        Box(Modifier.offset(x = lane * 0.04f).size(lane * 0.92f, carY(CARS) + 16.dp)
            .border(1.dp, p.sec.copy(alpha = 0.3f), RoundedCornerShape(6.dp)))
        repeat(CARS) { i ->
            val x = if (i % 2 == 0) lane * 0.18f else lane * 0.58f
            val fill = when { i == 0 -> p.accent; i < q3 -> p.txt; i < q3 + out[1] -> p.sec; else -> p.canvas }
            Box(Modifier.offset(x = x, y = carY(i)).size(CAR_W, CAR_H).background(fill, RoundedCornerShape(3.dp))
                .then(if (i >= q3 + out[1]) Modifier.border(1.dp, p.sec, RoundedCornerShape(3.dp)) else Modifier))
            Text("P${i + 1}", style = F1Type.label.copy(color = p.sec, fontSize = fixedSp(10.dp)), softWrap = false,
                modifier = Modifier.offset(x = if (i % 2 == 0) x - 28.dp else x + CAR_W + 6.dp, y = carY(i) + 1.dp))
        }
        rounds.forEach { Bracket(it, lane) }
    }
}

@Composable
private fun Bracket(r: Round, lane: Dp) {
    val p = LocalPalette.current
    val top = carY(r.first)
    val bottom = carY(r.first + r.count - 1) + CAR_H
    Box(Modifier.offset(x = lane + 10.dp, y = top).width(1.dp).height(bottom - top).background(p.sec))
    Column(Modifier.offset(x = lane + 18.dp, y = (top + bottom) / 2 - 20.dp)) {
        Text(r.name, style = F1Type.stat.copy(color = p.txt))
        Text(r.minutes, style = F1Type.meta.copy(color = p.sec, fontSize = fixedSp(11.dp)), softWrap = false)
        Text(r.note, style = F1Type.meta.copy(color = p.sec, fontSize = fixedSp(11.dp)), softWrap = false)
    }
}

/** 100 percent is the fastest Q1 lap. The tick at 107 percent is the cut-off. */
@Composable
private fun Rule107() {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().semantics { contentDescription = "The 107 percent rule: a lap slower than 107 percent of the fastest Q1 lap is not classified." }) {
        HeroLabel("The 107 percent rule")
        Spacer(Modifier.height(10.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().height(20.dp).clearAndSetSemantics { }) {
            val fast = maxWidth / 1.07f
            Box(Modifier.offset(y = 6.dp).width(fast).height(8.dp).background(p.txt, RoundedCornerShape(4.dp)))
            Box(Modifier.offset(x = fast, y = 6.dp).width(maxWidth - fast).height(8.dp).background(p.sec.copy(alpha = 0.35f), RoundedCornerShape(4.dp)))
            Box(Modifier.offset(x = maxWidth - 2.dp).width(2.dp).height(20.dp).background(p.accent))
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp).clearAndSetSemantics { }, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("100% is the fastest Q1 lap", style = F1Type.meta.copy(color = p.sec, fontSize = 11.sp))
            Text("107%", style = F1Type.meta.copy(color = p.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold))
        }
    }
}
