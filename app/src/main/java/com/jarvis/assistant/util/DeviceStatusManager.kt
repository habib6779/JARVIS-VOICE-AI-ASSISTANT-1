package com.jarvis.assistant.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import android.telephony.TelephonyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DeviceStatus(
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val networkLabel: String = "Wi-Fi",
    val timeLabel: String = "00:00"
)

class DeviceStatusManager(private val context: Context) {

    private val _status = MutableStateFlow(DeviceStatus())
    val status: StateFlow<DeviceStatus> = _status.asStateFlow()

    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            updateBattery(intent)
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateNetwork()
        }

        override fun onLost(network: Network) {
            updateNetwork()
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            updateNetwork()
        }
    }

    fun startMonitoring(scope: CoroutineScope) {
        // Battery sticky intent & receiver
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val initialBattery = context.registerReceiver(batteryReceiver, filter)
        updateBattery(initialBattery)

        // Network callback
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm?.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            // Ignore if restricted
        }
        updateNetwork()

        // Clock loop
        scope.launch(Dispatchers.Default) {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            while (isActive) {
                val timeStr = sdf.format(Date())
                _status.value = _status.value.copy(timeLabel = timeStr)
                delay(1000)
            }
        }
    }

    private fun updateBattery(intent: Intent?) {
        if (intent == null) return
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 100
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL)

        _status.value = _status.value.copy(
            batteryPercent = pct,
            isCharging = isCharging
        )
    }

    private fun updateNetwork() {
        var label = "Offline"
        if (cm != null) {
            val active = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(active)
            if (caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                label = when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                        val netName = tm?.networkOperatorName
                        if (!netName.isNullOrBlank()) "4G/5G ($netName)" else "Cellular 5G"
                    }
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                    else -> "Connected"
                }
            }
        }
        _status.value = _status.value.copy(networkLabel = label)
    }

    fun stopMonitoring() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // Ignored if already unregistered
        }
        try {
            cm?.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Ignored
        }
    }
}
