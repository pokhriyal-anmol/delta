package io.github.pokhriyal_anmol.f1.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import io.github.pokhriyal_anmol.f1.ReminderReceiver

const val REMINDER_LEAD_MS = 60 * 60 * 1000L

/** One hour before lights out, or null when that moment has already passed. */
fun reminderTime(raceStartMs: Long?, nowMs: Long): Long? =
    raceStartMs?.minus(REMINDER_LEAD_MS)?.takeIf { it > nowMs }

/**
 * The race reminder. The app hands the OS one timestamp it already knows and exits, so it does no work
 * in the background (the project's rule). The alarm is exact where Android allows it, because an inexact
 * alarm set a day ahead is given a window of up to an hour, which would land the reminder at lights out
 * (measured on the emulator: window=+1h0m0s0ms). It is set each time Home opens, because a reboot clears
 * it and the app may not run in between.
 */
object Reminders {
    const val CHANNEL = "race_reminder"
    const val EXTRA_NAME = "race"
    private const val REQUEST = 1
    private const val PREFS = "reminder"
    private const val KEY_ON = "on"
    private const val KEY_ASKED = "asked"

    fun enabled(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ON, true)
    fun setEnabled(ctx: Context, on: Boolean) = prefs(ctx).edit().putBoolean(KEY_ON, on).apply()
    fun asked(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ASKED, false)
    fun markAsked(ctx: Context) = prefs(ctx).edit().putBoolean(KEY_ASKED, true).apply()

    /** Android 13 and later ask before any notification. Earlier versions need nothing. */
    fun canNotify(ctx: Context): Boolean = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun schedule(ctx: Context, raceName: String, raceStartMs: Long?) {
        val at = reminderTime(raceStartMs, System.currentTimeMillis())
        if (at == null) { cancel(ctx); return }
        ensureChannel(ctx)
        val alarms = ctx.getSystemService(AlarmManager::class.java)
        // shortcut: below Android 12 or without the exact-alarm grant this falls back to an inexact alarm, which can be late.
        if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms())
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(ctx, raceName))
        else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(ctx, raceName))
    }

    fun cancel(ctx: Context) {
        ctx.getSystemService(AlarmManager::class.java).cancel(pending(ctx, ""))
    }

    fun ensureChannel(ctx: Context) {
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Race reminders", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "One reminder an hour before lights out" })
    }

    private fun pending(ctx: Context, raceName: String): PendingIntent = PendingIntent.getBroadcast(
        ctx, REQUEST, Intent(ctx, ReminderReceiver::class.java).putExtra(EXTRA_NAME, raceName),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
