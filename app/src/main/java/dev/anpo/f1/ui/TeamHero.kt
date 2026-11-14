package dev.anpo.f1.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Outline
import coil.compose.AsyncImage
import dev.anpo.f1.data.Net

/** What the hero needs about one driver: enough to show a chip and open the page. */
data class HeroDriver(val driverId: String, val family: String, val headshot: String?)

/** What the hero shows about the team. Plain values so this file knows nothing of the database. */
data class HeroTeam(
    val name: String, val season: String, val position: Int,
    val points: String, val titles: String, val carFolder: String?,
)

/**
 * The team page hero (design-system section 11): a glow in the team colour, the real 2026 car
 * crossing the page, the name inside a stripe band, both drivers as chips, then points and
 * titles. The car image is F1's own, loaded from their CDN the way the headshots are, and
 * never stored. Offline it is simply absent and the hero keeps its height.
 */
@Composable
fun TeamHero(team: HeroTeam, drivers: List<HeroDriver>, col: Color, onDriver: (String) -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    Column(Modifier.fillMaxWidth().height(424.dp).glow(col).halftone(col), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(64.dp))
        Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
            if (team.carFolder != null) {
                AsyncImage(
                    model = fadeModel(ctx, Net.car(team.carFolder)), contentDescription = "${team.name} 2026 car",
                    contentScale = ContentScale.Fit, modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(96.dp),
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        StripeBand(team.name.removeSuffix(" F1 Team"), col)
        Spacer(Modifier.height(14.dp))
        Text("CONSTRUCTOR · ${team.season} · P${team.position}", style = F1Type.label.copy(color = p.sec, fontSize = 11.sp))
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { drivers.forEach { DriverChip(it, onDriver) } }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 40.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            HeroFigure(team.points, "PTS")
            HeroFigure(team.titles, "TITLES")
        }
    }
}

@Composable
private fun HeroFigure(value: String, label: String) {
    val p = LocalPalette.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = F1Type.display.copy(color = p.txt, fontSize = 38.sp), maxLines = 1, softWrap = false)
        Text(label, style = F1Type.label.copy(color = p.sec))
    }
}

@Composable
private fun DriverChip(d: HeroDriver, onDriver: (String) -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    Row(
        Modifier.clip(CircleShape).background(p.card).pressable(role = androidx.compose.ui.semantics.Role.Button, onClickLabel = d.family) { onDriver(d.driverId) }
            .padding(start = 5.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(p.chip)) {
            if (d.headshot != null) {
                AsyncImage(model = fadeModel(ctx, Net.headshot(d.headshot)), contentDescription = null,
                    contentScale = ContentScale.Crop, alignment = Alignment.TopCenter, modifier = Modifier.size(34.dp))
            }
        }
        Text(d.family, style = F1Type.body.copy(color = p.txt, fontSize = 13.sp), maxLines = 1)
    }
}

/** The name between two stripes. A long name drops to 38 sp so the stripes never vanish. */
@Composable
private fun StripeBand(name: String, col: Color) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().height(58.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Stripes(col, mirrored = false, modifier = Modifier.weight(1f))
        Text(name, style = F1Type.name.copy(color = p.txt, fontSize = if (name.length > 8) 38.sp else 48.sp, letterSpacing = (-2).sp),
            maxLines = 1, softWrap = false, textAlign = TextAlign.Center)
        Stripes(col, mirrored = true, modifier = Modifier.weight(1f))
    }
}

/** A thick bar over a thin team-colour line, the outer end slanted. */
@Composable
private fun Stripes(col: Color, mirrored: Boolean, modifier: Modifier) {
    val p = LocalPalette.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.fillMaxWidth().height(11.dp).clip(SlantShape(mirrored)).background(p.txt.copy(alpha = 0.92f)))
        Box(Modifier.fillMaxWidth().height(3.dp).clip(SlantShape(mirrored)).background(col))
    }
}

/** A bar whose inner end is cut at an angle, like the banner it comes from. */
private class SlantShape(private val mirrored: Boolean) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cut = with(density) { 11.dp.toPx() }.coerceAtMost(size.width)
        val path = Path().apply {
            if (!mirrored) { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width - cut, size.height); lineTo(0f, size.height) }
            else { moveTo(cut, 0f); lineTo(size.width, 0f); lineTo(size.width, size.height); lineTo(0f, size.height) }
            close()
        }
        return Outline.Generic(path)
    }
}

/** A band of fading dots behind the car, in the team colour. Light, not shadow. */
private fun Modifier.halftone(col: Color): Modifier = drawBehind {
    val step = 6.dp.toPx(); val top = 56.dp.toPx(); val rows = 18
    for (r in 0 until rows) {
        val y = top + r * step
        val v = 1f - kotlin.math.abs(r - rows / 2f) / (rows / 2f)
        dotRow(y, step, col.copy(alpha = 0.45f * v))
    }
}

private fun DrawScope.dotRow(y: Float, step: Float, colour: Color) {
    var x = step / 2
    while (x < size.width) {
        val edge = minOf(x, size.width - x) / (size.width * 0.28f)
        val a = edge.coerceIn(0f, 1f)
        drawCircle(colour.copy(alpha = colour.alpha * a), radius = 1.1.dp.toPx(), center = Offset(x, y))
        x += step
    }
}
