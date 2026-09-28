package com.jarvis.assistant.tools

import android.content.Context
import android.net.Uri
import com.jarvis.assistant.data.model.ToolExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class DocumentReaderTool(private val context: Context) {

    suspend fun readDocumentFromUri(uri: Uri): ToolExecutionResult = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext ToolExecutionResult(
                    toolName = "DocumentReader",
                    success = false,
                    message = "Could not open document stream."
                )

            val reader = BufferedReader(InputStreamReader(inputStream))
            val sb = StringBuilder()
            var line: String?
            var lineCount = 0
            while (reader.readLine().also { line = it } != null && lineCount < 200) {
                sb.append(line).append("\n")
                lineCount++
            }
            reader.close()

            val text = sb.toString().trim()
            if (text.isBlank()) {
                ToolExecutionResult(
                    toolName = "DocumentReader",
                    success = false,
                    message = "Document is empty or unreadable text."
                )
            } else {
                ToolExecutionResult(
                    toolName = "DocumentReader",
                    success = true,
                    message = "Read ${text.length} characters from document:\n${text.take(300)}...",
                    data = text
                )
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                toolName = "DocumentReader",
                success = false,
                message = "Failed to read document: ${e.message}"
            )
        }
    }
}
