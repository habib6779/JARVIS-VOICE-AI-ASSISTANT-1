package com.jarvis.assistant.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jarvis.assistant.databinding.ActivitySplashBinding
import com.jarvis.assistant.ui.home.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            // Synchronized initialization
            binding.tvSplashStatus.text = "Loading Acoustic Pipelines..."
            delay(400)
            binding.tvSplashStatus.text = "Syncing Core Memory..."
            delay(400)
            binding.tvSplashStatus.text = "Online."

            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }
    }
}
