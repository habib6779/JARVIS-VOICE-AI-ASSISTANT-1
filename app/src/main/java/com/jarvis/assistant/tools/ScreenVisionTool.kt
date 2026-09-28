package com.jarvis.assistant.tools

import android.graphics.Bitmap
import com.jarvis.assistant.data.model.ScreenShareState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScreenVisionTool {

    private val _screenState = MutableStateFlow(ScreenShareState.IDLE)
    val screenState: StateFlow<ScreenShareState> = _screenState.asStateFlow()

    private var latestBitmap: Bitmap? = null

    fun requestScreenShare() {
        if (_screenState.value != ScreenShareState.ACTIVE) {
            _screenState.value = ScreenShareState.REQUESTING_PERMISSION
        }
    }

    fun onPermissionGranted() {
        _screenState.value = ScreenShareState.ACTIVE
    }

    fun onPermissionCancelled() {
        _screenState.value = ScreenShareState.USER_CANCELLED
    }

    fun stopScreenShare() {
        _screenState.value = ScreenShareState.IDLE
        latestBitmap?.recycle()
        latestBitmap = null
    }

    fun updateScreenFrame(bitmap: Bitmap) {
        latestBitmap = bitmap
    }

    fun getLatestFrame(): Bitmap? = latestBitmap
}
