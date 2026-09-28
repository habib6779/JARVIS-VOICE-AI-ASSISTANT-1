package com.jarvis.assistant.tools

import com.jarvis.assistant.data.model.ToolExecutionResult
import com.jarvis.assistant.data.repository.ReminderRepository
import java.util.regex.Pattern

class ReminderTool(private val reminderRepository: ReminderRepository) {

    fun createReminder(query: String): ToolExecutionResult {
        var durationMinutes = 10L // default
        var reminderTitle = "Reminder from JARVIS"

        // Check for minutes
        val minutePattern = Pattern.compile("(\\d+)\\s*(?:min|minute|মিনিট)", Pattern.CASE_INSENSITIVE)
        val minuteMatcher = minutePattern.matcher(query)
        if (minuteMatcher.find()) {
            minuteMatcher.group(1)?.toLongOrNull()?.let { durationMinutes = it }
        } else {
            // Check for hours
            val hourPattern = Pattern.compile("(\\d+)\\s*(?:hour|hr|ঘণ্টা)", Pattern.CASE_INSENSITIVE)
            val hourMatcher = hourPattern.matcher(query)
            if (hourMatcher.find()) {
                hourMatcher.group(1)?.toLongOrNull()?.let { durationMinutes = it * 60 }
            }
        }

        // Clean title
        val cleaned = query
            .replace(Regex("remind me (to|about)?", RegexOption.IGNORE_CASE), "")
            .replace(Regex("in \\d+\\s*(?:min|minute|hour|hr|মিনিট|ঘণ্টা)?", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\d+\\s*(?:min|minute|hour|hr|মিনিট|ঘণ্টা) পরে?", RegexOption.IGNORE_CASE), "")
            .replace(Regex("আমাকে মনে করিয়ে (দাও|দিও)?", RegexOption.IGNORE_CASE), "")
            .trim()

        if (cleaned.isNotBlank()) {
            reminderTitle = cleaned.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
        }

        val triggerTimeMs = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        val item = reminderRepository.addReminder(reminderTitle, triggerTimeMs)

        return ToolExecutionResult(
            toolName = "Reminder",
            success = true,
            message = "Reminder set for '$reminderTitle' at ${item.formattedTriggerTime} (in $durationMinutes minutes).",
            data = item
        )
    }
}
