package com.localdrop.server

import android.content.Context
import android.net.Uri
import com.localdrop.core.constants.AppConstants
import java.io.BufferedInputStream
import java.io.InputStream

class FileStreamer(private val context: Context) {

    class OpenedRange(val stream: InputStream, val startOffset: Long, val lengthToServe: Long)

    fun openRange(uri: Uri, totalBytes: Long, startByte: Long, endByteInclusive: Long?): OpenedRange {
        val raw = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Unable to open file for streaming")

        // 512 KB hardware read buffer wrapping content resolver
        val bufferedStream = BufferedInputStream(raw, AppConstants.STREAM_BUFFER_SIZE)

        var remainingToSkip = startByte
        while (remainingToSkip > 0) {
            val skipped = bufferedStream.skip(remainingToSkip)
            if (skipped <= 0) break
            remainingToSkip -= skipped
        }

        val end = endByteInclusive ?: (totalBytes - 1)
        val length = (end - startByte + 1).coerceAtLeast(0)
        return OpenedRange(bufferedStream, startByte, length)
    }

    companion object {
        const val BUFFER_SIZE = AppConstants.STREAM_BUFFER_SIZE
    }
}
