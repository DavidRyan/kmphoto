package com.example.kmpicture.android

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
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
        val sourceUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.setRequireOriginal(uri)
        } else {
            uri
        }
        Log.d(TAG, "Resolve image uri=$uri sourceUri=$sourceUri")
        val contentResolver = context.contentResolver
        val cacheFile = File(
            context.cacheDir,
            "kmpicture_${System.currentTimeMillis()}_${asset.id}.jpg",
        )

        val hasExif = contentResolver.openInputStream(sourceUri)?.use { input ->
            runCatching { containsExif(ExifInterface(input)) }.getOrDefault(false)
        } ?: false
        Log.d(TAG, "EXIF present=$hasExif")

        contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(cacheFile).use { output ->
                input.copyTo(output)
            }
        } ?: error("Unable to open image stream")

        val exif = runCatching { ExifInterface(cacheFile.absolutePath) }.getOrNull()
        val exifPreserved = hasExif && exif != null
        val location = exif?.let { readGeoPoint(it, contentResolver, uri) }
        val timestamp = exif?.let { readExifTimestampMillis(it) }
        Log.d(TAG, "EXIF preserved=$exifPreserved location=$location timestamp=$timestamp")

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

private const val TAG = "KMPicture"

private fun containsExif(exif: ExifInterface): Boolean {
    return exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) != null ||
        exif.getAttribute(ExifInterface.TAG_MAKE) != null ||
        exif.getAttribute(ExifInterface.TAG_MODEL) != null ||
        exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) != null ||
        exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE) != null
}

private fun readGeoPoint(
    exif: ExifInterface,
    contentResolver: android.content.ContentResolver,
    uri: Uri,
): GeoPoint? {
    val latLong = FloatArray(2)
    if (exif.getLatLong(latLong)) {
        Log.d(TAG, "GPS from ExifInterface.getLatLong")
        return GeoPoint(lat = latLong[0].toDouble(), lon = latLong[1].toDouble())
    }

    val lat = parseDms(
        exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE),
        exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF),
    )
    val lon = parseDms(
        exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE),
        exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF),
    )
    if (lat != null && lon != null) {
        Log.d(TAG, "GPS from EXIF DMS tags")
        return GeoPoint(lat = lat, lon = lon)
    }

    Log.d(TAG, "GPS not found in EXIF, trying MediaStore")
    return readMediaStoreLocation(contentResolver, uri)
}

private fun parseDms(dms: String?, ref: String?): Double? {
    if (dms.isNullOrBlank() || ref.isNullOrBlank()) return null
    val parts = dms.split(",")
    if (parts.size < 3) return null
    val degrees = parseRational(parts[0]) ?: return null
    val minutes = parseRational(parts[1]) ?: return null
    val seconds = parseRational(parts[2]) ?: return null
    var value = degrees + (minutes / 60.0) + (seconds / 3600.0)
    if (ref.equals("S", ignoreCase = true) || ref.equals("W", ignoreCase = true)) {
        value = -value
    }
    return value
}

private fun parseRational(value: String): Double? {
    val parts = value.trim().split("/")
    if (parts.size != 2) return null
    val numerator = parts[0].toDoubleOrNull() ?: return null
    val denominator = parts[1].toDoubleOrNull() ?: return null
    if (denominator == 0.0) return null
    return numerator / denominator
}

private fun readMediaStoreLocation(
    contentResolver: android.content.ContentResolver,
    uri: Uri,
): GeoPoint? {
    val projection = arrayOf(
        MediaStore.Images.Media.LATITUDE,
        MediaStore.Images.Media.LONGITUDE,
    )
    contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return null
        val latIndex = cursor.getColumnIndex(MediaStore.Images.Media.LATITUDE)
        val lonIndex = cursor.getColumnIndex(MediaStore.Images.Media.LONGITUDE)
        if (latIndex == -1 || lonIndex == -1) return null
        val lat = cursor.getDouble(latIndex)
        val lon = cursor.getDouble(lonIndex)
        if (lat == 0.0 && lon == 0.0) return null
        Log.d(TAG, "GPS from MediaStore lat=$lat lon=$lon")
        return GeoPoint(lat = lat, lon = lon)
    }
    return null
}

private fun readExifTimestampMillis(exif: ExifInterface): Long? {
    val raw = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: return null
    val parser = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return runCatching { parser.parse(raw)?.time }.getOrNull()
}
