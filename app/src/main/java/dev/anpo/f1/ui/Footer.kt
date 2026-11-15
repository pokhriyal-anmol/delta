package dev.anpo.f1.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anpo.f1.data.diagnostics

private const val NOTICE_URL = "https://github.com/pokhriyal-anmol/delta/blob/master/NOTICE"

/** The end of Home: the flavor's support banner (empty in the Play build), who this is not, the data credit the licence asks for, and a copy-diagnostics link for bug reports. */
@Composable
fun AppFooter() {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        SupportBanner()
        Text("Unofficial fan app. Not affiliated with Formula 1 or any team. Results from Jolpica-F1 (CC BY-NC-SA 4.0), tracks from MultiViewer.",
            style = F1Type.label.copy(color = p.sec, fontSize = 10.sp), textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp).padding(top = 22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            FooterLink("Data and licences") { open(ctx, NOTICE_URL) }
            FooterLink("Copy diagnostics") {
                ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Delta diagnostics", diagnostics(ctx)))
                if (Build.VERSION.SDK_INT < 33) Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
private fun FooterLink(text: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    Text(text, style = F1Type.label.copy(color = p.accent, fontSize = 11.sp),
        modifier = Modifier.heightIn(min = 48.dp).pressable(role = Role.Button, onClickLabel = text, onClick = onClick)
            .padding(top = 14.dp))
}
