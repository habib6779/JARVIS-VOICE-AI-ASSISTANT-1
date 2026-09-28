package com.jarvis.assistant.tools

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ReminderItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val triggerTimeMs: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val isDelivered: Boolean = false
) {
    val formattedTriggerTime: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(triggerTimeMs))
        }
}
