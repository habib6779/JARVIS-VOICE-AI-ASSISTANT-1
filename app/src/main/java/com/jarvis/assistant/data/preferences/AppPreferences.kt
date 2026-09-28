package com.jarvis.assistant.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jarvis.assistant.BuildConfig
import com.jarvis.assistant.data.model.ChatTurn
import com.jarvis.assistant.data.model.GeminiConstants
import com.jarvis.assistant.memory.MemoryEntry
import com.jarvis.assistant.tools.ReminderItem

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "jarvis_prefs"
        private const val KEY_API_KEY = "key_api_key"
        private const val KEY_MODEL = "key_model"
        private const val KEY_VOICE = "key_voice"
        private const val KEY_PERSONALITY = "key_personality"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_MIC_MUTED = "key_mic_muted"
        private const val KEY_CHAT_HISTORY = "key_chat_history"
        private const val KEY_LANGUAGE = "key_language"
        private const val KEY_WAKE_WORD_ENABLED = "key_wake_word_enabled"
        private const val KEY_MEMORIES = "key_memories"
        private const val KEY_REMINDERS = "key_reminders"
        private const val KEY_FIRST_LAUNCH = "key_first_launch"
        private const val KEY_ELEVENLABS_KEY = "key_elevenlabs_key"
    }

    var apiKey: String
        get() {
            val stored = prefs.getString(KEY_API_KEY, "") ?: ""
            if (stored.isNotBlank()) return stored
            return try {
                val injected = BuildConfig.GEMINI_API_KEY
                if (!injected.isNullOrBlank() && !injected.startsWith("MY_GEMINI")) {
                    injected
                } else {
                    ""
                }
            } catch (e: Exception) {
                ""
            }
        }
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    var aiModel: String
        get() {
            val stored = prefs.getString(KEY_MODEL, GeminiConstants.DEFAULT_MODEL) ?: GeminiConstants.DEFAULT_MODEL
            return if (GeminiConstants.SUPPORTED_MODELS.contains(stored)) {
                stored
            } else {
                GeminiConstants.DEFAULT_MODEL
            }
        }
        set(value) = prefs.edit().putString(KEY_MODEL, value).apply()

    var voice: String
        get() = prefs.getString(KEY_VOICE, "Aoede") ?: "Aoede"
        set(value) = prefs.edit().putString(KEY_VOICE, value).apply()

    var personality: String
        get() = prefs.getString(KEY_PERSONALITY, GeminiConstants.PERSONALITY_ASSISTANT) ?: GeminiConstants.PERSONALITY_ASSISTANT
        set(value) = prefs.edit().putString(KEY_PERSONALITY, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_NAME, value.trim()).apply()

    var languagePreference: String
        get() = prefs.getString(KEY_LANGUAGE, "auto") ?: "auto"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var isMicMuted: Boolean
        get() = prefs.getBoolean(KEY_MIC_MUTED, false)
        set(value) = prefs.edit().putBoolean(KEY_MIC_MUTED, value).apply()

    var isWakeWordEnabled: Boolean
        get() = prefs.getBoolean(KEY_WAKE_WORD_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, value).apply()

    var isFirstLaunchCompleted: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, false)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()

    var elevenLabsApiKey: String
        get() = prefs.getString(KEY_ELEVENLABS_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ELEVENLABS_KEY, value.trim()).apply()

    fun loadChatHistory(): List<ChatTurn> {
        val json = prefs.getString(KEY_CHAT_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ChatTurn>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveChatHistory(history: List<ChatTurn>) {
        val json = gson.toJson(history)
        prefs.edit().putString(KEY_CHAT_HISTORY, json).apply()
    }

    fun clearChatHistory() {
        prefs.edit().remove(KEY_CHAT_HISTORY).apply()
    }

    fun loadMemories(): List<MemoryEntry> {
        val json = prefs.getString(KEY_MEMORIES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<MemoryEntry>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveMemories(memories: List<MemoryEntry>) {
        val json = gson.toJson(memories)
        prefs.edit().putString(KEY_MEMORIES, json).apply()
    }

    fun loadReminders(): List<ReminderItem> {
        val json = prefs.getString(KEY_REMINDERS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ReminderItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveReminders(reminders: List<ReminderItem>) {
        val json = gson.toJson(reminders)
        prefs.edit().putString(KEY_REMINDERS, json).apply()
    }
}
