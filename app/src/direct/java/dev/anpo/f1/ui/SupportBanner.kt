package dev.anpo.f1.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anpo.f1.BuildConfig
import dev.anpo.f1.data.httpsUrl

/**
 * A quiet note at the very end of Home, seen only by scrolling to the bottom. One sentence and one
 * text link that opens the support page in the browser. The app makes no request of its own, and
 * with no `supportUrl` set (or one that is not https) nothing is drawn at all.
 */
@Composable
fun SupportBanner() {
    val url = httpsUrl(BuildConfig.SUPPORT_URL) ?: return
    val p = LocalPalette.current
    val ctx = LocalContext.current
    F1Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 28.dp), radius = 22) {
        Column(Modifier.padding(horizontal = 18.dp).padding(top = 16.dp)) {
            Text("Delta has no ads, no account and no server. If it earned a place on your phone, a coffee helps keep it going.",
                style = F1Type.meta.copy(color = p.sec, fontSize = 13.sp))
            Text("Buy me a coffee ›", style = F1Type.body.copy(color = p.accent, fontSize = 13.sp),
                modifier = Modifier.pressable(role = Role.Button, onClickLabel = "Open the support page") { open(ctx, url) }
                    .padding(top = 12.dp, bottom = 16.dp))
        }
    }
}
