package com.jarvis.assistant.tools

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.jarvis.assistant.data.model.ToolExecutionResult

class AppLauncherTool(private val context: Context) {

    fun launchApp(appName: String): ToolExecutionResult {
        val cleanName = appName.trim().lowercase()
        val pm = context.packageManager

        // Known direct package mappings for popular apps
        val knownPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "whatsapp" to "com.whatsapp",
            "minecraft" to "com.mojang.minecraftpe",
            "camera" to "com.google.android.GoogleCamera",
            "settings" to "com.android.settings",
            "maps" to "com.google.android.apps.maps",
            "clock" to "com.google.android.deskclock",
            "gmail" to "com.google.android.gm",
            "calculator" to "com.google.android.calculator",
            "files" to "com.google.android.documentsui",
            "roblox" to "com.roblox.client"
        )

        // Check if direct package exists
        val directPackage = knownPackages[cleanName]
        if (directPackage != null) {
            val intent = pm.getLaunchIntentForPackage(directPackage)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return ToolExecutionResult(
                    toolName = "AppLauncher",
                    success = true,
                    message = "Opened $appName successfully."
                )
            }
        }

        // Search through all installed applications
        try {
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in packages) {
                val label = pm.getApplicationLabel(appInfo).toString().lowercase()
                if (label.contains(cleanName) || cleanName.contains(label)) {
                    val intent = pm.getLaunchIntentForPackage(appInfo.packageName)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        return ToolExecutionResult(
                            toolName = "AppLauncher",
                            success = true,
                            message = "Opened ${pm.getApplicationLabel(appInfo)}."
                        )
                    }
                }
            }
        } catch (e: Exception) {
            return ToolExecutionResult(
                toolName = "AppLauncher",
                success = false,
                message = "Failed to launch $appName: ${e.message}"
            )
        }

        return ToolExecutionResult(
            toolName = "AppLauncher",
            success = false,
            message = "Application '$appName' could not be found installed on this device."
        )
    }
}
