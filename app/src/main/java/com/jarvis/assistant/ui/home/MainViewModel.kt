package com.jarvis.assistant.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.assistant.JarvisApp
import com.jarvis.assistant.data.model.JarvisState
import com.jarvis.assistant.data.model.ScreenShareState
import com.jarvis.assistant.data.preferences.AppPreferences
import com.jarvis.assistant.data.repository.ChatRepository
import com.jarvis.assistant.util.DeviceStatus
import com.jarvis.assistant.util.DeviceStatusManager
import com.jarvis.assistant.voice.VoiceEngine
import com.jarvis.assistant.voice.WakeWordManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences: AppPreferences = (application as JarvisApp).preferences
    private val chatRepository: ChatRepository = (application as JarvisApp).chatRepository

    val voiceEngine = VoiceEngine(application, preferences, chatRepository)
    private val deviceStatusManager = DeviceStatusManager(application)
    private val wakeWordManager = WakeWordManager {
        viewModelScope.launch {
            if (voiceEngine.jarvisState.value == JarvisState.IDLE || voiceEngine.jarvisState.value == JarvisState.SLEEPING) {
                voiceEngine.start(viewModelScope)
            }
        }
    }

    val jarvisState: StateFlow<JarvisState> = voiceEngine.jarvisState
    val screenShareState: StateFlow<ScreenShareState> = voiceEngine.toolManager.screenVisionTool.screenState
    val isCameraActive: StateFlow<Boolean> = voiceEngine.toolManager.cameraVisionTool.isCameraActive
    val isMicMuted: StateFlow<Boolean> = MutableStateFlow(preferences.isMicMuted)
    val audioLevel: StateFlow<Float> = voiceEngine.audioLevel
    val connectionStatus: StateFlow<String> = voiceEngine.connectionStatus
    val deviceStatus: StateFlow<DeviceStatus> = deviceStatusManager.status

    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow: SharedFlow<String> = _eventFlow.asSharedFlow()

    init {
        deviceStatusManager.startMonitoring(viewModelScope)
        if (preferences.isWakeWordEnabled) {
            wakeWordManager.startListening()
        }

        viewModelScope.launch {
            voiceEngine.eventFlow.collect { message ->
                _eventFlow.emit(message)
            }
        }
    }

    fun toggleVoiceSession() {
        val state = voiceEngine.jarvisState.value
        if (state == JarvisState.IDLE || state == JarvisState.SLEEPING || state == JarvisState.ERROR) {
            voiceEngine.start(viewModelScope)
        } else {
            voiceEngine.stop()
        }
    }

    fun toggleSleep() {
        if (voiceEngine.jarvisState.value == JarvisState.SLEEPING) {
            voiceEngine.wake(viewModelScope)
        } else {
            voiceEngine.sleep()
            viewModelScope.launch {
                _eventFlow.emit("JARVIS entering sleep mode.")
            }
        }
    }

    fun toggleScreenShare() {
        val current = screenShareState.value
        if (current == ScreenShareState.ACTIVE) {
            voiceEngine.toolManager.screenVisionTool.stopScreenShare()
            viewModelScope.launch {
                _eventFlow.emit("Screen sharing stopped.")
            }
        } else {
            voiceEngine.toolManager.screenVisionTool.requestScreenShare()
        }
    }

    fun onScreenSharePermissionResult(granted: Boolean) {
        if (granted) {
            voiceEngine.toolManager.screenVisionTool.onPermissionGranted()
            viewModelScope.launch {
                _eventFlow.emit("Screen sharing active.")
            }
        } else {
            voiceEngine.toolManager.screenVisionTool.onPermissionCancelled()
            viewModelScope.launch {
                _eventFlow.emit("Screen sharing permission was cancelled.")
            }
        }
    }

    fun toggleCamera() {
        val current = isCameraActive.value
        voiceEngine.toolManager.cameraVisionTool.setCameraActive(!current)
        viewModelScope.launch {
            _eventFlow.emit(if (!current) "Camera vision activated." else "Camera vision stopped.")
        }
    }

    fun refreshSettings() {
        if (preferences.isWakeWordEnabled) {
            wakeWordManager.startListening()
        } else {
            wakeWordManager.stopListening()
        }
    }

    fun onAppPaused() {
        // Gaming / resource conservation mode
    }

    fun onAppResumed() {
        refreshSettings()
    }

    override fun onCleared() {
        super.onCleared()
        deviceStatusManager.stopMonitoring()
        wakeWordManager.stopListening()
        voiceEngine.release()
    }
}
