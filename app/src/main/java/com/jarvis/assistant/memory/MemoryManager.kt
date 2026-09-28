package com.jarvis.assistant.memory

class MemoryManager(private val repository: MemoryRepository) {

    fun remember(key: String, value: String): String {
        repository.addOrUpdateMemory(key, value)
        return "I will remember that $key is $value."
    }

    fun recall(key: String): String {
        val memory = repository.getMemory(key)
        return if (memory != null) {
            "I remember: $key is $memory."
        } else {
            "I don't have any saved memory about $key."
        }
    }

    fun forget(key: String): String {
        repository.removeMemory(key)
        return "Memory forgotten: $key."
    }

    fun getMemoryPromptContext(): String {
        return repository.getFormattedMemoryContext()
    }
}
