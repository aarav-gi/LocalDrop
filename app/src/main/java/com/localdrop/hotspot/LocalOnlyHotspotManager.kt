package com.localdrop.hotspot

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Thin wrapper around the documented, supported
 * WifiManager.startLocalOnlyHotspot() API (API 26+). This is the only
 * hotspot-creation path this app uses — no undocumented/root-only APIs.
 * On many devices/OS versions this requires ACCESS_FINE_LOCATION to be
 * granted; that is checked before calling.
 */
class LocalOnlyHotspotManager(private val context: Context) {

    sealed class Result {
        data class Success(val reservation: WifiManager.LocalOnlyHotspotReservation) : Result()
        data class Failed(val reason: String) : Result()
    }

    fun hasRequiredPermission(): Boolean {
        // LocalOnlyHotspot has historically required fine location; some
        // OEM/OS combinations also gate it behind NEARBY_WIFI_DEVICES on 33+.
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val nearbyWifi = ContextCompat.checkSelfPermission(
                context, Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
            return fineLocation || nearbyWifi
        }
        return fineLocation
    }

    suspend fun start(): Result = suspendCancellableCoroutine { cont ->
        if (!hasRequiredPermission()) {
            cont.resume(Result.Failed("Missing required permission for local hotspot"))
            return@suspendCancellableCoroutine
        }

        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManager == null) {
            cont.resume(Result.Failed("Wi-Fi service unavailable on this device"))
            return@suspendCancellableCoroutine
        }

        try {
            wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                    if (cont.isActive) cont.resume(Result.Success(reservation))
                }

                override fun onStopped() {
                    // Handled by caller observing HotspotState; nothing to resume here
                    // since either onStarted or onFailed will have already completed
                    // this coroutine.
                }

                override fun onFailed(reason: Int) {
                    if (cont.isActive) {
                        cont.resume(Result.Failed(describeFailure(reason)))
                    }
                }
            }, null)
        } catch (e: Exception) {
            cont.resume(Result.Failed(e.message ?: "Unknown hotspot error"))
        }
    }

    private fun describeFailure(reason: Int): String = when (reason) {
        WifiManager.LocalOnlyHotspotCallback.ERROR_NO_CHANNEL -> "No channel available for local hotspot"
        WifiManager.LocalOnlyHotspotCallback.ERROR_GENERIC -> "Local hotspot failed to start"
        WifiManager.LocalOnlyHotspotCallback.ERROR_INCOMPATIBLE_MODE -> "Device Wi-Fi mode is incompatible with local hotspot"
        WifiManager.LocalOnlyHotspotCallback.ERROR_TETHERING_DISALLOWED -> "Tethering/hotspot disallowed by device policy"
        else -> "Local hotspot failed (code $reason)"
    }
}
