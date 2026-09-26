package com.localdrop.core.constants

object AppConstants {
    const val NOTIFICATION_CHANNEL_ID = "localdrop_sharing_channel"
    const val NOTIFICATION_ID = 4201

    // Ultra high-speed 1MB chunk buffer for local Wi-Fi direct saturation
    const val STREAM_BUFFER_SIZE = 1024 * 1024

    // How long an idle session stays valid before it's rejected.
    const val SESSION_TIMEOUT_MILLIS = 30 * 60 * 1000L

    // High throughput per-IP request rate limit
    const val RATE_LIMIT_WINDOW_MILLIS = 1000L
    const val RATE_LIMIT_MAX_REQUESTS_PER_WINDOW = 200
}
