package com.localdrop.server

import java.io.InputStream

/**
 * Wraps a source InputStream and invokes [onBytesRead] with the cumulative
 * count as data flows through — used to feed live progress/speed into
 * TransferManager without buffering the file in memory.
 */
class ProgressReportingInputStream(
    private val source: InputStream,
    private val startingOffset: Long,
    private val onBytesRead: (cumulativeBytes: Long) -> Unit
) : InputStream() {

    private var cumulative = startingOffset

    override fun read(): Int {
        val b = source.read()
        if (b != -1) {
            cumulative += 1
            onBytesRead(cumulative)
        }
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val n = source.read(b, off, len)
        if (n > 0) {
            cumulative += n
            onBytesRead(cumulative)
        }
        return n
    }

    override fun close() {
        source.close()
    }
}
