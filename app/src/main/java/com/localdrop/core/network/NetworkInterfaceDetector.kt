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
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return fallbackWifiOrHotspot()
            for (iface in interfaces) {
                if (!iface.isUp || iface.isLoopback) continue
                val name = iface.name.lowercase()

                // 1. Strictly block all cellular, modem, mobile data, and virtual tunnel interfaces
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

                        // STRICT BLOCK: Carrier-grade NAT & cellular subnets (Never pick 10.x or 100.x)
                        if (ip.startsWith("10.") || ip.startsWith("100.") ||
                            ip.startsWith("192.0.0.") || ip.startsWith("127.")) {
                            continue
                        }

                        candidates.add(LocalAddress(iface.name, ip))
                    }
                }
            }
        } catch (_: Exception) {
            return fallbackWifiOrHotspot()
        }

        // 2. Highest priority: Hotspot or Wi-Fi standard subnet (192.168.x.x)
        val hotspotSubnet = candidates.firstOrNull { it.ipv4.startsWith("192.168.") }
        if (hotspotSubnet != null) return hotspotSubnet

        // 3. Priority: Wi-Fi interfaces (wlan, ap, softap)
        val wifiInterface = candidates.firstOrNull { c ->
            val n = c.interfaceName.lowercase()
            n.startsWith("wlan") || n.startsWith("ap") || n.startsWith("swlan") || n.startsWith("softap")
        }
        if (wifiInterface != null) return wifiInterface

        // 4. Any remaining non-cellular candidate
        val validCandidate = candidates.firstOrNull()
        if (validCandidate != null) return validCandidate

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
                if (!wifiIp.startsWith("0.") && !wifiIp.startsWith("10.")) {
                    return LocalAddress("wlan0", wifiIp)
                }
            }
        } catch (_: Exception) {}

        // Safe Hotspot default when sender is acting as Wi-Fi Access Point
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
