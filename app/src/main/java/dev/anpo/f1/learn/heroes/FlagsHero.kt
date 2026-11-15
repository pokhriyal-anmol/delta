package dev.anpo.f1.learn.heroes

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anpo.f1.ui.F1Type
import dev.anpo.f1.ui.LocalPalette
import dev.anpo.f1.ui.pressable
import dev.anpo.f1.ui.rememberMotionEnabled
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private enum class Kind { SOLID, STRIPED, SPLIT, CHECKER }
private class Flag(val name: String, val kind: Kind, val colour: Color, val meaning: String, val detail: String)

private val RED = Color(0xFFD71921)
private val INK = Color(0xFF111111)
private val PAPER = Color(0xFFF2F2F2)

/** Flag colours are data: they are what each flag looks like on the track, never app chrome. */
private val FLAGS = listOf(
    Flag("Green", Kind.SOLID, Color(0xFF1FA24A), "The hazard is behind you.", "Back to full racing. Shown after a yellow or at the start of a session."),
    Flag("Yellow", Kind.SOLID, Color(0xFFF5C400), "Danger ahead. Slow down.", "No overtaking. Two waved flags means be ready to stop."),
    Flag("Yellow and red", Kind.STRIPED, Color(0xFFF5C400), "Slippery surface ahead.", "Oil or water on the track. Take care."),
    Flag("Blue", Kind.SOLID, Color(0xFF2F6FDE), "A faster car is behind you.", "Let it past. Shown mostly to cars about to be lapped."),
    Flag("White", Kind.SOLID, PAPER, "A slow vehicle on track.", "Usually a car going slowly or a service vehicle."),
    Flag("Red", Kind.SOLID, RED, "Session stopped.", "Every car returns to the pit lane."),
    Flag("Black and white", Kind.SPLIT, PAPER, "A warning.", "For unsporting driving. Think of it as a yellow card."),
    Flag("Black", Kind.SOLID, Color(0xFF000000), "Driver disqualified.", "Return to the pits. Black with an orange circle means your car has a fault."),
    Flag("Chequered", Kind.CHECKER, PAPER, "The session is over.", "Shown to the winner first, then to every car as it finishes."),
)

private const val WAVE_STEPS = 28

/** One large waving flag at a time. Swipe it, or tap the rack below. Meanings come from secondary sources, named in the lesson. */
@Composable
fun FlagsHero() {
    val pager = rememberPagerState { FLAGS.size }
    val scope = rememberCoroutineScope()
    HeroFrame {
        HorizontalPager(pager, Modifier.fillMaxWidth(), pageSpacing = 16.dp) { i -> FlagPage(FLAGS[i]) }
        Spacer(Modifier.height(10.dp))
        FLAGS.chunked(5).forEachIndexed { row, group ->
            Row(Modifier.fillMaxWidth()) {
                group.forEachIndexed { c, f ->
                    val i = row * 5 + c
                    Chip(f, pager.currentPage == i) { scope.launch { pager.animateScrollToPage(i) } }
                }
                repeat(5 - group.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun FlagPage(f: Flag) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "${f.name} flag. ${f.meaning} ${f.detail}" }) {
        WavingFlag(f, Modifier.fillMaxWidth().height(170.dp))
        Spacer(Modifier.height(6.dp))
        Text(f.name, style = F1Type.display.copy(color = p.txt), lineHeight = 44.sp)
        Spacer(Modifier.height(6.dp))
        Text(f.meaning, style = F1Type.stat.copy(color = p.txt), lineHeight = 25.sp)
        Spacer(Modifier.height(4.dp))
        Text(f.detail, style = F1Type.meta.copy(color = p.sec), lineHeight = 17.sp)
    }
}

@Composable
private fun WavingFlag(f: Flag, modifier: Modifier) {
    val p = LocalPalette.current
    val motion = rememberMotionEnabled()
    val phase = if (motion) rememberInfiniteTransition(label = "wave")
        .animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart), label = "phase").value
    else 0.6f
    Canvas(modifier) {
        val pole = 4.dp.toPx(); val left = 14.dp.toPx(); val top = 18.dp.toPx()
        val body = Rect(left, top, size.width - 8.dp.toPx(), top + size.height * 0.62f)
        drawRoundRect(p.sec, Offset(left - 10.dp.toPx(), 0f), Size(pole, size.height), CornerRadius(2.dp.toPx()))
        val cloth = wavePath(body, phase, 9.dp.toPx())
        clipPath(cloth) {
            paintFlag(f, body.inflate(30.dp.toPx()))
            drawRect(shade, body.topLeft, body.size)
        }
        drawPath(cloth, p.txt.copy(alpha = 0.18f), style = Stroke(1.dp.toPx()))
    }
}

private val shade = Brush.horizontalGradient(
    0f to Color.Black.copy(alpha = 0.18f), 0.25f to Color.White.copy(alpha = 0.10f), 0.5f to Color.Black.copy(alpha = 0.16f),
    0.75f to Color.White.copy(alpha = 0.10f), 1f to Color.Black.copy(alpha = 0.20f),
)

/** The outline of a flag whose free edge ripples more than the edge on the pole. */
private fun wavePath(r: Rect, phase: Float, amp: Float): Path {
    fun dy(t: Float) = amp * t * sin(t * 2f * PI.toFloat() * 1.15f - phase)
    return Path().apply {
        moveTo(r.left, r.top + dy(0f))
        for (s in 1..WAVE_STEPS) { val t = s / WAVE_STEPS.toFloat(); lineTo(r.left + r.width * t, r.top + dy(t)) }
        for (s in WAVE_STEPS downTo 0) { val t = s / WAVE_STEPS.toFloat(); lineTo(r.left + r.width * t, r.bottom + dy(t)) }
        close()
    }
}

private fun DrawScope.paintFlag(f: Flag, r: Rect) {
    drawRect(f.colour, r.topLeft, r.size)
    when (f.kind) {
        Kind.SOLID -> Unit
        Kind.STRIPED -> { val step = 18.dp.toPx(); var x = r.left; while (x < r.right) { drawRect(RED, Offset(x + step * 0.3f, r.top), Size(step * 0.34f, r.height)); x += step } }
        Kind.SPLIT -> drawPath(Path().apply { moveTo(r.right, r.top); lineTo(r.right, r.bottom); lineTo(r.left, r.bottom); close() }, INK)
        Kind.CHECKER -> { val s = r.height / 6f; var y = r.top; var row = 0
            while (y < r.bottom) { var x = r.left; var col = row % 2
                while (x < r.right) { if (col % 2 == 0) drawRect(INK, Offset(x, y), Size(s, s)); x += s; col++ }
                y += s; row++ } }
    }
}

/** A tappable swatch in a 48dp cell, so the nine flags are reachable by touch and by TalkBack. */
@Composable
private fun RowScope.Chip(f: Flag, on: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(Modifier.weight(1f).heightIn(min = 48.dp).pressable(role = Role.Button, onClickLabel = "Show the ${f.name} flag", onClick = onClick)
        .semantics { contentDescription = "${f.name} flag"; selected = on }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(34.dp, 22.dp).then(if (on) Modifier.border(2.dp, p.accent, RoundedCornerShape(3.dp)) else Modifier.border(1.dp, p.sec.copy(alpha = 0.4f), RoundedCornerShape(3.dp)))) {
            paintFlag(f, Rect(Offset.Zero, size))
        }
    }
}
