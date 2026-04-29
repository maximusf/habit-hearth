package com.project.habithearth.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

private const val ChannelId = "habit_reminders"
private const val ChannelName = "Habit reminders"
private const val ReminderRequestCode = 1441
private const val Tag = "TaskReminderScheduler"

object TaskReminderScheduler {
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            ChannelId,
            ChannelName,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Daily reminders for pending tasks"
        }
        manager.createNotificationChannel(channel)
    }

    fun scheduleDaily(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = reminderPendingIntent(context)
        alarmManager.cancel(pendingIntent)
        val triggerAtMillis = nextTriggerMillis(hour = hour, minute = minute)
        runCatching {
            // Android 12+ may block exact alarms unless the app is exempt or has
            // user-granted exact-alarm access. Fall back instead of crashing.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            }
        }.onFailure { error ->
            Log.e(Tag, "Failed to schedule reminder alarm", error)
            // Last-resort fallback for OEM/permission edge cases.
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = reminderPendingIntent(context)
        alarmManager.cancel(pendingIntent)
    }

    fun channelId(): String = ChannelId

    private fun reminderPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, TaskReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            ReminderRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (trigger.timeInMillis <= now.timeInMillis) {
            trigger.add(Calendar.DAY_OF_YEAR, 1)
        }
        return trigger.timeInMillis
    }
}
