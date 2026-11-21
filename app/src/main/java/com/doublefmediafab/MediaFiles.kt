package com.doublefmediafab

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

data class MediaFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val modified: Long,
    val folder: String,
    val kind: Int,
    val mime: String
) {
    val isVideo: Boolean get() = kind == VIDEO
    companion object { const val PHOTO = 0; const val VIDEO = 1 }
}

object MediaFiles {
    fun scan(context: Context): List<MediaFile> {
        val all = ArrayList<MediaFile>()
        fun collect(base: Uri, kind: Int) {
            val columns = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.RELATIVE_PATH,
                MediaStore.MediaColumns.MIME_TYPE
            )
            context.contentResolver.query(base, columns, null, null,
                MediaStore.MediaColumns.DATE_MODIFIED + " DESC")?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val folderIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
                val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    all.add(MediaFile(
                        ContentUris.withAppendedId(base, id),
                        cursor.getString(nameIndex) ?: "Média $id",
                        cursor.getLong(sizeIndex),
                        cursor.getLong(dateIndex),
                        cursor.getString(folderIndex) ?: "",
                        kind,
                        cursor.getString(mimeIndex) ?: ""
                    ))
                }
            }
        }
        collect(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaFile.PHOTO)
        collect(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaFile.VIDEO)
        return all.sortedByDescending { it.modified }
    }
}

data class MatchGroup(val items: List<MediaFile>, val label: String, val score: Int) {
    val savedBytes: Long get() = items.sortedByDescending { it.size }
        .drop(1).sumOf { it.size }
}
