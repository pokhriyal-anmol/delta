package dev.anpo.f1.learn.heroes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anpo.f1.ui.F1Type
import dev.anpo.f1.ui.LocalPalette
import dev.anpo.f1.ui.pressable

private class Event(val year: String, val title: String, val ban: Boolean, val detail: String)

/** Six moments, oldest first. Red marks a ban or an exclusion. The text agrees with lessons 10 to 12. */
private val EVENTS = listOf(
    Event("1994", "Schumacher: two race ban", true, "At Silverstone he overtook on the formation lap, ignored a stop-and-go and then a black flag. He was disqualified, Benetton was fined $500,000 and he missed Italy and Portugal."),
    Event("1997", "Schumacher: excluded", true, "After the Jerez collision with Villeneuve the FIA threw him out of the whole championship. His race results stood. Wikipedia calls it unprecedented."),
    Event("2012", "Grosjean: one race ban", true, "He caused a first lap crash at Spa and sat out the Italian Grand Prix."),
    Event("2014", "Penalty points begin", false, "The Super Licence started to carry penalty points, and the 5 second time penalty arrived the same year."),
    Event("2024", "Magnussen: banned by points", true, "He reached 12 penalty points at Monza and missed the Azerbaijan Grand Prix. He is the only driver banned this way."),
    Event("2026", "Bortoleto: 80 places", false, "New power unit parts cost 20 places on Friday and 60 more on Saturday at the United States Grand Prix. Over 15 places means starting last."),
)

private val FIRST = 40.dp

/** The gap between corners grows with the user's text size, so the two lines beside a corner never reach the next one. */
@Composable
private fun step(): Dp = 80.dp * LocalDensity.current.fontScale.coerceIn(1f, 1.7f)

private fun cornerY(i: Int, step: Dp): Dp = FIRST + step * i

/** The events laid along a racing line. Tap a corner to read what happened. */
@Composable
fun PenaltiesHero() {
    val p = LocalPalette.current
    var sel by remember { mutableIntStateOf(0) }
    val e = EVENTS[sel]
    HeroFrame {
        Line(sel) { sel = it }
        Spacer(Modifier.height(6.dp))
        Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "${e.year}. ${e.title}. ${e.detail}" }) {
            HeroLabel(e.year)
            Text(e.title, style = F1Type.stat.copy(color = p.txt), lineHeight = 25.sp, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
            Text(e.detail, style = F1Type.body.copy(color = p.txt, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), lineHeight = 22.sp)
        }
    }
}

@Composable
private fun Line(sel: Int, onPick: (Int) -> Unit) {
    val p = LocalPalette.current
    val step = step()
    BoxWithConstraints(Modifier.fillMaxWidth().height(cornerY(EVENTS.size - 1, step) + 70.dp)) {
        val left = maxWidth * 0.19f; val right = maxWidth * 0.81f
        Canvas(Modifier.fillMaxWidth().height(maxHeight).clearAndSetSemantics { }) {
            val path = Path().apply {
                moveTo(size.width / 2f, 0f)
                var x = size.width / 2f; var y = 0f
                EVENTS.indices.forEach { i ->
                    val nx = if (i % 2 == 0) left.toPx() else right.toPx(); val ny = cornerY(i, step).toPx(); val dy = (ny - y) * 0.55f
                    cubicTo(x, y + dy, nx, ny - dy, nx, ny); x = nx; y = ny
                }
                cubicTo(x, y + 36.dp.toPx(), size.width / 2f, y + 24.dp.toPx(), size.width / 2f, y + 70.dp.toPx())
            }
            drawPath(path, p.sec.copy(alpha = 0.3f), style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
            drawPath(path, p.canvas, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 16f))))
        }
        EVENTS.forEachIndexed { i, e ->
            val onLeft = i % 2 == 0
            val x = if (onLeft) left else right
            Box(Modifier.offset(x = x - 24.dp, y = cornerY(i, step) - 24.dp)) { Corner(e, i == sel) { onPick(i) } }
            val w = if (onLeft) maxWidth - x - 34.dp else x - 34.dp
            Column(Modifier.offset(x = if (onLeft) x + 30.dp else 0.dp, y = cornerY(i, step) - 20.dp).width(w)) {
                Text(e.year, style = F1Type.stat.copy(color = if (e.ban) p.accent else p.txt, textAlign = if (onLeft) TextAlign.Start else TextAlign.End), modifier = Modifier.fillMaxWidth())
                Text(e.title, style = F1Type.meta.copy(color = p.sec, fontSize = 11.sp, textAlign = if (onLeft) TextAlign.Start else TextAlign.End), modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun Corner(e: Event, on: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(Modifier.size(48.dp).pressable(role = Role.Button, onClickLabel = "Show ${e.year}", onClick = onClick)
        .semantics { contentDescription = "${e.year}, ${e.title}"; selected = on }, contentAlignment = androidx.compose.ui.Alignment.Center) {
        Canvas(Modifier.size(28.dp)) {
            val c = if (e.ban) p.accent else p.txt
            drawCircle(c, if (on) 9.dp.toPx() else 6.dp.toPx())
            if (on) drawCircle(c, 13.dp.toPx(), style = Stroke(1.5.dp.toPx()))
        }
    }
}
