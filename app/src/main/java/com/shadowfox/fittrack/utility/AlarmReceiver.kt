package com.shadowfox.fittrack.utility

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.shadowfox.fittrack.R

class AlarmReceiver : BroadcastReceiver() {
    private val CHANNEL_ID = "WaterReminderChannel"
    private val NOTIFICATION_ID = 200

    override fun onReceive(context: Context, intent: Intent) {
        // Use the public constant from NotificationScheduler
        if (intent.action == NotificationScheduler.ACTION_REMIND) {
            showNotification(context)
        }
    }
    private fun showNotification(context: Context) {
        // Create the notification channel (must be done on API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Water Reminders"
            val descriptionText = "Reminders to drink water"
            val importance = NotificationManagerCompat.IMPORTANCE_DEFAULT
            val channel = android.app.NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Build the notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Hydration Reminder 💧")
            .setContentText("Time to drink some water to hit your daily goal!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        // Show the notification
        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }
}