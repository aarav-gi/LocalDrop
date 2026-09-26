package com.localdrop.server

import android.content.Context
import android.net.Uri
import com.localdrop.core.constants.AppConstants
import java.io.InputStream

/**
 * Opens a content:// Uri for buffered, seekable streaming. Never reads the
 * whole file into a ByteArray/RAM — the caller (DownloadRoute) hands the
 * returned InputStream straight to the HTTP response, which reads it in
 * AppConstants.STREAM_BUFFER_SIZE chunks.
 */
class FileStreamer(private val context: Context) {

    class OpenedRange(val stream: InputStream, val startOffset: Long, val lengthToServe: Long)

    /**
     * @param startByte inclusive start offset (0 for a full, non-range request)
     * @param endByteInclusive inclusive end offset, or null for "to end of file"
     */
    fun openRange(uri: Uri, totalBytes: Long, startByte: Long, endByteInclusive: Long?): OpenedRange {
        val raw = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Unable to open file for streaming")

        // Skip to the requested start offset. Loop because skip() is not
        // guaranteed to skip the full requested amount in one call.
        var remainingToSkip = startByte
        while (remainingToSkip > 0) {
            val skipped = raw.skip(remainingToSkip)
            if (skipped <= 0) break
            remainingToSkip -= skipped
        }

        val end = endByteInclusive ?: (totalBytes - 1)
        val length = (end - startByte + 1).coerceAtLeast(0)
        return OpenedRange(raw, startByte, length)
    }

    companion object {
        const val BUFFER_SIZE = AppConstants.STREAM_BUFFER_SIZE
    }
}
