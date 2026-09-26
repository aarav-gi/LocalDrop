package com.localdrop.core.security

/**
 * Because files are only ever addressed by opaque random tokens (never a
 * filesystem path segment), classic path traversal ("../../etc") has no
 * meaning here — there is no path to traverse. This still validates that
 * incoming path segments look like tokens we actually issued, so malformed
 * or hostile input is rejected before it ever reaches SessionManager.
 */
object AccessController {

    private val TOKEN_PATTERN = Regex("^[A-Za-z0-9_-]{8,64}$")

    fun isPlausibleToken(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        if (raw.contains("..") || raw.contains("/") || raw.contains("\\")) return false
        return TOKEN_PATTERN.matches(raw)
    }
}
