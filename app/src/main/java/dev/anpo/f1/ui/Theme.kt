package dev.anpo.f1.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anpo.f1.R

/**
 * Two Inter families split by optical size, same idea as SF Text vs SF Display: the
 * Display cut is drawn tighter and is used at 23sp and up; the Text cut keeps digits
 * and small copy legible below that.
 */
val InterText = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_bold, FontWeight.Bold),
)
val InterDisplay = FontFamily(
    Font(R.font.inter_display_bold, FontWeight.Bold),
    Font(R.font.inter_display_extrabold, FontWeight.ExtraBold),
)

/** Tabular figures - every number in the app uses these so columns don't jitter. */
private const val TNUM = "tnum"

/**
 * The design system, as agreed. Every value here was a decision:
 *
 *  - Canvas is #050506, NOT pure black. Shadows need somewhere to land.
 *  - Four greys from Nothing OS. Two of them (#848484, #A7A7A7) are
 *    mode-INVARIANT - mid-greys read against both ends, so they never flip.
 *  - Accent is Nothing Red. It is the only accent. Team colour is DATA,
 *    it never appears in chrome. (Ferrari #ED1131 and Audi #F50537 are both
 *    reds and both live in lists - see accentFor().)
 *  - Type ladder: 48 / 40 / 38 / 23 / 19 / 10. Nothing lives in 20-35.
 *    The gap IS the design; don't fill it.
 */

@Immutable
data class F1Palette(
    val canvas: Color, val card: Color, val chip: Color, val nav: Color,
    val txt: Color, val sec: Color, val ter: Color, val qua: Color, val divider: Color,
    val accent: Color, val barBg: Color,
    val trackBase: Color, val trackMain: Color, val track3d: Color,
    val numStroke: Color, val engrave: Color,
    val isDark: Boolean,
)

private val Dark = F1Palette(
    canvas = Color(0xFF050506), card = Color(0xFF131316), chip = Color(0xFF1E1E23),
    nav = Color(0xFF16161A), txt = Color(0xFFFFFFFF), sec = Color(0xFF848484),
    ter = Color(0xFFA7A7A7), qua = Color(0xFFC5C5C5), divider = Color(0xFF1E1E23),
    accent = Color(0xFFD71921), barBg = Color(0xFF18181D),
    trackBase = Color(0xFF1C1C20), trackMain = Color(0xFFA7A7A7), track3d = Color(0xFF2A2A33),
    numStroke = Color(0xFF43434F), engrave = Color(0x0FFFFFFF), isDark = true,
)

private val Light = F1Palette(
    canvas = Color(0xFFF5F5F3), card = Color(0xFFFFFFFF), chip = Color(0xFFF0F0EE),
    nav = Color(0xFFFFFFFF), txt = Color(0xFF0A0A0A), sec = Color(0xFF848484),
    ter = Color(0xFFA7A7A7), qua = Color(0xFF9A9A94), divider = Color(0xFFEDEDEA),
    accent = Color(0xFFD71921), barBg = Color(0xFFFFFFFF),   // opaque: 74% white vanished on white cards
    trackBase = Color(0xFFE6E6E3), trackMain = Color(0xFF3A3A3A), track3d = Color(0xFFBEBEB6),
    numStroke = Color(0xFFD2D2CB), engrave = Color(0x12000000), isDark = false,
)

val LocalPalette = staticCompositionLocalOf { Dark }

@Composable
fun F1Theme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPalette provides if (dark) Dark else Light, content = content)
}

/**
 * Type ladder. Weight 800 / 700 / 500 / 400 - nothing between 23 and 38sp.
 * Display cut (InterDisplay) at 23sp and up; Text cut (InterText) below. Tracking was
 * retuned for Inter Display, which sets tighter than Roboto (hero/display loosened).
 */
object F1Type {
    val hero    = TextStyle(fontFamily = InterDisplay, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.4).sp, fontFeatureSettings = TNUM)
    val display = TextStyle(fontFamily = InterDisplay, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.2).sp, fontFeatureSettings = TNUM)
    val name    = TextStyle(fontFamily = InterDisplay, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.2).sp, fontFeatureSettings = TNUM)
    val section = TextStyle(fontFamily = InterDisplay, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp, fontFeatureSettings = TNUM)
    val stat    = TextStyle(fontFamily = InterText, fontSize = 19.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = TNUM)
    val body    = TextStyle(fontFamily = InterText, fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TNUM)
    val meta    = TextStyle(fontFamily = InterText, fontSize = 12.sp, fontWeight = FontWeight.Normal, fontFeatureSettings = TNUM)
    val label   = TextStyle(fontFamily = InterText, fontSize = 10.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.5.sp, fontFeatureSettings = TNUM)
}

object F1Dim {
    val cardRadius = 28.dp
    val tileRadius = 22.dp
    val pillRadius = 999.dp
    val screenPad  = 22.dp
    val cardPad    = 20.dp
}

/**
 * The accent-yields rule. Ferrari (#ED1131) and Audi (#F50537) sit within a few
 * degrees of Nothing Red (#D71921); putting them adjacent reads as a rendering
 * bug rather than a distinction. On a page themed to a near-red team, chrome
 * falls back to monochrome and lets the team own the colour.
 */
fun accentFor(teamColour: Color, p: F1Palette): Color {
    val h = hueOf(teamColour); val a = hueOf(p.accent)
    val d = kotlin.math.abs(h - a).let { if (it > 180f) 360f - it else it }
    return if (d < 30f) p.txt else p.accent
}

private fun hueOf(c: Color): Float {
    val r = c.red; val g = c.green; val b = c.blue
    val max = maxOf(r, g, b); val min = minOf(r, g, b); val d = max - min
    if (d == 0f) return 0f
    val h = when (max) {
        r -> 60f * (((g - b) / d) % 6f)
        g -> 60f * (((b - r) / d) + 2f)
        else -> 60f * (((r - g) / d) + 4f)
    }
    return (h + 360f) % 360f
}

/** team_colour arrives from the API as a bare hex string, no leading #. */
fun teamColour(hex: String?): Color =
    runCatching { Color(("FF" + (hex ?: "888888")).toLong(16)) }.getOrDefault(Color(0xFF888888))
