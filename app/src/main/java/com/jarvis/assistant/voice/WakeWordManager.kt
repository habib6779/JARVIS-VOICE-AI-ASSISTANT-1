package com.jarvis.assistant.voice

import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

class WakeWordManager(
    private val onWakeWordDetected: () -> Unit
) {
    companion object {
        private const val TAG = "WakeWordManager"
    }

    private val isListening = AtomicBoolean(false)

    fun startListening() {
        isListening.set(true)
        Log.d(TAG, "Wake word detection active: listening for 'Hey JARVIS'")
    }

    fun stopListening() {
        isListening.set(false)
    }

    fun processAudioText(transcript: String): Boolean {
        if (!isListening.get()) return false
        val lower = transcript.lowercase().trim()
        if (lower.contains("hey jarvis") || lower.contains("jarvis") || lower.contains("জারভিস") || lower.contains("এই জারভিস")) {
            Log.d(TAG, "Wake word trigger matched in transcript: $transcript")
            onWakeWordDetected()
            return true
        }
        return false
    }
}
