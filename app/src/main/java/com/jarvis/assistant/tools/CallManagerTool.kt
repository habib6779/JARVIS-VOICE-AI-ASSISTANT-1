package com.jarvis.assistant.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.jarvis.assistant.data.model.ToolExecutionResult

class CallManagerTool(private val context: Context) {

    fun makeCall(phoneNumberOrName: String): ToolExecutionResult {
        val cleanNumber = phoneNumberOrName.trim()
        if (cleanNumber.isBlank()) {
            return ToolExecutionResult(
                toolName = "CallManager",
                success = false,
                message = "Phone number or recipient name is required."
            )
        }

        // If it's a name, resolve via contacts first if possible
        var targetNumber = cleanNumber
        if (!cleanNumber.matches(Regex("^[+0-9\\s\\-()]+$"))) {
            val contactTool = ContactManagerTool(context)
            val result = contactTool.searchContact(cleanNumber)
            val list = result.data as? List<*>
            if (!list.isNullOrEmpty()) {
                val firstMatch = list[0].toString()
                val parts = firstMatch.split(":")
                if (parts.size >= 2) {
                    targetNumber = parts[1].trim()
                }
            } else {
                return ToolExecutionResult(
                    toolName = "CallManager",
                    success = false,
                    message = "Could not find contact '$cleanNumber' in your address book."
                )
            }
        }

        val uri = Uri.parse("tel:${Uri.encode(targetNumber)}")

        return try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                val callIntent = Intent(Intent.ACTION_CALL, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(callIntent)
                ToolExecutionResult(
                    toolName = "CallManager",
                    success = true,
                    message = "Calling $targetNumber."
                )
            } else {
                // Safe dial fallback
                val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
                ToolExecutionResult(
                    toolName = "CallManager",
                    success = true,
                    message = "Opened dialer for $targetNumber."
                )
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                toolName = "CallManager",
                success = false,
                message = "Failed to initiate call: ${e.message}"
            )
        }
    }
}
