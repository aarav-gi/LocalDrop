package com.localdrop.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.wifi.WifiManager
import android.os.Build
import java.net.Inet4Address
import java.net.NetworkInterface

class NetworkInterfaceDetector(private val context: Context) {

    data class LocalAddress(val interfaceName: String, val ipv4: String)

    fun findLocalIpv4Address(): LocalAddress? {
        // Priority 1: Check ConnectivityManager LinkProperties (most accurate on Android 10+)
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                for (network in cm.allNetworks) {
                    val caps = cm.getNetworkCapabilities(network) ?: continue
                    val isWifi = caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                    if (isWifi) {
                        val lp = cm.getLinkProperties(network)
                        val ip = lp?.linkAddresses?.mapNotNull { it.address as? Inet4Address }
                            ?.firstOrNull { !it.isLoopbackAddress }
                            ?.hostAddress
                        if (ip != null) {
                            return LocalAddress(lp.interfaceName ?: "wlan0", ip)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Priority 2: Enumerate all active network interfaces
        val candidates = mutableListOf<LocalAddress>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            if (interfaces != null) {
                for (iface in interfaces) {
                    if (!iface.isUp || iface.isLoopback) continue
                    val name = iface.name.lowercase()

                    // Exclude cellular modems & virtual tunnels
                    if (name.startsWith("rmnet") || name.startsWith("ccmni") ||
                        name.startsWith("clat") || name.startsWith("dummy") ||
                        name.startsWith("tun") || name.startsWith("ppp") ||
                        name.startsWith("radio") || name.startsWith("wwan") ||
                        name.startsWith("v4-") || name.startsWith("seth")) {
                        continue
                    }

                    for (addr in iface.inetAddresses) {
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            val ip = addr.hostAddress ?: continue
                            if (ip.startsWith("127.")) continue
                            candidates.add(LocalAddress(iface.name, ip))
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Pick specific Hotspot interface first (ap, softap, swlan)
        val hotspotIface = candidates.firstOrNull { c ->
            val n = c.interfaceName.lowercase()
            n.startsWith("ap") || n.startsWith("softap") || n.startsWith("swlan")
        }
        if (hotspotIface != null) return hotspotIface

        // Pick standard Wi-Fi (wlan0)
        val wifiIface = candidates.firstOrNull { it.interfaceName.lowercase().startsWith("wlan") }
        if (wifiIface != null) return wifiIface

        // Pick any standard 192.168.x.x
        val private192 = candidates.firstOrNull { it.ipv4.startsWith("192.168.") }
        if (private192 != null) return private192

        if (candidates.isNotEmpty()) return candidates.first()

        return fallbackWifiOrHotspot()
    }

    @Suppress("DEPRECATION")
    private fun fallbackWifiOrHotspot(): LocalAddress {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ip = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ip != 0) {
                val bytes = byteArrayOf(
                    (ip and 0xff).toByte(),
                    (ip shr 8 and 0xff).toByte(),
                    (ip shr 16 and 0xff).toByte(),
                    (ip shr 24 and 0xff).toByte()
                )
                val wifiIp = bytes.joinToString(".") { (it.toInt() and 0xff).toString() }
                if (!wifiIp.startsWith("0.")) {
                    return LocalAddress("wlan0", wifiIp)
                }
            }
        } catch (_: Exception) {}

        return LocalAddress("ap0", "192.168.43.1")
    }

    fun hasUsableNetwork(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
    }
}
