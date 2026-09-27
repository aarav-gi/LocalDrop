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

                // Exclude mobile data, CLAT, VPN, and cellular interfaces
                if (name.startsWith("rmnet") || name.startsWith("clat") ||
                    name.startsWith("ccmni") || name.startsWith("dummy") ||
                    name.startsWith("tun") || name.startsWith("ppp")) {
                    continue
                }

                for (addr in iface.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress ?: continue
                        // Exclude 192.0.0.x (Cellular 464XLAT internal translation)
                        if (ip.startsWith("192.0.0.")) continue
                        candidates.add(LocalAddress(iface.name, ip))
                    }
                }
            }
        } catch (_: Exception) {
            return fallbackFromWifiManager()
        }

        if (candidates.isEmpty()) return fallbackFromWifiManager()

        // 1. Hotspot & Wi-Fi interfaces prioritized
        val preferredPrefixes = listOf("ap", "wlan", "swlan", "softap", "p2p", "rndis")
        val wifiOrHotspot = candidates.filter { c ->
            preferredPrefixes.any { c.interfaceName.lowercase().startsWith(it) }
        }

        // 2. Pick valid local private IP (192.168.x.x, 10.x.x.x, 172.16-31.x.x)
        val privateCandidate = wifiOrHotspot.firstOrNull { isPrivateSubnet(it.ipv4) }
            ?: candidates.firstOrNull { isPrivateSubnet(it.ipv4) }

        return privateCandidate ?: wifiOrHotspot.firstOrNull() ?: candidates.firstOrNull() ?: fallbackFromWifiManager()
    }

    private fun isPrivateSubnet(ip: String): Boolean {
        return ip.startsWith("192.168.") || ip.startsWith("10.") || ip.matches(Regex("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*"))
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
