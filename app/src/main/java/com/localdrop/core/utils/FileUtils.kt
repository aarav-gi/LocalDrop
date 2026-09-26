package com.localdrop.core.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

data class PickedFileMeta(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String
)

object FileUtils {

    /** Reads display name + size for a content:// Uri via the OpenableColumns projection. */
    fun readMeta(context: Context, uri: Uri): PickedFileMeta {
        var name = "file"
        var size = -1L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
                if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            }
        }
        val mime = MimeTypeUtils.resolveMimeType(context.contentResolver, uri)
        return PickedFileMeta(uri, name, size, mime)
    }
}
