package com.jarvis.assistant.audio

class VoiceActivityDetector(
    private val speechThreshold: Float = 0.07f
) {
    fun isSpeech(amplitude: Float): Boolean {
        return amplitude > speechThreshold
    }
}
