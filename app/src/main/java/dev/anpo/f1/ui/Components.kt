package dev.anpo.f1.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import android.graphics.BlurMaskFilter
import androidx.compose.ui.text.style.TextAlign
import dev.anpo.f1.data.CircuitGeometryEntity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

/**
 * Ambient accent glow — two red radials bleeding from the edges (design-system §2).
 * Flat chrome, but NOT flat presence: the canvas is black and lit. One large glow off
 * the top-right corner, one small one off the left edge. Accent red, low alpha.
 */
fun Modifier.ambient(dark: Boolean): Modifier = this.drawBehind {
    val red = Color(0xFFD71921)
    val ga = if (dark) 0.40f else 0.13f   // light stays under 0.20 or it reads as a pink smear
    val gb = if (dark) 0.11f else 0.05f
    // Big glow, top-right, bleeding off the corner.
    val c1 = Offset(size.width - 40.dp.toPx(), 60.dp.toPx()); val r1 = 230.dp.toPx()
    drawCircle(
        Brush.radialGradient(
            0f to red.copy(alpha = ga), 0.44f to red.copy(alpha = ga * 0.30f), 0.70f to Color.Transparent,
            center = c1, radius = r1),
        radius = r1, center = c1)
    // Small glow, mid-left, bleeding off the edge.
    val c2 = Offset(-40.dp.toPx(), 380.dp.toPx()); val r2 = 175.dp.toPx()
    drawCircle(
        Brush.radialGradient(0f to red.copy(alpha = gb), 0.70f to Color.Transparent, center = c2, radius = r2),
        radius = r2, center = c2)
}

/**
 * Data glow — the driver page lights up in its own team_colour (design-system §2:
 * "on hero screens the glow comes from the data"). A single radial near the top,
 * centred behind the number and figure. col at ~40% alpha per f1-tab-complete.html.
 */
fun Modifier.glow(colour: Color): Modifier = this.drawBehind {
    val c = Offset(size.width / 2f, 130.dp.toPx()); val r = 205.dp.toPx()
    drawCircle(
        Brush.radialGradient(0f to colour.copy(alpha = 0.40f), 0.68f to Color.Transparent, center = c, radius = r),
        radius = r, center = c)
}

/**
 * Bleed the hero photo off the bottom (design-system §8). The source is cropped at
 * the chest; without a mask, bleeding it just shows the crop as a hard line. Fade
 * it: solid to 62%, then out to transparent by 100%. graphicsLayer forces an
 * offscreen layer so DstIn composites against the photo's own alpha, not the canvas.
 */
fun Modifier.fadeBottom(): Modifier = this
    // Offscreen compositing is REQUIRED: the alpha=0.99 trick doesn't reliably force
    // a layer in this Compose version, and without an isolated layer the DstIn rect
    // has nothing to blend against - the mask silently no-ops and the crop stays hard.
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                0.00f to Color.Black,
                0.62f to Color.Black,
                0.82f to Color.Black.copy(alpha = 0.72f),
                0.94f to Color.Black.copy(alpha = 0.18f),
                1.00f to Color.Transparent),
            blendMode = BlendMode.DstIn)
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

/**
 * The 3D circuit hero. FLAT extrusion straight down the Y axis - no isometric squash.
 * A stack of 22 offset copies is the side wall (fading with depth), the deck sits on
 * top, the racing surface is the bright hairline, Eau Rouge -> Raidillon is picked out
 * in accent red, and the start/finish line is a white dot. The outline is rotated by the
 * circuit's own `rotation` and normalised to the canvas here - room is reserved at the
 * bottom for the extrusion so it never clips.
 */
@Composable
fun Circuit3D(geo: CircuitGeometryEntity, modifier: Modifier = Modifier) {
    val xs = remember(geo) { geo.xs.split(",").map { it.toFloat() } }
    val ys = remember(geo) { geo.ys.split(",").map { it.toFloat() } }
    Canvas(modifier) {
        if (xs.size < 2) return@Canvas
        val th = Math.toRadians(geo.rotation.toDouble())
        val cos = kotlin.math.cos(th).toFloat(); val sin = kotlin.math.sin(th).toFloat()
        val rx = FloatArray(xs.size) { xs[it] * cos - ys[it] * sin }
        val ry = FloatArray(xs.size) { xs[it] * sin + ys[it] * cos }
        val mnx = rx.min(); val mny = ry.min()
        val w = (rx.max() - mnx).coerceAtLeast(1f); val h = (ry.max() - mny).coerceAtLeast(1f)
        val pad = 22.dp.toPx()
        // Thicker extrusion: 48 fine bands so it reads as a road with real depth, but
        // stays smooth enough that you can't count the steps. ~38dp of wall.
        val bands = 48
        val step = 0.8.dp.toPx()
        val depth = bands * step
        val sc = kotlin.math.min((size.width - 2 * pad) / w, (size.height - 2 * pad - depth) / h)
        val offX = pad + (size.width - 2 * pad - w * sc) / 2f
        val offY = pad + (size.height - 2 * pad - depth - h * sc) / 2f
        fun pt(i: Int) = Offset(offX + (rx[i] - mnx) * sc, offY + (ry[i] - mny) * sc)
        val path = Path().apply {
            moveTo(pt(0).x, pt(0).y)
            for (i in 1 until rx.size) { val p = pt(i); lineTo(p.x, p.y) }
            close()
        }
        val last = rx.lastIndex
        val a = geo.eauStart.coerceIn(0, last); val b = geo.eauEnd.coerceIn(a, last)
        val eau = Path().apply {   // Eau Rouge -> Raidillon: derived from corners 2..5
            val p0 = pt(a); moveTo(p0.x, p0.y)
            for (i in (a + 1)..b) { val p = pt(i); lineTo(p.x, p.y) }
        }
        // side wall - fine bands, fading with depth, so it reads as thickness not steps.
        for (i in bands downTo 1) {
            val t = i / bands.toFloat()
            translate(0f, i * step) {
                drawPath(path, Color(0xFF2A2A33).copy(alpha = 0.18f + 0.82f * (1 - t)),
                    style = Stroke(10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        drawPath(path, Color(0xFF1C1C20), style = Stroke(10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, Color(0xFFA7A7A7), style = Stroke(4.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(eau, Color(0xFFD71921), style = Stroke(4.8.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(Color.White, 3.2.dp.toPx(), pt(0))
    }
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
fun CarbonNumber(number: String, glowColour: Color, modifier: Modifier = Modifier, centerFraction: Float = 0.64f) {
    val p = LocalPalette.current
    Canvas(modifier) {
        // getPathForRange returns the SELECTION box, not glyph outlines - use platform Paint.
        // Tight tracking (-0.11em) so the two digits read as one unit ("12", "44"), not two.
        fun makePaint(sizePx: Float) = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sizePx
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            letterSpacing = -0.11f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        // Size to a TARGET WIDTH (~95% of the hero), not a fixed height: measure at a
        // reference size then scale. "1" and "44" land at the same visual weight; coerceIn
        // caps a lone "1" (which would otherwise be absurdly tall) and floors a wide number.
        val refWidth = makePaint(100.sp.toPx()).measureText(number).coerceAtLeast(1f)
        val finalSp = (100f * (size.width * 0.95f / refWidth)).coerceIn(160f, 480f)
        val glyph = android.graphics.Path()
        makePaint(finalSp.sp.toPx()).getTextPath(number, 0, number.length, 0f, 0f, glyph)
        val b = android.graphics.RectF()
        glyph.computeBounds(b, true)
        // Centre horizontally on the face; vertical centre low (~64%) so the tops bleed up
        // behind the head and the base drops into the name area (masked by the name-fade).
        glyph.offset(size.width / 2f - b.centerX(), size.height * centerFraction - b.centerY())
        val path = glyph.asComposePath()

        // Team glow behind: a blurred stroke of the glyph outline - wide colour, tight
        // neutral - so the halo wraps the digits' contour (drop-shadow on the alpha).
        drawIntoCanvas { canvas ->
            val gp = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 4.dp.toPx()
            }
            gp.color = glowColour.copy(alpha = 0.55f).toArgb()
            gp.maskFilter = BlurMaskFilter(46f, BlurMaskFilter.Blur.NORMAL)
            canvas.nativeCanvas.drawPath(glyph, gp)
            gp.color = Color(0xFFD2D2E6).copy(alpha = 0.14f).toArgb()
            gp.maskFilter = BlurMaskFilter(16f, BlurMaskFilter.Blur.NORMAL)
            canvas.nativeCanvas.drawPath(glyph, gp)
        }

        // Solid filled glyph, dark carbon - no hollow stroke, no twill. The team glow
        // behind is what reads; the fill is barely above canvas so it sits back.
        drawPath(path, color = if (p.isDark) Color(0xFF17171B) else Color(0xFFE6E6E2))
    }
}
