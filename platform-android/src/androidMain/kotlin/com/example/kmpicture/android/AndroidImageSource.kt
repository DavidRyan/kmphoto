package com.example.kmpicture.android

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.kmpicture.domain.ImageAsset
import com.example.kmpicture.domain.ImageAssetSource

class AndroidImageSource(
    private val context: Context,
) : ImageAssetSource {
    override suspend fun loadPage(page: Int, pageSize: Int): List<ImageAsset> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        val offset = page * pageSize
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val args = android.os.Bundle().apply {
                putString(android.content.ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
                putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, pageSize)
                putInt(android.content.ContentResolver.QUERY_ARG_OFFSET, offset)
            }
            context.contentResolver.query(uri, projection, args, null)
        } else {
            val legacySort = "$sortOrder LIMIT $pageSize OFFSET $offset"
            context.contentResolver.query(uri, projection, null, null, legacySort)
        }

        cursor?.use {
            return buildList {
                while (it.moveToNext()) {
                    add(it.toImageAsset(uri))
                }
            }
        }

        return emptyList()
    }

    override fun hasMore(page: Int, pageSize: Int): Boolean {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        val total = cursor?.use { it.count } ?: 0
        return total > (page + 1) * pageSize
    }
}

private fun Cursor.toImageAsset(baseUri: Uri): ImageAsset {
    val id = getLong(getColumnIndexOrThrow(MediaStore.Images.Media._ID))
    val displayName = getStringOrNull(MediaStore.Images.Media.DISPLAY_NAME)
    val mimeType = getStringOrNull(MediaStore.Images.Media.MIME_TYPE)
    val dateTaken = getLongOrNull(MediaStore.Images.Media.DATE_TAKEN)
    val dateAdded = getLongOrNull(MediaStore.Images.Media.DATE_ADDED)
    val width = getIntOrNull(MediaStore.Images.Media.WIDTH)
    val height = getIntOrNull(MediaStore.Images.Media.HEIGHT)
    val timestampMillis = dateTaken ?: dateAdded?.times(1000L) ?: 0L
    val contentUri = ContentUris.withAppendedId(baseUri, id)

    return ImageAsset(
        id = id.toString(),
        displayName = displayName,
        mimeType = mimeType,
        timestampMillis = timestampMillis,
        width = width,
        height = height,
        previewUri = contentUri.toString(),
    )
}

private fun Cursor.getStringOrNull(columnName: String): String? {
    val index = getColumnIndex(columnName)
    if (index == -1 || isNull(index)) return null
    return getString(index)
}

private fun Cursor.getLongOrNull(columnName: String): Long? {
    val index = getColumnIndex(columnName)
    if (index == -1 || isNull(index)) return null
    return getLong(index)
}

private fun Cursor.getIntOrNull(columnName: String): Int? {
    val index = getColumnIndex(columnName)
    if (index == -1 || isNull(index)) return null
    return getInt(index)
}
