package com.localdrop.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface

class NetworkInterfaceDetector(private val context: Context) {

    data class LocalAddress(val interfaceName: String, val ipv4: String)

    fun findLocalIpv4Address(): LocalAddress? {
        val candidates = mutableListOf<LocalAddress>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return fallbackFromWifiManager()
            for (iface in interfaces) {
                if (!iface.isUp || iface.isLoopback) continue
                val name = iface.name.lowercase()

                // 1. Exclude Cellular / Mobile Data / VPN interfaces
                if (name.startsWith("rmnet") || name.startsWith("clat") ||
                    name.startsWith("ccmni") || name.startsWith("dummy") ||
                    name.startsWith("tun") || name.startsWith("ppp") ||
                    name.startsWith("radio") || name.startsWith("wwan")) {
                    continue
                }

                for (addr in iface.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress ?: continue

                        // Exclude carrier NAT & cellular subnet ranges
                        if (ip.startsWith("192.0.0.") || ip.startsWith("100.") || ip.startsWith("127.")) continue

                        candidates.add(LocalAddress(iface.name, ip))
                    }
                }
            }
        } catch (_: Exception) {
            return fallbackFromWifiManager()
        }

        if (candidates.isEmpty()) return fallbackFromWifiManager()

        // 2. Priority 1: Hotspot & Wi-Fi interfaces with 192.168.x.x (Standard for Android Hotspot & Wi-Fi)
        val wifiOrHotspot = candidates.filter { c ->
            val n = c.interfaceName.lowercase()
            n.startsWith("wlan") || n.startsWith("ap") || n.startsWith("swlan") || n.startsWith("softap") || n.startsWith("p2p")
        }

        val primary192 = wifiOrHotspot.firstOrNull { it.ipv4.startsWith("192.168.") }
        if (primary192 != null) return primary192

        // 3. Priority 2: Any 192.168.x.x interface
        val any192 = candidates.firstOrNull { it.ipv4.startsWith("192.168.") }
        if (any192 != null) return any192

        // 4. Priority 3: Other Wi-Fi subnets (e.g. 172.16-31.x or non-cellular 10.x on wlan)
        val wifiOther = wifiOrHotspot.firstOrNull { !it.ipv4.startsWith("10.") }
            ?: wifiOrHotspot.firstOrNull()
        if (wifiOther != null) return wifiOther

        return candidates.firstOrNull() ?: fallbackFromWifiManager()
    }

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
        } catch (_: Exception) {
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
