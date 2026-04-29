package com.project.habithearth.notifications

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.project.habithearth.R
import com.project.habithearth.data.UserProgressRepository
import com.project.habithearth.data.datastore.userProgressProtoDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val (settings, pendingTasks) = runBlocking {
            val repo = UserProgressRepository(context)
            val account = repo.accountSettings.first()
            val pending = context.userProgressProtoDataStore.data.first().tasks.count { !it.isCompleted }
            account to pending
        }

        if (settings.pushNotifications) {
            // Re-arm next day's exact alarm each time this receiver runs.
            TaskReminderScheduler.scheduleDaily(
                context = context,
                hour = settings.notificationHour,
                minute = settings.notificationMinute,
            )
        }

        if (!canPostNotifications(context)) return

        val contentText = "You have $pendingTasks tasks that you need to do"
        val notification = NotificationCompat.Builder(context, TaskReminderScheduler.channelId())
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Habit Hearth")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(2001, notification)
    }

    private fun canPostNotifications(context: Context): Boolean {
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!enabled) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
