package com.jarvis.assistant.data.model

enum class JarvisState(val displayName: String, val orbKey: String) {
    INITIALIZING("INITIALIZING", "thinking"),
    IDLE("IDLE", "idle"),
    LISTENING("LISTENING", "listening"),
    PROCESSING("PROCESSING", "thinking"),
    SPEAKING("SPEAKING", "speaking"),
    INTERRUPTED("INTERRUPTED", "listening"),
    CONNECTING("CONNECTING...", "thinking"),
    RECONNECTING("RECONNECTING...", "thinking"),
    SLEEPING("SLEEPING", "idle"),
    ERROR("ERROR", "idle")
}
