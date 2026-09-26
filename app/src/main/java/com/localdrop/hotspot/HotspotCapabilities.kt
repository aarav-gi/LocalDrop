package com.localdrop.hotspot

import android.content.Context
import android.net.wifi.WifiManager
import com.localdrop.core.network.NetworkInterfaceDetector

/**
 * Reports what this specific device/OS combination can actually do, so the
 * UI can show a clear fallback/error path instead of assuming every device
 * supports identical hotspot APIs.
 */
class HotspotCapabilities(private val context: Context) {

    data class Report(
        val wifiHardwareAvailable: Boolean,
        val alreadyOnWifi: Boolean,
        val canAttemptLocalOnlyHotspot: Boolean
    )

    fun evaluate(): Report {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val wifiHardwareAvailable = wifiManager?.isWifiEnabled ?: false
        val alreadyOnWifi = NetworkInterfaceDetector(context).hasUsableNetwork()
        val canAttemptLocalOnlyHotspot = LocalOnlyHotspotManager(context).hasRequiredPermission()

        return Report(
            wifiHardwareAvailable = wifiHardwareAvailable || alreadyOnWifi,
            alreadyOnWifi = alreadyOnWifi,
            canAttemptLocalOnlyHotspot = canAttemptLocalOnlyHotspot
        )
    }
}
