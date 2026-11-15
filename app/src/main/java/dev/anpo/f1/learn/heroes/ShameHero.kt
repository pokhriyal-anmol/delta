package dev.anpo.f1.learn.heroes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anpo.f1.learn.gridAfterPenalty
import dev.anpo.f1.ui.F1Type
import dev.anpo.f1.ui.LocalPalette

private const val FIELD = 22
private const val QUALIFIED = 13
private const val PLACES = 80

/** Bortoleto at Austin 2026: a giant 80, and the car sent from 13th to the slot gridAfterPenalty gives. Red here is data, a penalty, not chrome. */
@Composable
fun ShameHero() {
    val p = LocalPalette.current
    val start = gridAfterPenalty(QUALIFIED, PLACES, FIELD)
    HeroFrame {
        Column(Modifier.semantics(mergeDescendants = true) { contentDescription = "$PLACES grid places lost, a record. Qualified ${QUALIFIED}th and started in slot $start of $FIELD." }) {
            Text("Austin 2026. Gabriel Bortoleto qualified ${QUALIFIED}th.", style = F1Type.meta.copy(color = p.sec))
            Text("$PLACES", style = F1Type.hero.copy(color = p.accent, fontSize = 120.sp), lineHeight = 120.sp)
            Text("grid places lost. A record.", style = F1Type.stat.copy(color = p.txt))
        }
        Spacer(Modifier.height(14.dp))
        Text("Over 15 places means starting last, whatever the sum says.", style = F1Type.meta.copy(color = p.sec), modifier = Modifier.padding(bottom = 8.dp))
        GridMove(start)
    }
}

private fun slotX(i: Int, w: Float) = if (i % 2 == 0) w * 0.2f else w * 0.6f
private fun slotY(i: Int, rowH: Float) = rowH * (i / 2) + rowH / 2 * (i % 2)

@Composable
private fun GridMove(start: Int) {
    val p = LocalPalette.current
    Canvas(Modifier.fillMaxWidth().height(190.dp).semantics { contentDescription = "" }) {
        val rowH = size.height / 12.4f
        val carW = size.width * 0.2f; val carH = rowH * 0.5f
        repeat(FIELD) { i ->
            val x = slotX(i, size.width); val y = slotY(i, rowH) + 4.dp.toPx()
            val from = i == QUALIFIED - 1; val to = i == start - 1
            when {
                to -> drawRoundRect(p.accent, Offset(x, y), Size(carW, carH), CornerRadius(3.dp.toPx()))
                from -> drawRoundRect(p.accent, Offset(x, y), Size(carW, carH), CornerRadius(3.dp.toPx()), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
                else -> drawRoundRect(p.sec.copy(alpha = 0.35f), Offset(x, y), Size(carW, carH), CornerRadius(3.dp.toPx()))
            }
        }
        val a = Offset(slotX(QUALIFIED - 1, size.width) + carW, slotY(QUALIFIED - 1, rowH) + 4.dp.toPx() + carH / 2)
        val b = Offset(slotX(start - 1, size.width) + carW / 2, slotY(start - 1, rowH) + 4.dp.toPx())
        drawPath(Path().apply { moveTo(a.x, a.y); cubicTo(a.x + 60.dp.toPx(), a.y, b.x + 50.dp.toPx(), b.y - 40.dp.toPx(), b.x, b.y - 2.dp.toPx()) },
            p.accent, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))))
    }
}
