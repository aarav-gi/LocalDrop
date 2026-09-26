package com.localdrop.hotspot

import android.content.Context
import android.net.wifi.WifiManager
import com.localdrop.core.network.NetworkInterfaceDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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

        // Naya connection shuru karne se pehle puraana clean karein
        teardown()

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
        } catch (_: Exception) {}
        reservation = null
        localOnlyHotspotManager.stop()
        _state.value = HotspotState.Stopped
    }
}
