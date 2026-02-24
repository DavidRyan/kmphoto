# KMPicture

Compose Multiplatform image selector with a Material 3 bottom sheet. Android uses MediaStore and preserves existing EXIF (including GPS) when present.

## Quick Start

Add the `kmpicture` module to your app and call the composable:

```kotlin
import com.example.kmpicture.ui.ImageSelectorBottomSheet

ImageSelectorBottomSheet(
    visible = showPicker,
    onDismiss = { showPicker = false },
    onSelected = { selection ->
        // Use selection.uri, selection.exifPreserved, selection.originalLocation
    },
)
```

## Permissions (Android)
The library requests permission at runtime. You must declare the manifest permission in your app:

- Android 13+ (API 33+): `android.permission.READ_MEDIA_IMAGES`
- Android 12 and below: `android.permission.READ_EXTERNAL_STORAGE`

Example manifest entry:

```xml
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

## Advanced Customization (Internal)
This repository contains internal modules (`core-domain`, `core-ui`, `platform-android`, `platform-ios`) used to build `kmpicture`.
End users should depend on `kmpicture` only.

## EXIF Behavior
- Preserves existing EXIF when present.
- Does not inject new location data.
- If EXIF retention fails, `ImageSelection.exifPreserved` is `false`.

## Selection Modes
Default is single-select. To enable multi-select:

```kotlin
import com.example.kmpicture.domain.SelectionMode

ImageSelectorBottomSheet(
    visible = showPicker,
    onDismiss = { showPicker = false },
    onSelected = { /* single */ },
    onSelectedMultiple = { selections -> /* multi */ },
    selectionMode = SelectionMode.Multiple,
)
```

## Current iOS Status
- Listing and EXIF preservation are stubbed.
- The API is ready for implementation using `Photos`/`PHPicker`.

## Notes
- Android thumbnails use MediaStore; pre-29 decodes are sampled to reduce memory usage.
- Paging loads more items as you scroll.
