package com.localdrop.core.utils

import kotlin.math.pow

object SizeFormatter {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            .coerceIn(0, units.size - 1)
        val value = bytes / 1024.0.pow(digitGroups)
        return if (digitGroups == 0) "${bytes} ${units[0]}"
        else String.format("%.1f %s", value, units[digitGroups])
    }

    /** e.g. "38.6 MB/s" */
    fun formatSpeed(bytesPerSecond: Double): String {
        if (bytesPerSecond <= 0) return "0 B/s"
        return "${formatBytes(bytesPerSecond.toLong())}/s"
    }

    fun formatEta(seconds: Long): String {
        if (seconds < 0) return "--"
        if (seconds < 60) return "${seconds}s"
        val m = seconds / 60
        val s = seconds % 60
        return if (m < 60) "${m}m ${s}s" else "${m / 60}h ${m % 60}m"
    }
}
