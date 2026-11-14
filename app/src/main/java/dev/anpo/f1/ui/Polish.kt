package dev.anpo.f1.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.request.ImageRequest

/**
 * One motion language for the app. Everything uses a strong ease-out and stays under 300 ms, except sheets.
 * It all switches off when the phone's "remove animations" setting is on.
 */
val EaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
private const val PRESS_SCALE = 0.97f
private const val ARRIVE_ROWS = 6
private const val ARRIVE_STAGGER_MS = 35

@Composable
fun rememberMotionEnabled(): Boolean {
    val ctx = LocalContext.current
    return remember { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f }
}

/** A light haptic tick. */
@Composable
fun rememberTick(): () -> Unit {
    val haptics = LocalHapticFeedback.current
    return remember(haptics) { { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) } }
}

/** Dips to 97% while pressed, so a control feels like it heard the touch. No ripple: depth comes from light. */
fun Modifier.pressable(enabled: Boolean = true, role: Role? = null, onClickLabel: String? = null, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val motion = rememberMotionEnabled()
    val scale by animateFloatAsState(if (pressed && motion) PRESS_SCALE else 1f, tween(160, easing = EaseOut), label = "press")
    graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = source, indication = null, enabled = enabled, role = role, onClickLabel = onClickLabel, onClick = onClick)
}

/** The first rows of a list fade and rise in 35 ms apart. Later rows appear at once. */
fun Modifier.arrive(index: Int): Modifier = composed {
    val motion = rememberMotionEnabled()
    if (!motion || index >= ARRIVE_ROWS) return@composed this
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { a.animateTo(1f, tween(350, delayMillis = index * ARRIVE_STAGGER_MS, easing = EaseOut)) }
    graphicsLayer { alpha = a.value; translationY = (1f - a.value) * 8.dp.toPx() }
}

/** Quiet placeholder rows with a slow shimmer, the same height as the rows they stand in for. */
@Composable
fun SkeletonRows(count: Int = 6, rowHeight: androidx.compose.ui.unit.Dp = 64.dp, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val motion = rememberMotionEnabled()
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "x")
    Column(modifier) {
        repeat(count) { i ->
            val at = if (motion) x * 1200f - 200f else 400f
            val brush = Brush.linearGradient(listOf(p.chip, p.chip.copy(alpha = 0.55f), p.chip), start = androidx.compose.ui.geometry.Offset(at, 0f), end = androidx.compose.ui.geometry.Offset(at + 400f, 120f))
            Row(Modifier.fillMaxWidth().heightIn(min = rowHeight).padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 4.dp, height = 24.dp).clip(RoundedCornerShape(2.dp)).background(brush))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth(0.55f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(brush))
                    Spacer(Modifier.height(7.dp))
                    Box(Modifier.fillMaxWidth(0.3f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(brush))
                }
                Box(Modifier.size(width = 34.dp, height = 16.dp).clip(RoundedCornerShape(8.dp)).background(brush))
            }
            if (i < count - 1) Box(Modifier.fillMaxWidth().height(0.5.dp).background(p.divider))
        }
    }
}

/** The one main action on a page: a red pill with an arrow. The 48 dp tap area is bigger than the pill. */
@Composable
fun PillButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(modifier.heightIn(min = 48.dp).pressable(enabled = enabled, role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Row(Modifier.clip(CircleShape).background(if (enabled) p.accent else p.chip).padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text, style = F1Type.body.copy(color = if (enabled) Color.White else p.sec, fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
            Text("→", style = F1Type.body.copy(color = if (enabled) Color.White else p.sec, fontSize = 13.sp))
        }
    }
}

/** One line of what went wrong or is missing, and one action when there is one. */
@Composable
fun StateMessage(title: String, detail: String? = null, action: String? = null, modifier: Modifier = Modifier, onAction: () -> Unit = {}) {
    val p = LocalPalette.current
    Column(modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(p.chip), contentAlignment = Alignment.Center) {
            Text("!", style = F1Type.body.copy(color = p.sec, fontSize = 18.sp, fontWeight = FontWeight.Bold))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = F1Type.body.copy(color = p.txt, fontSize = 15.sp, fontWeight = FontWeight.SemiBold), textAlign = TextAlign.Center)
        if (detail != null) { Spacer(Modifier.height(4.dp)); Text(detail, style = F1Type.meta.copy(color = p.sec), textAlign = TextAlign.Center) }
        if (action != null) { Spacer(Modifier.height(10.dp)); PillButton(action, onClick = onAction) }
    }
}

/** A compact title that fades in over the list once the big title has scrolled away. */
@Composable
fun CompactTitleBar(title: String, visible: Boolean, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val motion = rememberMotionEnabled()
    val a by animateFloatAsState(if (visible) 1f else 0f, if (motion) tween(200, easing = EaseOut) else snap(), label = "bar")
    if (a == 0f) return
    Column(modifier.fillMaxWidth().graphicsLayer { alpha = a; translationY = (1f - a) * -8.dp.toPx() }.background(p.canvas.copy(alpha = 0.96f))) {
        Box(Modifier.fillMaxWidth().statusBarsPadding().height(52.dp).padding(horizontal = F1Dim.screenPad), contentAlignment = Alignment.CenterStart) {
            Text(title, style = F1Type.section.copy(color = p.txt, fontSize = 20.sp, letterSpacing = (-0.4).sp), maxLines = 1)
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(p.divider))
    }
}

/** Fades pictures in over the tinted placeholder instead of popping. */
fun fadeModel(ctx: Context, url: String): ImageRequest = ImageRequest.Builder(ctx).data(url).crossfade(250).build()

/** The thin lit edge along the top of a card: light, not shadow, on near-black. */
fun litEdge(dark: Boolean): Brush =
    Brush.verticalGradient(listOf(Color.White.copy(alpha = if (dark) 0.10f else 0f), Color.White.copy(alpha = if (dark) 0.02f else 0f)))

val PillShape = RoundedCornerShape(50)

/** The race reminder switch: outline when off, filled red when on, and it rings when switched on. */
@Composable
fun ReminderBell(on: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val motion = rememberMotionEnabled()
    val rot = remember { Animatable(0f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(on) {
        if (first) first = false
        else if (on && motion) for (a in listOf(18f, -14f, 9f, -5f, 0f)) rot.animateTo(a, tween(90, easing = EaseOut))
    }
    val bg by animateColorAsState(if (on) p.accent else p.nav, tween(200, easing = EaseOut), label = "bell-bg")
    val fg by animateColorAsState(if (on) Color.White else p.sec, tween(200, easing = EaseOut), label = "bell-fg")
    Box(Modifier.size(48.dp).pressable(role = Role.Switch, onClickLabel = if (on) "Turn the race reminder off" else "Turn the race reminder on", onClick = onClick),
        contentAlignment = Alignment.Center) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(dev.anpo.f1.R.drawable.ic_bell), contentDescription = null,
                tint = fg, modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = rot.value })
        }
    }
}
