package com.localdrop.server

import android.content.Context
import android.net.Uri
import com.localdrop.core.constants.AppConstants
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class FileStreamer(private val context: Context) {

    class OpenedRange(val stream: InputStream, val startOffset: Long, val lengthToServe: Long)

    fun openRange(uri: Uri, totalBytes: Long, startByte: Long, endByteInclusive: Long?): OpenedRange {
        val rawStream: InputStream = when (uri.scheme) {
            "file" -> {
                val file = uri.path?.let { File(it) }
                if (file != null && file.exists()) {
                    FileInputStream(file)
                } else {
                    context.contentResolver.openInputStream(uri)
                }
            }
            else -> context.contentResolver.openInputStream(uri)
        } ?: throw IllegalStateException("Unable to open file for streaming: $uri")

        val bufferedStream = BufferedInputStream(rawStream, AppConstants.STREAM_BUFFER_SIZE)

        var remainingToSkip = startByte
        while (remainingToSkip > 0) {
            val skipped = bufferedStream.skip(remainingToSkip)
            if (skipped <= 0) break
            remainingToSkip -= skipped
        }

        val end = endByteInclusive ?: (totalBytes - 1)
        val length = (end - startByte + 1).coerceAtLeast(0)

        // Bounded stream to guarantee exact bytes served for pause/resume & full download
        val bounded = object : InputStream() {
            private var bytesLeft = length

            override fun read(): Int {
                if (bytesLeft <= 0) return -1
                val b = bufferedStream.read()
                if (b != -1) bytesLeft--
                return b
            }

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (bytesLeft <= 0) return -1
                val toRead = minOf(len.toLong(), bytesLeft).toInt()
                val readCount = bufferedStream.read(b, off, toRead)
                if (readCount > 0) {
                    bytesLeft -= readCount
                }
                return readCount
            }

            override fun close() {
                bufferedStream.close()
            }
        }

        return OpenedRange(bounded, startByte, length)
    }

    companion object {
        const val BUFFER_SIZE = AppConstants.STREAM_BUFFER_SIZE
    }
}
