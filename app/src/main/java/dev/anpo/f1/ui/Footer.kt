package dev.anpo.f1.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The end of Home: the flavor's support banner (empty in the Play build), then who this is not. */
@Composable
fun AppFooter() {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        SupportBanner()
        Text("Unofficial fan app. Not affiliated with Formula 1 or any team. Results from Jolpica, tracks from MultiViewer.",
            style = F1Type.label.copy(color = p.sec, fontSize = 10.sp), textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp).padding(top = 22.dp, bottom = 8.dp))
    }
}
