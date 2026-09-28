package com.jarvis.assistant.memory

import java.util.UUID

data class MemoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val key: String,
    val value: String,
    val type: String = "PREFERENCE",
    val createdAt: Long = System.currentTimeMillis(),
    val persistent: Boolean = true
)
