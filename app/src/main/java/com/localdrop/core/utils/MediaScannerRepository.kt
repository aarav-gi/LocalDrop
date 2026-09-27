package com.localdrop.core.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class SelectableItem(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val appIcon: Drawable? = null
)

class MediaScannerRepository(private val context: Context) {

    suspend fun getPhotos(): List<SelectableItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SelectableItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE
        )
        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

                while (cursor.moveToNext() && list.size < 200) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Photo_$id.jpg"
                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol) ?: "image/jpeg"
                    val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                    if (size > 0) {
                        list.add(SelectableItem(uri, name, size, mime))
                    }
                }
            }
        } catch (_: Exception) {}
        list
    }

    suspend fun getVideos(): List<SelectableItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SelectableItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE
        )
        try {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)

                while (cursor.moveToNext() && list.size < 200) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Video_$id.mp4"
                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol) ?: "video/mp4"
                    val uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                    if (size > 0) {
                        list.add(SelectableItem(uri, name, size, mime))
                    }
                }
            }
        } catch (_: Exception) {}
        list
    }

    suspend fun getInstalledApps(): List<SelectableItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SelectableItem>()
        try {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in packages) {
                // Ignore pure system packages without launcher icon
                if ((app.flags and ApplicationInfo.FLAG_SYSTEM) != 0 && (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                    continue
                }
                val apkFile = File(app.sourceDir)
                if (apkFile.exists()) {
                    val appName = pm.getApplicationLabel(app).toString()
                    val icon = try { pm.getApplicationIcon(app) } catch (_: Exception) { null }
                    list.add(
                        SelectableItem(
                            uri = Uri.fromFile(apkFile),
                            name = "$appName.apk",
                            sizeBytes = apkFile.length(),
                            mimeType = "application/vnd.android.package-archive",
                            appIcon = icon
                        )
                    )
                }
            }
            list.sortBy { it.name.lowercase() }
        } catch (_: Exception) {}
        list
    }

    suspend fun getDocuments(): List<SelectableItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SelectableItem>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE
        )
        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"

        val args = arrayOf(
            "%pdf%", "%text%", "%document%", "%sheet%",
            "%.zip", "%.apk"
        )

        try {
            context.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                args,
                "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

                while (cursor.moveToNext() && list.size < 200) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Document_$id"
                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol) ?: "application/octet-stream"
                    val uri = Uri.withAppendedPath(MediaStore.Files.getContentUri("external"), id.toString())
                    if (size > 0) {
                        list.add(SelectableItem(uri, name, size, mime))
                    }
                }
            }
        } catch (_: Exception) {}
        list
    }
}
