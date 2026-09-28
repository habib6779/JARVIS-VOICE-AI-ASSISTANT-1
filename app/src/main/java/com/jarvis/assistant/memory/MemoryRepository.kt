package com.jarvis.assistant.memory

import com.jarvis.assistant.data.preferences.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MemoryRepository(private val preferences: AppPreferences) {

    private val _memories = MutableStateFlow<List<MemoryEntry>>(emptyList())
    val memories: StateFlow<List<MemoryEntry>> = _memories.asStateFlow()

    init {
        _memories.value = preferences.loadMemories()
    }

    @Synchronized
    fun addOrUpdateMemory(key: String, value: String, type: String = "PREFERENCE") {
        val current = _memories.value.toMutableList()
        val index = current.indexOfFirst { it.key.equals(key, ignoreCase = true) }
        val entry = MemoryEntry(key = key.trim(), value = value.trim(), type = type)
        if (index >= 0) {
            current[index] = entry
        } else {
            current.add(entry)
        }
        _memories.value = current
        preferences.saveMemories(current)
    }

    @Synchronized
    fun removeMemory(key: String) {
        val current = _memories.value.toMutableList()
        current.removeAll { it.key.equals(key, ignoreCase = true) }
        _memories.value = current
        preferences.saveMemories(current)
    }

    @Synchronized
    fun clearAll() {
        _memories.value = emptyList()
        preferences.saveMemories(emptyList())
    }

    fun getMemory(key: String): String? {
        return _memories.value.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
    }

    fun getFormattedMemoryContext(): String {
        val list = _memories.value
        if (list.isEmpty()) return "None"
        return list.joinToString("\n") { "- ${it.key}: ${it.value}" }
    }
}
