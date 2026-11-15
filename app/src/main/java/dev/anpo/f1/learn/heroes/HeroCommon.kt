package dev.anpo.f1.learn.heroes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anpo.f1.ui.F1Dim
import dev.anpo.f1.ui.F1Type
import dev.anpo.f1.ui.LocalPalette

/** The frame every hero sits in: the screen's side padding and a gap above. */
@Composable
internal fun HeroFrame(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = F1Dim.screenPad).padding(top = 8.dp, bottom = 6.dp)) { content() }
}

/** A small accent label in capitals, the same one the rest of the app uses for "DAYS" and "DID YOU KNOW". */
@Composable
internal fun HeroLabel(text: String) {
    Text(text.uppercase(), style = F1Type.label.copy(color = LocalPalette.current.accent, fontSize = 11.sp))
}

/** A text size that stays put when the user enlarges text. Used only for small labels drawn on a diagram, which also has a spoken description and the full text below it. */
@Composable
internal fun fixedSp(size: Dp): TextUnit = with(LocalDensity.current) { size.toSp() }
