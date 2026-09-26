package com.localdrop.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Discovers the actual local IPv4 address to bind/advertise the HTTP server
 * on, at runtime. Deliberately never assumes 192.168.43.1 or any fixed
 * address — different OEMs, Android versions, and LocalOnlyHotspot vs.
 * regular Wi-Fi all hand out different ranges.
 */
class NetworkInterfaceDetector(private val context: Context) {

    data class LocalAddress(val interfaceName: String, val ipv4: String)

    /**
     * Walks every active, non-loopback network interface looking for an
     * IPv4 address. Prefers a typical hotspot/Wi-Fi-Direct interface name
     * (wlan, ap, swlan, etc.) if more than one candidate exists, but falls
     * back to the first valid address found so we still work on devices
     * that name things differently.
     */
    fun findLocalIpv4Address(): LocalAddress? {
        val candidates = mutableListOf<LocalAddress>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (iface in interfaces) {
                if (!iface.isUp || iface.isLoopback) continue
                for (addr in iface.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        candidates.add(LocalAddress(iface.name, addr.hostAddress ?: continue))
                    }
                }
            }
        } catch (e: Exception) {
            return fallbackFromWifiManager()
        }

        if (candidates.isEmpty()) return fallbackFromWifiManager()

        val preferredPrefixes = listOf("ap", "wlan", "swlan", "softap", "p2p")
        return candidates.firstOrNull { c ->
            preferredPrefixes.any { c.interfaceName.startsWith(it, ignoreCase = true) }
        } ?: candidates.first()
    }

    /** Last-resort fallback using WifiManager's connection info. */
    @Suppress("DEPRECATION")
    private fun fallbackFromWifiManager(): LocalAddress? {
        return try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
            val ip = wifiManager.connectionInfo?.ipAddress ?: return null
            if (ip == 0) return null
            val bytes = byteArrayOf(
                (ip and 0xff).toByte(),
                (ip shr 8 and 0xff).toByte(),
                (ip shr 16 and 0xff).toByte(),
                (ip shr 24 and 0xff).toByte()
            )
            LocalAddress("wlan-fallback", bytes.joinToString(".") { (it.toInt() and 0xff).toString() })
        } catch (e: Exception) {
            null
        }
    }

    fun hasUsableNetwork(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
    }
}
