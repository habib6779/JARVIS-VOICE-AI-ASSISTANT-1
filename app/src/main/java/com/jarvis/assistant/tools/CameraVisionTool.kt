package com.jarvis.assistant.tools

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CameraVisionTool {

    private val _isCameraActive = MutableStateFlow(false)
    val isCameraActive: StateFlow<Boolean> = _isCameraActive.asStateFlow()

    private var latestFrame: Bitmap? = null

    fun setCameraActive(active: Boolean) {
        _isCameraActive.value = active
        if (!active) {
            latestFrame?.recycle()
            latestFrame = null
        }
    }

    fun onFrameCaptured(bitmap: Bitmap) {
        latestFrame = bitmap
    }

    fun getLatestFrame(): Bitmap? = latestFrame
}
