package com.example.kmpicture.domain

data class ImageAsset(
    val id: String,
    val displayName: String?,
    val mimeType: String?,
    val timestampMillis: Long,
    val width: Int?,
    val height: Int?,
    val previewUri: String,
)

data class ImageSelection(
    val uri: String,
    val byteSize: Long,
    val mimeType: String?,
    val exifPreserved: Boolean,
    val originalTimestampMillis: Long?,
    val originalLocation: GeoPoint?,
)

data class GeoPoint(
    val lat: Double,
    val lon: Double,
)

sealed interface ImagePickerError {
    data object PermissionDenied : ImagePickerError
    data object SourceUnavailable : ImagePickerError
    data class PlatformError(val message: String) : ImagePickerError
}

enum class SelectionMode {
    Single,
    Multiple,
}
