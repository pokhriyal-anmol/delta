package dev.anpo.f1.ui

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anpo.f1.BuildConfig
import dev.anpo.f1.data.Net
import dev.anpo.f1.data.RELEASES_FEED
import dev.anpo.f1.data.RELEASE_PAGE
import dev.anpo.f1.data.isNewer
import dev.anpo.f1.data.latestTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The newest release tag if it is ahead of this build. Asks GitHub's releases feed at most once a day, only while the app is open, and records a failed try so it is not repeated on every screen. */
private suspend fun newerTag(ctx: Context): String? {
    val prefs = ctx.getSharedPreferences("update", Context.MODE_PRIVATE)
    val now = System.currentTimeMillis()
    var latest = prefs.getString("latest", null)
    if (now - prefs.getLong("checkedAt", 0L) > 86_400_000L) {
        val fresh = runCatching { latestTag(Net.feed(RELEASES_FEED)) }.getOrNull()
        prefs.edit().putLong("checkedAt", now).apply { if (fresh != null) putString("latest", fresh) }.apply()
        if (fresh != null) latest = fresh
    }
    return latest?.takeIf { isNewer(it, BuildConfig.VERSION_NAME) }
}

/** One quiet line under the Home title when a newer release exists. It opens that release in the browser. The Play build has no such code, because Play updates itself. */
@Composable
fun UpdateNote() {
    val ctx = LocalContext.current
    val p = LocalPalette.current
    var tag by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { tag = withContext(Dispatchers.IO) { newerTag(ctx) } }
    tag?.let { t ->
        Text("Update available: $t ›", style = F1Type.body.copy(color = p.accent, fontSize = 12.sp),
            modifier = Modifier.padding(horizontal = F1Dim.screenPad).padding(top = 10.dp)
                .pressable(role = Role.Button, onClickLabel = "Open the new release") { open(ctx, RELEASE_PAGE + t) })
    }
}
