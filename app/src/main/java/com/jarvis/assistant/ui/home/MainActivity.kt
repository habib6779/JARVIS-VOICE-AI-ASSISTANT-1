package com.jarvis.assistant.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.jarvis.assistant.JarvisApp
import com.jarvis.assistant.R
import com.jarvis.assistant.data.model.JarvisState
import com.jarvis.assistant.data.model.ScreenShareState
import com.jarvis.assistant.databinding.ActivityMainBinding
import com.jarvis.assistant.service.JarvisForegroundService
import com.jarvis.assistant.ui.chat.ChatHistoryBottomSheet
import com.jarvis.assistant.ui.orb.OrbHelper
import com.jarvis.assistant.ui.settings.SettingsActivity
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var orbHelper: OrbHelper
    private lateinit var gestureDetector: GestureDetector

    // Audio Permission Launcher
    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            try {
                JarvisForegroundService.start(this)
            } catch (e: Exception) {
                // Ignore if background start is restricted
            }
            viewModel.toggleVoiceSession()
        } else {
            Toast.makeText(this, "Microphone permission is required for voice conversation.", Toast.LENGTH_LONG).show()
        }
    }

    // Camera Permission Launcher
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.toggleCamera()
        } else {
            Toast.makeText(this, "Camera permission was not granted.", Toast.LENGTH_SHORT).show()
        }
    }

    // Screen Share Media Projection Launcher
    private val screenShareLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            viewModel.onScreenSharePermissionResult(true)
        } else {
            viewModel.onScreenSharePermissionResult(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupOrb()
        setupGestures()
        setupListeners()
        observeViewModel()
        checkFirstLaunchApiKey()

        // Start background service only if audio permission is already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            try {
                JarvisForegroundService.start(this)
            } catch (e: Exception) {
                // Ignore if restricted
            }
        }
    }

    override fun onResume() {
        super.onResume()
        orbHelper.resume()
        viewModel.onAppResumed()
    }

    override fun onPause() {
        super.onPause()
        // Gaming / resource saving optimization
        orbHelper.pause()
        viewModel.onAppPaused()
    }

    private fun setupOrb() {
        orbHelper = OrbHelper(binding.webViewOrb)
        orbHelper.setup()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null) {
            gestureDetector.onTouchEvent(ev)
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun setupGestures() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 120
            private val SWIPE_VELOCITY_THRESHOLD = 150

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX < 0) {
                            // Left swipe -> Open History
                            showChatHistory()
                            return true
                        } else {
                            // Right swipe -> Open Settings
                            openSettings()
                            return true
                        }
                    }
                }
                return false
            }
        })
    }

    private fun setupListeners() {
        // Tapping Center Title -> History
        binding.centerTitleLayout.setOnClickListener {
            showChatHistory()
        }

        // 1. Screen Share Button
        binding.btnScreenShare.setOnClickListener {
            val state = viewModel.screenShareState.value
            if (state == ScreenShareState.ACTIVE) {
                viewModel.toggleScreenShare()
            } else {
                val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
                if (mediaProjectionManager != null) {
                    screenShareLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                }
            }
        }

        // 2. Microphone Button (Voice Engine Toggle)
        binding.btnMicrophone.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) {
                viewModel.toggleVoiceSession()
            } else {
                requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        // 3. Camera Button
        binding.btnCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                viewModel.toggleCamera()
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        // 4. Sleep Button
        binding.btnSleep.setOnClickListener {
            viewModel.toggleSleep()
        }
    }

    private fun showChatHistory() {
        ChatHistoryBottomSheet.newInstance().show(
            supportFragmentManager,
            ChatHistoryBottomSheet.TAG
        )
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun checkFirstLaunchApiKey() {
        if (isFinishing || isDestroyed) return
        val prefs = JarvisApp.instance.preferences
        if (prefs.apiKey.isBlank() && !prefs.isFirstLaunchCompleted) {
            val container = android.widget.FrameLayout(this).apply {
                val paddingPx = (20 * resources.displayMetrics.density).toInt()
                setPadding(paddingPx, paddingPx / 2, paddingPx, paddingPx / 2)
            }
            val input = EditText(this).apply {
                hint = "Paste your Gemini API Key here"
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                setHintTextColor(ContextCompat.getColor(context, R.color.text_muted))
                setBackgroundResource(R.drawable.bg_input_field)
                val padInner = (14 * resources.displayMetrics.density).toInt()
                setPadding(padInner, padInner, padInner, padInner)
            }
            container.addView(input)

            AlertDialog.Builder(this)
                .setTitle("Welcome to JARVIS")
                .setMessage("Please enter your Google Gemini API Key to enable real-time speech conversation. You can also configure this later in Settings.")
                .setView(container)
                .setPositiveButton("Save") { _, _ ->
                    val key = input.text.toString().trim()
                    if (key.isNotBlank()) {
                        prefs.apiKey = key
                        Toast.makeText(this, "API Key saved. Ready to connect!", Toast.LENGTH_SHORT).show()
                    }
                    prefs.isFirstLaunchCompleted = true
                }
                .setNegativeButton("Skip") { _, _ ->
                    prefs.isFirstLaunchCompleted = true
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Device Status: Battery, Network, Wi-Fi, Time
                launch {
                    viewModel.deviceStatus.collect { status ->
                        binding.tvBattery.text = "${status.batteryPercent}%"
                        binding.tvNetwork.text = status.networkLabel
                        binding.tvWifi.text = if (status.networkLabel == "Wi-Fi") "Wi-Fi" else "Data"
                        binding.tvLiveTime.text = status.timeLabel
                    }
                }

                // Jarvis State
                launch {
                    viewModel.jarvisState.collect { state ->
                        updateJarvisStateUi(state)
                    }
                }

                // Audio Level -> Orb Visualizer
                launch {
                    viewModel.audioLevel.collect { level ->
                        orbHelper.updateAudioLevel(level)
                    }
                }

                // Screen Share State
                launch {
                    viewModel.screenShareState.collect { state ->
                        updateScreenShareUi(state)
                    }
                }

                // Camera Active State
                launch {
                    viewModel.isCameraActive.collect { active ->
                        updateCameraUi(active)
                    }
                }

                // Notification & Status Events
                launch {
                    viewModel.eventFlow.collect { message ->
                        Snackbar.make(binding.rootLayout, message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun updateJarvisStateUi(state: JarvisState) {
        binding.tvConversationState.text = state.displayName
        val colorRes = when (state) {
            JarvisState.IDLE -> R.color.text_secondary
            JarvisState.LISTENING -> R.color.primary_cyan
            JarvisState.PROCESSING -> R.color.purple_glow
            JarvisState.SPEAKING -> R.color.neon_blue
            JarvisState.INTERRUPTED -> R.color.status_amber
            JarvisState.SLEEPING -> R.color.text_muted
            JarvisState.ERROR -> R.color.status_red
            else -> R.color.primary_cyan
        }
        binding.tvConversationState.setTextColor(ContextCompat.getColor(this, colorRes))
        orbHelper.updateState(state)

        // Mic Button visuals
        val isRecordingOrSpeaking = (state == JarvisState.LISTENING || state == JarvisState.SPEAKING || state == JarvisState.PROCESSING)
        if (isRecordingOrSpeaking) {
            binding.btnMicrophone.setBackgroundResource(R.drawable.bg_bottom_action_active)
            binding.ivMicrophoneIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_cyan))
            binding.tvMicrophoneLabel.setTextColor(ContextCompat.getColor(this, R.color.primary_cyan))
        } else {
            binding.btnMicrophone.setBackgroundResource(R.drawable.bg_bottom_action)
            binding.ivMicrophoneIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text_secondary))
            binding.tvMicrophoneLabel.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        }

        // Sleep Button visuals
        if (state == JarvisState.SLEEPING) {
            binding.tvSleepLabel.text = "Wake Up"
            binding.tvSleepLabel.setTextColor(ContextCompat.getColor(this, R.color.primary_cyan))
            binding.ivSleepIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_cyan))
        } else {
            binding.tvSleepLabel.text = "Sleep"
            binding.tvSleepLabel.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
            binding.ivSleepIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text_muted))
        }
    }

    private fun updateScreenShareUi(state: ScreenShareState) {
        if (state == ScreenShareState.ACTIVE) {
            binding.btnScreenShare.setBackgroundResource(R.drawable.bg_bottom_action_active)
            binding.ivScreenShareIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_cyan))
            binding.tvScreenShareLabel.setTextColor(ContextCompat.getColor(this, R.color.primary_cyan))
            binding.tvScreenShareLabel.text = "Sharing"
        } else {
            binding.btnScreenShare.setBackgroundResource(R.drawable.bg_bottom_action)
            binding.ivScreenShareIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text_secondary))
            binding.tvScreenShareLabel.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            binding.tvScreenShareLabel.text = "Screen Share"
        }
    }

    private fun updateCameraUi(active: Boolean) {
        if (active) {
            binding.btnCamera.setBackgroundResource(R.drawable.bg_bottom_action_active)
            binding.ivCameraIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_cyan))
            binding.tvCameraLabel.setTextColor(ContextCompat.getColor(this, R.color.primary_cyan))
            binding.tvCameraLabel.text = "Vision Active"
        } else {
            binding.btnCamera.setBackgroundResource(R.drawable.bg_bottom_action)
            binding.ivCameraIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text_secondary))
            binding.tvCameraLabel.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            binding.tvCameraLabel.text = "Camera"
        }
    }
}
