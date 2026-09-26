package com.localdrop.core.utils

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap

object MimeTypeUtils {

    fun resolveMimeType(contentResolver: ContentResolver, uri: Uri): String {
        contentResolver.getType(uri)?.let { return it }
        val extension = uri.toString().substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"
    }
}
