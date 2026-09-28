package com.jarvis.assistant.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class AndroidTTSFallback(
    private val context: Context,
    private val onSpeechStart: () -> Unit = {},
    private val onSpeechDone: () -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private val isInitialized = AtomicBoolean(false)

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.ENGLISH
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onSpeechStart()
                }

                override fun onDone(utteranceId: String?) {
                    onSpeechDone()
                }

                override fun onError(utteranceId: String?) {
                    onSpeechDone()
                }
            })
            isInitialized.set(true)
        }
    }

    fun speak(text: String, languageCode: String = "en") {
        if (!isInitialized.get()) return

        val locale = when (languageCode.lowercase()) {
            "bn", "bangla" -> Locale.forLanguageTag("bn-BD")
            else -> Locale.ENGLISH
        }
        val langResult = tts?.setLanguage(locale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.language = Locale.ENGLISH
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_utterance_${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized.set(false)
    }
}
