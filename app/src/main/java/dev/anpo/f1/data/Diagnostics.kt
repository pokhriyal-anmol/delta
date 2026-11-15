package dev.anpo.f1.data

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import dev.anpo.f1.BuildConfig
import java.time.Instant

private fun exitName(reason: Int) = when (reason) {
    ApplicationExitInfo.REASON_ANR -> "not responding"
    ApplicationExitInfo.REASON_CRASH -> "crash"
    ApplicationExitInfo.REASON_CRASH_NATIVE -> "native crash"
    ApplicationExitInfo.REASON_LOW_MEMORY -> "low memory"
    ApplicationExitInfo.REASON_EXIT_SELF -> "exited itself"
    ApplicationExitInfo.REASON_USER_REQUESTED -> "closed by the user"
    ApplicationExitInfo.REASON_SIGNALED -> "killed by a signal"
    else -> "reason $reason"
}

/** Text a user can paste into an issue. Built on the phone from what Android keeps about why the last runs ended; nothing is sent anywhere. */
fun diagnostics(ctx: Context): String = buildString {
    appendLine("Delta ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}, ${BuildConfig.FLAVOR})")
    appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), ${Build.MANUFACTURER} ${Build.MODEL}")
    if (Build.VERSION.SDK_INT >= 30) runCatching {
        ctx.getSystemService(ActivityManager::class.java).getHistoricalProcessExitReasons(ctx.packageName, 0, 5)
            .forEach { appendLine("Last exit: ${exitName(it.reason)} at ${Instant.ofEpochMilli(it.timestamp)} ${it.description.orEmpty()}".trim()) }
    }
}
