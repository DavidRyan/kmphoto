package com.example.kmpicture.android

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.kmpicture.domain.GeoPoint
import com.example.kmpicture.domain.ImageAsset
import com.example.kmpicture.domain.ImageSelection
import com.example.kmpicture.domain.ImageSelectionResolver
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class AndroidImageResolver(
    private val context: Context,
) : ImageSelectionResolver {
    override suspend fun resolve(asset: ImageAsset): ImageSelection {
        val uri = Uri.parse(asset.previewUri)
        val contentResolver = context.contentResolver
        val cacheFile = File(
            context.cacheDir,
            "kmpicture_${System.currentTimeMillis()}_${asset.id}.jpg",
        )

        val hasExif = contentResolver.openInputStream(uri)?.use { input ->
            runCatching { containsExif(ExifInterface(input)) }.getOrDefault(false)
        } ?: false

        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(cacheFile).use { output ->
                input.copyTo(output)
            }
        } ?: error("Unable to open image stream")

        val exif = runCatching { ExifInterface(cacheFile.absolutePath) }.getOrNull()
        val exifPreserved = hasExif && exif != null
        val location = exif?.let { readGeoPoint(it) }
        val timestamp = exif?.let { readExifTimestampMillis(it) }

        return ImageSelection(
            uri = cacheFile.toURI().toString(),
            byteSize = cacheFile.length(),
            mimeType = asset.mimeType ?: contentResolver.getType(uri),
            exifPreserved = exifPreserved,
            originalTimestampMillis = timestamp,
            originalLocation = location,
        )
    }
}

private fun containsExif(exif: ExifInterface): Boolean {
    return exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) != null ||
        exif.getAttribute(ExifInterface.TAG_MAKE) != null ||
        exif.getAttribute(ExifInterface.TAG_MODEL) != null ||
        exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) != null ||
        exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE) != null
}

private fun readGeoPoint(exif: ExifInterface): GeoPoint? {
    val latLong = FloatArray(2)
    return if (exif.getLatLong(latLong)) {
        GeoPoint(lat = latLong[0].toDouble(), lon = latLong[1].toDouble())
    } else {
        null
    }
}

private fun readExifTimestampMillis(exif: ExifInterface): Long? {
    val raw = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: return null
    val parser = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return runCatching { parser.parse(raw)?.time }.getOrNull()
}
