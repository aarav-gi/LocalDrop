package com.localdrop.server

/** Parses a `Range: bytes=START-END` header per RFC 7233 (single range only). */
object RangeHeader {

    data class Parsed(val start: Long, val endInclusive: Long?)

    fun parse(headerValue: String?, totalBytes: Long): Parsed? {
        if (headerValue.isNullOrBlank() || !headerValue.startsWith("bytes=")) return null
        val spec = headerValue.removePrefix("bytes=").substringBefore(',') // ignore multi-range, serve first
        val parts = spec.split("-")
        if (parts.size != 2) return null

        return try {
            when {
                parts[0].isBlank() -> {
                    // suffix range: bytes=-500 => last 500 bytes
                    val suffixLength = parts[1].toLong()
                    val start = (totalBytes - suffixLength).coerceAtLeast(0)
                    Parsed(start, totalBytes - 1)
                }
                parts[1].isBlank() -> {
                    Parsed(parts[0].toLong(), null)
                }
                else -> {
                    Parsed(parts[0].toLong(), parts[1].toLong())
                }
            }
        } catch (e: NumberFormatException) {
            null
        }
    }
}
