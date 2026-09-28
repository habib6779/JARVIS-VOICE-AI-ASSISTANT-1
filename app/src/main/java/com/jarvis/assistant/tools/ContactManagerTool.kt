package com.jarvis.assistant.tools

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.jarvis.assistant.data.model.ToolExecutionResult

class ContactManagerTool(private val context: Context) {

    fun searchContact(query: String): ToolExecutionResult {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return ToolExecutionResult(
                toolName = "ContactManager",
                success = false,
                message = "Contacts permission is not granted. Please allow Contacts permission."
            )
        }

        val results = mutableListOf<String>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext() && results.size < 5) {
                    val name = cursor.getString(nameIndex)
                    val number = cursor.getString(numberIndex)
                    results.add("$name: $number")
                }
            }
        } catch (e: Exception) {
            return ToolExecutionResult(
                toolName = "ContactManager",
                success = false,
                message = "Error searching contacts: ${e.message}"
            )
        }

        return if (results.isNotEmpty()) {
            ToolExecutionResult(
                toolName = "ContactManager",
                success = true,
                message = "Found contacts:\n" + results.joinToString("\n"),
                data = results
            )
        } else {
            ToolExecutionResult(
                toolName = "ContactManager",
                success = false,
                message = "No contact found matching '$query'."
            )
        }
    }
}
