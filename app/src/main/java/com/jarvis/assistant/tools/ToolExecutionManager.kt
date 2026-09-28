package com.jarvis.assistant.tools

import android.content.Context
import com.jarvis.assistant.JarvisApp
import com.jarvis.assistant.data.model.ToolDefinition
import com.jarvis.assistant.data.model.ToolExecutionResult
import java.util.regex.Pattern

class ToolExecutionManager(
    private val context: Context,
    private val appLauncher: AppLauncherTool = AppLauncherTool(context),
    private val contactManager: ContactManagerTool = ContactManagerTool(context),
    private val callManager: CallManagerTool = CallManagerTool(context),
    private val reminderTool: ReminderTool = ReminderTool(JarvisApp.instance.reminderRepository),
    private val webSearchTool: WebSearchTool = WebSearchTool(context),
    private val documentReaderTool: DocumentReaderTool = DocumentReaderTool(context),
    val screenVisionTool: ScreenVisionTool = ScreenVisionTool(),
    val cameraVisionTool: CameraVisionTool = CameraVisionTool()
) {
    val registeredTools: List<ToolDefinition> = listOf(
        ToolDefinition("open_app", "Launches an installed app on device", mapOf("appName" to "String")),
        ToolDefinition("search_contact", "Searches contacts by name", mapOf("name" to "String")),
        ToolDefinition("make_call", "Places a call to a contact or number", mapOf("target" to "String")),
        ToolDefinition("set_reminder", "Schedules an alarm/reminder", mapOf("text" to "String")),
        ToolDefinition("web_search", "Searches the web for facts/answers", mapOf("query" to "String")),
        ToolDefinition("remember", "Saves a persistent memory or user preference", mapOf("key" to "String", "value" to "String")),
        ToolDefinition("screen_share", "Toggles or requests screen sharing", emptyMap()),
        ToolDefinition("camera", "Toggles camera vision", emptyMap())
    )

    suspend fun evaluateAndExecuteCommand(command: String): ToolExecutionResult? {
        val lower = command.trim().lowercase()

        // 1. App Launch: "open minecraft", "launch youtube", "খোলো youtube"
        val openPattern = Pattern.compile("^(?:open|launch|start|চালু করো|খোলো)\\s+(.+)$", Pattern.CASE_INSENSITIVE)
        val openMatcher = openPattern.matcher(lower)
        if (openMatcher.find()) {
            val appName = openMatcher.group(1)?.trim() ?: ""
            if (appName.isNotEmpty() && !appName.contains("screen") && !appName.contains("camera")) {
                return appLauncher.launchApp(appName)
            }
        }

        // 2. Call: "call mom", "phone 123456", "ফোন করো"
        val callPattern = Pattern.compile("^(?:call|phone|dial|কল করো|ফোন করো)\\s+(.+)$", Pattern.CASE_INSENSITIVE)
        val callMatcher = callPattern.matcher(lower)
        if (callMatcher.find()) {
            val target = callMatcher.group(1)?.trim() ?: ""
            if (target.isNotEmpty()) {
                return callManager.makeCall(target)
            }
        }

        // 3. Search contact: "find contact John", "contact mom", "নাম্বার খোঁজো"
        val contactPattern = Pattern.compile("^(?:find contact|search contact|contact|খোঁজো)\\s+(.+)$", Pattern.CASE_INSENSITIVE)
        val contactMatcher = contactPattern.matcher(lower)
        if (contactMatcher.find()) {
            val target = contactMatcher.group(1)?.trim() ?: ""
            if (target.isNotEmpty()) {
                return contactManager.searchContact(target)
            }
        }

        // 4. Reminder: "remind me in 10 minutes", "10 মিনিট পরে মনে করিয়ে দিও"
        if (lower.contains("remind") || lower.contains("মনে করিয়ে")) {
            return reminderTool.createReminder(command)
        }

        // 5. Memory: "remember that my car is red", "এটা মনে রাখো"
        if (lower.contains("remember that") || lower.contains("মনে রাখো")) {
            val fact = command
                .replace(Regex("^(?:please )?remember that", RegexOption.IGNORE_CASE), "")
                .replace(Regex("^(?:দয়া করে )?মনে রাখো(?: যে)?", RegexOption.IGNORE_CASE), "")
                .trim()
            if (fact.isNotEmpty()) {
                val key = "User Note ${System.currentTimeMillis() % 1000}"
                JarvisApp.instance.memoryManager.remember(key, fact)
                return ToolExecutionResult(
                    toolName = "Memory",
                    success = true,
                    message = "I have permanently saved that to my memory."
                )
            }
        }

        // 6. Web Search: "search the web for...", "who is the president of..."
        if (lower.startsWith("search ") || lower.startsWith("google ") || lower.startsWith("web search ")) {
            val q = lower.removePrefix("search ").removePrefix("google ").removePrefix("web search ").trim()
            if (q.isNotEmpty()) {
                return webSearchTool.search(q)
            }
        }

        // 7. Screen Share toggle
        if (lower.contains("screen share") || lower.contains("স্ক্রিন শেয়ার")) {
            screenVisionTool.requestScreenShare()
            return ToolExecutionResult(
                toolName = "ScreenShare",
                success = true,
                message = "Initiating screen share request."
            )
        }

        return null
    }
}
