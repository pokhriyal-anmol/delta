package dev.anpo.f1.learn.heroes

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anpo.f1.ui.F1Type
import dev.anpo.f1.ui.LocalPalette
import dev.anpo.f1.ui.pressable
import dev.anpo.f1.ui.rememberMotionEnabled
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private class Compound(val letter: String, val name: String, val colour: Color, val note: String)

/** The usual colour markings. They come from secondary sources and the lesson names that. Each has a letter and a name too, so colour is never the only cue. */
private val COMPOUNDS = listOf(
    Compound("S", "Soft", Color(0xFFE8002D), "Grips most and wears out fastest."),
    Compound("M", "Medium", Color(0xFFFFD12E), "In between."),
    Compound("H", "Hard", Color(0xFFEDEDED), "Lasts longest and grips least."),
    Compound("I", "Intermediate", Color(0xFF2EAD5B), "For a damp track."),
    Compound("W", "Wet", Color(0xFF2F6FDE), "For a soaking track."),
)

/** One big tyre you can change by tapping a compound on the faster-to-lasts-longer axis, or inter and wet below it. */
@Composable
fun TyresHero() {
    val p = LocalPalette.current
    var sel by remember { mutableIntStateOf(0) }
    val c = COMPOUNDS[sel]
    HeroFrame {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Tyre(c.colour, Modifier.size(210.dp))
            Text(c.letter, style = F1Type.display.copy(color = Color.White))
        }
        Spacer(Modifier.height(8.dp))
        Text(c.name, style = F1Type.display.copy(color = p.txt), lineHeight = 44.sp)
        Text(c.note, style = F1Type.stat.copy(color = p.txt), lineHeight = 25.sp)
        Spacer(Modifier.height(18.dp))
        Axis(sel) { sel = it }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 3..4) Legend(COMPOUNDS[i], sel == i) { sel = i }
        }
    }
}

@Composable
private fun Tyre(colour: Color, modifier: Modifier) {
    val p = LocalPalette.current
    val motion = rememberMotionEnabled()
    val turn = if (motion) rememberInfiniteTransition(label = "spin")
        .animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart), label = "turn").value else 0f
    Canvas(modifier) {
        val r = size.minDimension / 2f
        drawCircle(Color(0xFF111113), r)
        drawCircle(p.sec.copy(alpha = 0.5f), r - 0.5.dp.toPx(), style = Stroke(1.dp.toPx()))
        drawCircle(colour, r * 0.76f, style = Stroke(7.dp.toPx()))
        drawCircle(Color(0xFF0B0B0C), r * 0.58f)
        drawCircle(p.sec.copy(alpha = 0.5f), r * 0.58f, style = Stroke(1.dp.toPx()))
        rotate(turn) {
            for (k in 0 until 8) {
                val a = k * PI.toFloat() / 4f
                drawLine(p.sec.copy(alpha = 0.55f), center + Offset(cos(a), sin(a)) * (r * 0.17f), center + Offset(cos(a), sin(a)) * (r * 0.56f), 1.5.dp.toPx())
            }
        }
        drawCircle(Color(0xFF1B1B1D), r * 0.16f)
    }
}

/** Faster and wears sooner on the left, slower and lasts longer on the right. Soft, medium and hard sit along it, in equal thirds so they never collide. */
@Composable
private fun Axis(sel: Int, onPick: (Int) -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth()) {
            Box(Modifier.offset(y = 24.dp).fillMaxWidth().height(1.dp).background(p.sec.copy(alpha = 0.4f)))
            Row(Modifier.fillMaxWidth()) {
                for (i in 0..2) Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) { Marker(COMPOUNDS[i], sel == i) { onPick(i) } }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Faster, wears out sooner", style = F1Type.meta.copy(color = p.sec, fontSize = 11.sp), modifier = Modifier.weight(1f).padding(end = 8.dp))
            Text("Slower, lasts longer", style = F1Type.meta.copy(color = p.sec, fontSize = 11.sp, textAlign = TextAlign.End), modifier = Modifier.weight(1f).padding(start = 8.dp))
        }
    }
}

@Composable
private fun Marker(c: Compound, on: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.pressable(role = Role.Button, onClickLabel = "Show the ${c.name} tyre", onClick = onClick)
        .semantics { contentDescription = "${c.name} tyre"; selected = on }, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(if (on) 36.dp else 30.dp).clip(CircleShape).background(Color(0xFF111113)), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(if (on) 36.dp else 30.dp)) { drawCircle(c.colour, size.minDimension / 2f - 2.dp.toPx(), style = Stroke(4.dp.toPx())) }
                Text(c.letter, style = F1Type.meta.copy(color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center))
            }
        }
        Text(c.name, style = F1Type.meta.copy(color = if (on) p.txt else p.sec, fontSize = 11.sp))
    }
}

@Composable
private fun RowScope.Legend(c: Compound, on: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.heightIn(min = 48.dp).pressable(role = Role.Button, onClickLabel = "Show the ${c.name} tyre", onClick = onClick)
        .semantics { contentDescription = "${c.name} tyre"; selected = on }, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(c.colour))
        Spacer(Modifier.width(8.dp))
        Text(c.name, style = F1Type.meta.copy(color = if (on) p.txt else p.sec, fontSize = 12.sp))
    }
}
