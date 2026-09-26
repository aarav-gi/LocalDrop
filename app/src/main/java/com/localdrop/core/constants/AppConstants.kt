package com.localdrop.core.constants

object AppConstants {
    const val NOTIFICATION_CHANNEL_ID = "localdrop_sharing_channel"
    const val NOTIFICATION_ID = 4201

    // Streaming buffer size used when copying bytes from a content:// Uri to
    // the HTTP response. Chosen to balance throughput vs. memory use — we
    // never load a whole file into RAM, only this buffer at a time.
    const val STREAM_BUFFER_SIZE = 64 * 1024

    // How long an idle session stays valid before it's rejected.
    const val SESSION_TIMEOUT_MILLIS = 30 * 60 * 1000L // 30 minutes

    // Simple per-IP request rate limit for the embedded server.
    const val RATE_LIMIT_WINDOW_MILLIS = 1000L
    const val RATE_LIMIT_MAX_REQUESTS_PER_WINDOW = 20
}
