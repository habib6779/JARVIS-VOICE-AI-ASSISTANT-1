package com.jarvis.assistant.voice

import android.content.Context
import android.util.Log
import com.jarvis.assistant.JarvisApp
import com.jarvis.assistant.audio.*
import com.jarvis.assistant.data.model.JarvisState
import com.jarvis.assistant.data.preferences.AppPreferences
import com.jarvis.assistant.data.repository.ChatRepository
import com.jarvis.assistant.network.GeminiLiveWebSocket
import com.jarvis.assistant.tools.ToolExecutionManager
import com.jarvis.assistant.tts.AndroidTTSFallback
import com.jarvis.assistant.util.PromptGenerator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class VoiceEngine(
    private val context: Context,
    private val preferences: AppPreferences,
    private val chatRepository: ChatRepository,
    val toolManager: ToolExecutionManager = ToolExecutionManager(context)
) {
    companion object {
        private const val TAG = "VoiceEngine"
    }

    private val _jarvisState = MutableStateFlow(JarvisState.IDLE)
    val jarvisState: StateFlow<JarvisState> = _jarvisState.asStateFlow()

    private val _connectionStatus = MutableStateFlow("READY")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow: SharedFlow<String> = _eventFlow.asSharedFlow()

    private var audioRecorder: AudioRecorder? = null
    private var audioPlayer: AudioPlayer? = null
    private var liveWebSocket: GeminiLiveWebSocket? = null
    private var audioFocusManager: AudioFocusManager? = null
    private var ttsFallback: AndroidTTSFallback? = null

    private val vad = VoiceActivityDetector()
    private var bargeInManager: BargeInManager? = null
    private var turnDetector: TurnDetector? = null

    private var engineScope: CoroutineScope? = null
    private val currentTurnText = StringBuilder()
    private val currentUserSpeechText = StringBuilder()

    init {
        ttsFallback = AndroidTTSFallback(context)
        audioFocusManager = AudioFocusManager(
            context,
            onFocusLost = {
                if (_jarvisState.value == JarvisState.SPEAKING) {
                    audioPlayer?.flush()
                }
            },
            onFocusGained = {
                // Focus restored
            }
        )
    }

    fun start(scope: CoroutineScope) {
        if (_jarvisState.value != JarvisState.IDLE && _jarvisState.value != JarvisState.SLEEPING) {
            return
        }
        engineScope = scope
        val apiKey = preferences.apiKey.trim()
        if (apiKey.isBlank()) {
            scope.launch {
                _eventFlow.emit("Please configure your Gemini API Key in Settings first.")
            }
            return
        }

        _jarvisState.value = JarvisState.CONNECTING
        currentTurnText.clear()
        currentUserSpeechText.clear()

        audioFocusManager?.requestAudioFocus()

        // 1. AudioPlayer setup (24kHz Mono playback)
        audioPlayer = AudioPlayer(
            onPlaybackStateChanged = { isPlaying ->
                if (isPlaying) {
                    _jarvisState.value = JarvisState.SPEAKING
                } else {
                    finalizeTurn()
                    if (_jarvisState.value == JarvisState.SPEAKING) {
                        _jarvisState.value = JarvisState.IDLE
                    }
                }
            },
            onPlaybackAmplitude = { amp ->
                if (_jarvisState.value == JarvisState.SPEAKING) {
                    _audioLevel.value = amp
                }
            }
        ).apply { start() }

        // 2. Barge-in & Turn Detector
        bargeInManager = BargeInManager {
            Log.d(TAG, "Barge-in triggered: interrupting assistant speech")
            audioPlayer?.flush()
            ttsFallback?.stop()
            currentTurnText.clear()
            _jarvisState.value = JarvisState.LISTENING
        }

        turnDetector = TurnDetector {
            if (_jarvisState.value == JarvisState.LISTENING) {
                _jarvisState.value = JarvisState.PROCESSING
            }
        }

        // 3. Gemini Live WebSocket Connection
        val systemPrompt = PromptGenerator.generateSystemPrompt(
            personality = preferences.personality,
            userName = preferences.userName,
            languagePreference = preferences.languagePreference
        )

        liveWebSocket = GeminiLiveWebSocket(
            apiKey = apiKey,
            model = preferences.aiModel,
            voiceName = preferences.voice,
            systemPrompt = systemPrompt,
            listener = object : GeminiLiveWebSocket.Listener {
                override fun onConnectionStateChanged(status: String) {
                    _connectionStatus.value = status
                    if (status == "LIVE") {
                        if (_jarvisState.value == JarvisState.CONNECTING || _jarvisState.value == JarvisState.RECONNECTING) {
                            _jarvisState.value = JarvisState.LISTENING
                        }
                    } else if (status.contains("RECONNECTING")) {
                        _jarvisState.value = JarvisState.RECONNECTING
                    } else if (status == "OFFLINE") {
                        if (_jarvisState.value != JarvisState.SLEEPING) {
                            _jarvisState.value = JarvisState.IDLE
                        }
                    }
                }

                override fun onAudioDataReceived(pcmData: ByteArray) {
                    _jarvisState.value = JarvisState.SPEAKING
                    audioPlayer?.enqueueAudio(pcmData)
                }

                override fun onAssistantTextReceived(textChunk: String) {
                    currentTurnText.append(textChunk)
                }

                override fun onInterrupted() {
                    audioPlayer?.flush()
                    currentTurnText.clear()
                    _jarvisState.value = JarvisState.LISTENING
                }

                override fun onTurnCompleted() {
                    // Turn audio drained in AudioPlayer
                }

                override fun onError(message: String) {
                    scope.launch {
                        _eventFlow.emit(message)
                    }
                }
            }
        ).apply {
            connect(scope)
        }

        // 4. Audio Recorder setup (16kHz Mono recording with hardware AEC)
        audioRecorder = AudioRecorder { chunk, amplitude ->
            val isSpeaking = (_jarvisState.value == JarvisState.SPEAKING)

            // Check for barge-in interruption while assistant speaks
            bargeInManager?.checkBargeIn(isSpeaking, amplitude)

            if (!isSpeaking) {
                _audioLevel.value = amplitude
                val isSpeech = vad.isSpeech(amplitude)
                turnDetector?.onAudioFrame(isSpeech)

                if (isSpeech && _jarvisState.value != JarvisState.PROCESSING) {
                    _jarvisState.value = JarvisState.LISTENING
                }

                liveWebSocket?.sendAudioChunk(chunk)
            }
        }.apply {
            setMuted(preferences.isMicMuted)
            start(scope)
        }
    }

    fun handleTextCommand(command: String) {
        engineScope?.launch {
            _jarvisState.value = JarvisState.PROCESSING
            val toolResult = toolManager.evaluateAndExecuteCommand(command)
            if (toolResult != null) {
                chatRepository.addTurn(
                    userText = command,
                    jarvisText = toolResult.message
                )
                ttsFallback?.speak(toolResult.message, preferences.languagePreference)
                _jarvisState.value = JarvisState.IDLE
            } else {
                _jarvisState.value = JarvisState.IDLE
            }
        }
    }

    private fun finalizeTurn() {
        val reply = currentTurnText.toString().trim()
        if (reply.isNotEmpty()) {
            chatRepository.addTurn(
                userText = "Spoken conversation turn",
                jarvisText = reply
            )
            currentTurnText.clear()
        }
    }

    fun stop() {
        finalizeTurn()

        audioRecorder?.stop()
        audioRecorder = null

        audioPlayer?.flush()
        audioPlayer?.release()
        audioPlayer = null

        liveWebSocket?.disconnect()
        liveWebSocket = null

        ttsFallback?.stop()
        audioFocusManager?.abandonAudioFocus()

        _jarvisState.value = JarvisState.IDLE
        _connectionStatus.value = "READY"
        _audioLevel.value = 0f
    }

    fun sleep() {
        stop()
        _jarvisState.value = JarvisState.SLEEPING
        _connectionStatus.value = "SLEEPING"
    }

    fun wake(scope: CoroutineScope) {
        if (_jarvisState.value == JarvisState.SLEEPING) {
            _jarvisState.value = JarvisState.IDLE
            start(scope)
        }
    }

    fun setMuted(muted: Boolean) {
        preferences.isMicMuted = muted
        audioRecorder?.setMuted(muted)
    }

    fun isMuted(): Boolean = preferences.isMicMuted

    fun release() {
        stop()
        ttsFallback?.release()
        ttsFallback = null
    }
}
