package com.localdrop.server

import java.io.InputStream

class ProgressReportingInputStream(
    private val source: InputStream,
    private val startingOffset: Long,
    private val onBytesRead: (cumulativeBytes: Long) -> Unit
) : InputStream() {

    private var cumulative = startingOffset
    private var lastReportedBytes = startingOffset
    private var lastReportedTime = System.currentTimeMillis()

    companion object {
        private const val UPDATE_THRESHOLD_BYTES = 1024 * 1024L // 1 MB batch
        private const val UPDATE_INTERVAL_MILLIS = 350L         // 350 ms throttle
    }

    override fun read(): Int {
        val b = source.read()
        if (b != -1) {
            cumulative += 1
            checkAndReport(force = false)
        }
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val n = source.read(b, off, len)
        if (n > 0) {
            cumulative += n
            checkAndReport(force = false)
        }
        return n
    }

    private fun checkAndReport(force: Boolean) {
        val now = System.currentTimeMillis()
        val bytesSince = cumulative - lastReportedBytes
        val timeSince = now - lastReportedTime

        if (force || bytesSince >= UPDATE_THRESHOLD_BYTES || timeSince >= UPDATE_INTERVAL_MILLIS) {
            lastReportedBytes = cumulative
            lastReportedTime = now
            onBytesRead(cumulative)
        }
    }

    override fun close() {
        checkAndReport(force = true)
        source.close()
    }
}
