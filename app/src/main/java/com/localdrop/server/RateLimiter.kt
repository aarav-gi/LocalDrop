package com.localdrop.server

import com.localdrop.core.constants.AppConstants
import java.util.concurrent.ConcurrentHashMap

/** Lightweight per-IP request rate limiter for the embedded server. */
class RateLimiter {

    private data class Window(var windowStartMillis: Long, var count: Int)

    private val windows = ConcurrentHashMap<String, Window>()

    fun allowRequest(remoteAddress: String): Boolean {
        val now = System.currentTimeMillis()
        val window = windows.compute(remoteAddress) { _, existing ->
            if (existing == null || now - existing.windowStartMillis > AppConstants.RATE_LIMIT_WINDOW_MILLIS) {
                Window(now, 1)
            } else {
                existing.count += 1
                existing
            }
        }
        return (window?.count ?: 0) <= AppConstants.RATE_LIMIT_MAX_REQUESTS_PER_WINDOW
    }
}
