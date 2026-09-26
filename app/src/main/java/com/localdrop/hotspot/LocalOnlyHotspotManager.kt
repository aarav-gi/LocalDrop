package com.localdrop.hotspot

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocalOnlyHotspotManager(private val context: Context) {

    private var activeReservation: WifiManager.LocalOnlyHotspotReservation? = null

    sealed class Result {
        data class Success(val reservation: WifiManager.LocalOnlyHotspotReservation) : Result()
        data class Failed(val reason: String) : Result()
    }

    fun hasRequiredPermission(): Boolean {
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

    fun stop() {
        try {
            activeReservation?.close()
        } catch (_: Exception) {}
        activeReservation = null
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

        // Agar pichla session system me phasa ho toh pehle force clean karein
        stop()

        try {
            wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                    activeReservation = reservation
                    if (cont.isActive) cont.resume(Result.Success(reservation))
                }

                override fun onStopped() {
                    activeReservation = null
                }

                override fun onFailed(reason: Int) {
                    activeReservation = null
                    if (cont.isActive) {
                        cont.resume(Result.Failed(describeFailure(reason)))
                    }
                }
            }, null)

            cont.invokeOnCancellation {
                stop()
            }
        } catch (e: Exception) {
            activeReservation = null
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
