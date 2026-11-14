package dev.anpo.f1

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.anpo.f1.data.Reminders

/** Posts the reminder and nothing else. It reads no network and no database. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!Reminders.enabled(context) || !Reminders.canNotify(context)) return
        val race = intent.getStringExtra(Reminders.EXTRA_NAME).orEmpty().ifEmpty { "The race" }
        Reminders.ensureChannel(context)
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val note = Notification.Builder(context, Reminders.CHANNEL)
            .setSmallIcon(R.drawable.ic_f1).setContentTitle(race).setContentText("Lights out in 1 hour")
            .setContentIntent(open).setAutoCancel(true).build()
        context.getSystemService(NotificationManager::class.java).notify(1, note)
    }
}
