package dev.anpo.f1.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anpo.f1.ui.F1Dim
import dev.anpo.f1.ui.F1Type
import dev.anpo.f1.ui.LocalPalette
import dev.anpo.f1.ui.ambient
import dev.anpo.f1.ui.pressable
import java.time.LocalDate

/** The Learn index: the lessons laid out as a staggered starting grid, with pole position as lesson one. Bundled text, no network. */
@Composable
fun LearnScreen(onLesson: (String) -> Unit) {
    val p = LocalPalette.current
    val fact = remember { factOfTheDay(LocalDate.now().dayOfYear) }
    LazyColumn(Modifier.fillMaxSize().background(p.canvas).ambient(p.isDark), contentPadding = PaddingValues(bottom = 140.dp)) {
        item { LearnHeader() }
        item { FactStrip(fact) }
        item { StartingGrid(onLesson) }
    }
}

@Composable
private fun LearnHeader() {
    val p = LocalPalette.current
    Column(Modifier.padding(horizontal = F1Dim.screenPad).padding(top = 60.dp)) {
        Text("Formula 1", style = F1Type.body.copy(color = p.sec))
        Spacer(Modifier.height(8.dp))
        Text("Learn F1", style = F1Type.display.copy(color = p.txt))
        Spacer(Modifier.height(8.dp))
        Text("Pole position is lesson one. ${lessons.size} short lessons, every number sourced.",
            style = F1Type.meta.copy(color = p.sec))
    }
}

@Composable
private fun FactStrip(fact: String) {
    val p = LocalPalette.current
    Column(Modifier.padding(horizontal = F1Dim.screenPad).padding(top = 22.dp)) {
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
        Spacer(Modifier.height(14.dp))
        Text("DID YOU KNOW", style = F1Type.label.copy(color = p.accent, fontSize = 11.sp))
        Spacer(Modifier.height(6.dp))
        Text(fact, style = F1Type.stat.copy(color = p.txt), lineHeight = 25.sp)
    }
}

/** Two columns, the right one dropped like the back row of a grid. Odd lessons on the left, even on the right. */
@Composable
private fun StartingGrid(onLesson: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = F1Dim.screenPad).padding(top = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.weight(1f)) { lessons.filter { it.number % 2 == 1 }.forEach { Slot(it, onLesson) } }
        Column(Modifier.weight(1f).padding(top = 30.dp)) { lessons.filter { it.number % 2 == 0 }.forEach { Slot(it, onLesson) } }
    }
}

/** One grid box, open at the front: a line down the left and along the bottom. Pole is drawn in the accent. */
@Composable
private fun Slot(l: Lesson, onLesson: (String) -> Unit) {
    val p = LocalPalette.current
    val pole = l.number == 1
    val line = if (pole) p.accent else p.sec.copy(alpha = 0.4f)
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)
        .pressable(role = Role.Button, onClickLabel = "Open ${l.title}") { onLesson(l.id) }
        .drawBehind {
            val w = 1.dp.toPx()
            drawLine(line, Offset(0f, 0f), Offset(0f, size.height), w)
            drawLine(line, Offset(0f, size.height), Offset(size.width, size.height), w)
        }
        .heightIn(min = 82.dp).padding(start = 12.dp, top = 8.dp, bottom = 10.dp)) {
        Text("P${l.number}", style = F1Type.label.copy(color = if (pole) p.accent else p.sec, fontSize = 11.sp))
        Spacer(Modifier.height(4.dp))
        Text(l.title, style = F1Type.stat.copy(color = p.txt), lineHeight = 23.sp)
        Spacer(Modifier.height(4.dp))
        Text("${l.minutes} min", style = F1Type.meta.copy(color = p.sec, fontSize = 11.sp))
    }
}
