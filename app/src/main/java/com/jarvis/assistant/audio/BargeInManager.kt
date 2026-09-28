package com.jarvis.assistant.audio

class BargeInManager(
    private val bargeInThreshold: Float = 0.12f,
    private val onBargeIn: () -> Unit
) {
    private var consecutiveSpeechFrames = 0

    fun checkBargeIn(isSpeaking: Boolean, amplitude: Float): Boolean {
        if (!isSpeaking) {
            consecutiveSpeechFrames = 0
            return false
        }

        if (amplitude > bargeInThreshold) {
            consecutiveSpeechFrames++
            if (consecutiveSpeechFrames >= 2) {
                consecutiveSpeechFrames = 0
                onBargeIn()
                return true
            }
        } else {
            consecutiveSpeechFrames = 0
        }
        return false
    }

    fun reset() {
        consecutiveSpeechFrames = 0
    }
}
