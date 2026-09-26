package com.localdrop.hotspot

import android.content.Context
import android.net.wifi.WifiManager
import com.localdrop.core.network.NetworkInterfaceDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Establishes local connectivity for sharing, preferring the simplest
 * working option:
 *
 *  1. If the device is already associated with a Wi-Fi network (e.g. both
 *     sender and receiver are on the same home/office Wi-Fi already), just
 *     use that — no hotspot needed at all.
 *  2. Otherwise, fall back to the supported LocalOnlyHotspot API so sharing
 *     still works with no router and no internet.
 *
 * Every path here uses documented Android APIs only.
 */
class WifiNetworkManager(private val context: Context) {

    private val _state = MutableStateFlow<HotspotState>(HotspotState.Idle)
    val state: StateFlow<HotspotState> = _state

    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null
    private val localOnlyHotspotManager = LocalOnlyHotspotManager(context)

    suspend fun establishConnectivity(): HotspotState {
        _state.value = HotspotState.Starting

        val detector = NetworkInterfaceDetector(context)
        if (detector.hasUsableNetwork()) {
            _state.value = HotspotState.Running(ssid = null, usingExistingWifi = true)
            return _state.value
        }

        val result = localOnlyHotspotManager.start()
        _state.value = when (result) {
            is LocalOnlyHotspotManager.Result.Success -> {
                reservation = result.reservation
                val ssid = try {
                    result.reservation.softApConfiguration?.ssid?.toString()
                } catch (e: Exception) {
                    null
                }
                HotspotState.Running(ssid = ssid, usingExistingWifi = false)
            }
            is LocalOnlyHotspotManager.Result.Failed -> HotspotState.Failed(result.reason)
        }
        return _state.value
    }

    fun teardown() {
        try {
            reservation?.close()
        } catch (e: Exception) {
            // best-effort
        } finally {
            reservation = null
            _state.value = HotspotState.Stopped
        }
    }
}
