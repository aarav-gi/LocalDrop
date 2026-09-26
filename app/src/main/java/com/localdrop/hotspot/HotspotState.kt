package com.localdrop.hotspot

sealed class HotspotState {
    data object Idle : HotspotState()
    data object Starting : HotspotState()
    data class Running(val ssid: String?, val usingExistingWifi: Boolean) : HotspotState()
    data class Failed(val reason: String) : HotspotState()
    data object Stopped : HotspotState()
}
