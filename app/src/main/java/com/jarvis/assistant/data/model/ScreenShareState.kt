package com.jarvis.assistant.data.model

enum class ScreenShareState {
    IDLE,
    REQUESTING_PERMISSION,
    ACTIVE,
    USER_CANCELLED,
    ERROR
}
