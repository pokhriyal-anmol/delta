package io.github.pokhriyal_anmol.f1.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Placeholder until slice S5 lands the feed list. Says so plainly rather than faking content. */
@Composable
fun NewsScreen() {
    val p = LocalPalette.current
    Column(Modifier.fillMaxSize().background(p.canvas).ambient(p.isDark)
        .padding(F1Dim.screenPad).padding(top = 60.dp)) {
        Text("News", style = F1Type.name.copy(color = p.txt))
        Spacer(Modifier.height(10.dp))
        Text("Headlines from eight F1 sources arrive in the next update.",
            style = F1Type.body.copy(color = p.sec))
    }
}
