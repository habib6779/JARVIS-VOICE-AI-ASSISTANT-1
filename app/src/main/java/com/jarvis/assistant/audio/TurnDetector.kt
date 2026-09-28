package com.jarvis.assistant.audio

class TurnDetector(
    private val silenceThresholdMs: Long = 1200L,
    private val onTurnEnd: () -> Unit
) {
    private var lastSpeechTimeMs = 0L
    private var hasSpeechStarted = false

    fun onAudioFrame(isSpeech: Boolean) {
        val now = System.currentTimeMillis()
        if (isSpeech) {
            hasSpeechStarted = true
            lastSpeechTimeMs = now
        } else if (hasSpeechStarted) {
            if (now - lastSpeechTimeMs > silenceThresholdMs) {
                hasSpeechStarted = false
                onTurnEnd()
            }
        }
    }

    fun reset() {
        hasSpeechStarted = false
        lastSpeechTimeMs = 0L
    }
}
