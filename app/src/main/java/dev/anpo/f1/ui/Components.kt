package dev.anpo.f1.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

/** The ambient glow. Team colour behind the subject, never on the chrome. */
fun Modifier.glow(colour: Color, alpha: Float = 0.40f, radius: Float = 1f) = this.drawBehind {
    val r = size.minDimension * radius
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(colour.copy(alpha = alpha), colour.copy(alpha = alpha * 0.28f), Color.Transparent),
            center = center, radius = r
        ),
        radius = r, center = center
    )
}

/** Card: borderless, 28dp, three-part shadow. On dark the drop shadow does almost
 *  nothing (nothing darker than #050506 to fall onto) - the lift comes from the
 *  surface being lighter than canvas plus the hairline. That's why light and dark
 *  use very different elevations rather than one value. */
@Composable
fun F1Card(modifier: Modifier = Modifier, radius: Int = 28, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    Column(
        modifier
            .shadow(
                elevation = if (p.isDark) 18.dp else 10.dp,
                shape = RoundedCornerShape(radius.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (p.isDark) 0.9f else 0.12f),
                spotColor = Color.Black.copy(alpha = if (p.isDark) 0.9f else 0.16f),
            )
            .clip(RoundedCornerShape(radius.dp))
            .background(p.card),
        content = content
    )
}

@Composable
fun Pill(text: String, onClick: (() -> Unit)? = null, leading: @Composable (() -> Unit)? = null) {
    val p = LocalPalette.current
    Row(
        Modifier
            .shadow(if (p.isDark) 10.dp else 6.dp, CircleShape, clip = false)
            .clip(CircleShape).background(p.nav)
            .let { if (onClick != null) it.clickable { onClick() } else it }
            .padding(horizontal = 17.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) { leading?.invoke(); Text(text, style = F1Type.body.copy(color = p.txt, fontSize = 13.sp)) }
}

/** Stat row: icon + big value + tiny label. 19 -> 10. Never a list row. */
@Composable
fun StatBlock(value: String, label: String, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = F1Type.stat.copy(color = p.txt, fontSize = 22.sp))
        Spacer(Modifier.height(5.dp))
        Text(label, style = F1Type.label.copy(color = p.sec, fontSize = 9.sp))
    }
}

/**
 * The hero number: carbon fill, glowing stroke, engraved lip.
 *
 * Compose can't background-clip text, so this is done properly: the glyph
 * becomes an outline Path, the carbon is drawn CLIPPED to that path, then the
 * stroke rides the same path. The engrave is the same path filled 3dp lower
 * in a hairline of light - that sliver is the whole illusion.
 *
 * Rib is 6px per the reference. Fine enough that it reads as material rather
 * than as stripes; on a 3x screen each rib gets 18 physical pixels.
 */
@Composable
fun CarbonNumber(number: String, glowColour: Color, modifier: Modifier = Modifier, sizeSp: Float = 186f) {
    val p = LocalPalette.current
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val style = androidx.compose.ui.text.TextStyle(
        fontSize = sizeSp.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
        letterSpacing = (-sizeSp * 0.06f).sp
    )
    Canvas(modifier) {
        val layout = measurer.measure(number, style)
        val x = (size.width - layout.size.width) / 2f
        val y = (size.height - layout.size.height) / 2f

        // Engraved lip: same glyph, 3dp lower, hairline of light (dark in light mode).
        translate(x, y + 3.dp.toPx()) { drawText(layout, color = p.engrave) }

        // Wide team-colour glow, then the tight neutral one.
        drawCircle(
            Brush.radialGradient(listOf(glowColour.copy(alpha = 0.30f), Color.Transparent),
                center = Offset(size.width / 2, size.height / 2), radius = size.width * 0.55f),
            radius = size.width * 0.55f, center = Offset(size.width / 2, size.height / 2)
        )

        // Carbon, clipped to the glyph. 6px rib at 45deg, cross-fibres at 135deg.
        val path = layout.multiParagraph.getPathForRange(0, number.length)
        translate(x, y) {
            clipPath(path) {
                drawRect(if (p.isDark) Color(0xFF0B0B0D) else Color(0xFFE9E9E5))
                val rib = 6.dp.toPx()
                var i = -size.height
                while (i < size.width + size.height) {
                    drawLine(if (p.isDark) Color(0xFF23232B) else Color(0xFFDBDBD5),
                        Offset(i, 0f), Offset(i + size.height, size.height), strokeWidth = rib / 2)
                    drawLine(if (p.isDark) Color(0x12FFFFFF) else Color(0x0E000000),
                        Offset(i + rib, 0f), Offset(i + rib - size.height, size.height), strokeWidth = 1.dp.toPx())
                    i += rib
                }
            }
            drawPath(path, color = p.numStroke, style = Stroke(width = 3.dp.toPx()))
        }
    }
}
