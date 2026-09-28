package com.jarvis.assistant.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.jarvis.assistant.data.preferences.AppPreferences
import com.jarvis.assistant.service.ReminderReceiver
import com.jarvis.assistant.tools.ReminderItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReminderRepository(
    private val context: Context,
    private val preferences: AppPreferences
) {
    private val _reminders = MutableStateFlow<List<ReminderItem>>(emptyList())
    val reminders: StateFlow<List<ReminderItem>> = _reminders.asStateFlow()

    init {
        _reminders.value = preferences.loadReminders()
    }

    @Synchronized
    fun addReminder(title: String, triggerTimeMs: Long): ReminderItem {
        val item = ReminderItem(title = title, triggerTimeMs = triggerTimeMs)
        val current = _reminders.value.toMutableList()
        current.add(item)
        _reminders.value = current
        preferences.saveReminders(current)

        scheduleSystemAlarm(item)
        return item
    }

    @Synchronized
    fun removeReminder(id: String) {
        val current = _reminders.value.toMutableList()
        val item = current.firstOrNull { it.id == id }
        if (item != null) {
            cancelSystemAlarm(item)
            current.remove(item)
            _reminders.value = current
            preferences.saveReminders(current)
        }
    }

    private fun scheduleSystemAlarm(item: ReminderItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TITLE, item.title)
            putExtra(ReminderReceiver.EXTRA_ID, item.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    item.triggerTimeMs,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    item.triggerTimeMs,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, item.triggerTimeMs, pendingIntent)
        }
    }

    private fun cancelSystemAlarm(item: ReminderItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun rescheduleAll() {
        val now = System.currentTimeMillis()
        for (item in _reminders.value) {
            if (item.triggerTimeMs > now) {
                scheduleSystemAlarm(item)
            }
        }
    }
}
