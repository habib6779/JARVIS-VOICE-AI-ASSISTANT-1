package com.jarvis.assistant.tools

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.gson.JsonParser
import com.jarvis.assistant.data.model.ToolExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class WebSearchTool(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNet = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNet) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun search(query: String): ToolExecutionResult = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext ToolExecutionResult(
                toolName = "WebSearch",
                success = false,
                message = "Internet connection is unavailable. Cannot perform web search."
            )
        }

        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "JARVIS-Assistant/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext ToolExecutionResult(
                    toolName = "WebSearch",
                    success = false,
                    message = "Web search server returned error code ${response.code}."
                )
            }

            val body = response.body?.string() ?: ""
            val json = JsonParser.parseString(body).asJsonObject

            val abstractText = json.get("AbstractText")?.asString ?: ""
            val heading = json.get("Heading")?.asString ?: ""

            if (abstractText.isNotBlank()) {
                ToolExecutionResult(
                    toolName = "WebSearch",
                    success = true,
                    message = "Search result for $heading:\n$abstractText",
                    data = abstractText
                )
            } else {
                val relatedTopics = json.getAsJsonArray("RelatedTopics")
                if (relatedTopics != null && relatedTopics.size() > 0) {
                    val firstTopic = relatedTopics.get(0).asJsonObject
                    val text = firstTopic.get("Text")?.asString ?: ""
                    if (text.isNotBlank()) {
                        return@withContext ToolExecutionResult(
                            toolName = "WebSearch",
                            success = true,
                            message = text,
                            data = text
                        )
                    }
                }
                ToolExecutionResult(
                    toolName = "WebSearch",
                    success = true,
                    message = "No direct instant summary found on the web for '$query'.",
                    data = null
                )
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                toolName = "WebSearch",
                success = false,
                message = "Search request failed: ${e.message}"
            )
        }
    }
}
