package com.landlordspaces.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Background check (about every 15 minutes): tells the member about new messages and alerts without needing Google push. */
class AlertWorker(private val ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val session = Session(ctx)
        if (!session.signedIn()) return Result.success()
        return try {
            val h = session.call("GET", "/home")
            val k = h.optJSONObject("kpi")
            val msgs = k?.optInt("unread_messages") ?: 0
            val notes = k?.optInt("unread_notifications") ?: 0
            val prefs = ctx.getSharedPreferences("ls_alerts", Context.MODE_PRIVATE)
            val seenMsgs = prefs.getInt("msgs", 0)
            val seenNotes = prefs.getInt("notes", 0)
            if (msgs > seenMsgs) show(1, "New message", if (msgs == 1) "You have 1 unread message." else "You have $msgs unread messages.")
            else if (notes > seenNotes) show(2, "LandlordSpaces", if (notes == 1) "You have 1 new alert." else "You have $notes new alerts.")
            prefs.edit().putInt("msgs", msgs).putInt("notes", notes).apply()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun show(id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(ctx, Alerts.CHANNEL)
            .setSmallIcon(R.drawable.ic_stat).setContentTitle(title).setContentText(text).setAutoCancel(true).setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT).setColor(0xFF0E7C56.toInt()).build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }
}

object Alerts {
    const val CHANNEL = "ls_alerts"

    fun setup(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Messages and alerts", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val req = PeriodicWorkRequest.Builder(AlertWorker::class.java, 15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("ls_alerts", ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun stop(ctx: Context) {
        WorkManager.getInstance(ctx).cancelUniqueWork("ls_alerts")
        ctx.getSharedPreferences("ls_alerts", Context.MODE_PRIVATE).edit().clear().apply()
    }
}
