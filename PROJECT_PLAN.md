Project Plan - KMP Image Selector

Overview
- Build a Compose Multiplatform image selector with a Material 3 slide-up bottom sheet UI.
- Android uses MediaStore to list images and preserves existing EXIF (including GPS) when present.
- iOS uses Photos/PHPicker, preserving metadata when possible and reporting when EXIF retention fails.

Goals
- Cross-platform API with platform-specific implementations.
- Preserve existing EXIF only; do not inject or strip location data.
- Smooth bottom sheet UX with paging grid of recent images.
- Clear error handling for permission and source availability.

Non-Goals
- No camera capture in the initial release.
- No manual EXIF editing UI.
- No desktop image picker in the initial release.

Architecture (SOLID-aligned)
- Single Responsibility
  - ImageAssetSource: list assets only.
  - ImageSelectionResolver: resolve asset to final selection and preserve EXIF.
  - PermissionProvider: check/request permissions.
  - ImagePickerViewModel: UI state orchestration only.
- Open/Closed
  - Add sources (camera, files) by new implementations, no UI changes.
- Liskov Substitution
  - All sources respect the same paging and error semantics.
- Interface Segregation
  - Separate contracts for listing, resolving, thumbnails, and permissions.
- Dependency Inversion
  - UI depends on interfaces; platform modules provide implementations.

Module Layout
- core-domain
  - Models: ImageAsset, ImageSelection, GeoPoint, ImagePickerError
  - Interfaces: ImageAssetSource, ImageSelectionResolver, PermissionProvider, ImagePickerController
- core-ui
  - ImageSelectorBottomSheet
  - ImagePickerViewModel (or state holder)
  - UI state models (Loading, Ready, Empty, Error)
- platform-android
  - MediaStoreImageSource
  - AndroidImageSelectionResolver
  - AndroidPermissionProvider
  - Thumbnail loader (ContentResolver.loadThumbnail or ImageDecoder)
- platform-ios
  - PhotosImageSource
  - IOSImageSelectionResolver
  - IOSPermissionProvider
- app
  - Wiring/DI, theme, entry points

API Contracts (common)
- ImageAssetSource
  - suspend fun loadPage(page: Int, pageSize: Int): List<ImageAsset>
  - fun hasMore(page: Int, pageSize: Int): Boolean
- ImageSelectionResolver
  - suspend fun resolve(asset: ImageAsset): ImageSelection
- PermissionProvider
  - suspend fun ensureReadAccess(): Boolean
- ImagePickerController
  - val source: ImageAssetSource
  - val resolver: ImageSelectionResolver
  - val permission: PermissionProvider

UI Design (Bottom Sheet)
- Uses Material 3 ModalBottomSheet.
- Header: title + close.
- Body: grid of thumbnails using LazyVerticalGrid.
- States: loading shimmer, empty state, error with retry.
- Selection
  - Single-select by default; multi-select optional later.

Android Implementation Plan
- MediaStoreImageSource
  - Query MediaStore.Images with sort by DATE_TAKEN desc.
  - Map results to ImageAsset with content:// URIs.
- Resolver
  - Copy original bytes to app cache.
  - Read EXIF using ExifInterface; re-write unchanged to preserve tags.
  - If EXIF read/write succeeds: exifPreserved=true; else false.
- Permissions
  - READ_MEDIA_IMAGES (API 33+) or READ_EXTERNAL_STORAGE (pre-33).

iOS Implementation Plan
- PhotosImageSource
  - Use PHFetchOptions sorted by creationDate desc.
  - Map assets to ImageAsset with local identifiers.
- Resolver
  - Export original via PHAssetResourceManager to cache for best EXIF retention.
  - If original export fails, use image data export and set exifPreserved=false.
- Permissions
  - PHPicker for selection; no mandatory prompt on first use.

Component Flow Overview
1) Entry + Presentation
   - Host screen triggers ImageSelectorBottomSheet.
   - Bottom sheet opens via Material 3 ModalBottomSheet.
   - Shared state holder initializes and begins permission check.
2) Permission Gate
   - PermissionProvider.ensureReadAccess() runs.
   - Android: READ_MEDIA_IMAGES (33+) or READ_EXTERNAL_STORAGE (pre-33).
   - iOS: PHPicker path resolves immediately without a mandatory prompt.
   - Denied access shows error UI with retry.
3) Asset Loading
   - UI requests ImageAssetSource.loadPage(0, pageSize).
   - Android: MediaStore query sorted by date.
   - iOS: Photos fetch sorted by creation date.
   - Results mapped into ImageAsset list.
4) UI States
   - Loading: shimmer/skeleton placeholders.
   - Ready: thumbnail grid in LazyVerticalGrid.
   - Empty: no items with guidance.
   - Error: message with retry.
5) Thumbnail Rendering
   - Android: ContentResolver.loadThumbnail or ImageDecoder fallback.
   - iOS: PHImageManager with target size.
6) Selection
   - Single-select returns immediately.
   - Multi-select (later) collects selections and confirms.
7) Resolve Selection (EXIF preservation)
   - UI calls ImageSelectionResolver.resolve(asset).
   - Android: copy original to cache, read/write EXIF unchanged.
   - iOS: export original via PHAssetResourceManager or fallback.
   - exifPreserved=true only when metadata retention succeeds.
8) Result Delivery
   - onSelected(ImageSelection) invoked and sheet dismissed.
   - Host receives final uri, byteSize, mimeType, exifPreserved flag.
9) Error Handling
   - Source failures or permission issues surface in UI.
   - EXIF failures return image with exifPreserved=false.
10) Cleanup
   - Cached files remain until app cleanup policy is applied.

Milestones
1) Foundation (Day 1-2)
   - Define shared interfaces and models.
   - Create bottom sheet UI skeleton and state model.
2) Android Source + Resolver (Day 3-4)
   - MediaStore listing, thumbnails, EXIF preservation.
3) iOS Source + Resolver (Day 5-6)
   - PHFetch listing, original export, EXIF preservation best-effort.
4) UX polish + error states (Day 7)
   - Loading, empty, error, retry UX.
5) Integration + validation (Day 8)
   - Host in app, manual tests on device.

Testing Strategy
- Unit tests (common)
  - Paging logic and state transitions in ImagePickerViewModel.
- Android instrumentation
  - Verify EXIF retained on sample images with GPS data.
- iOS manual
  - Validate exifPreserved flag with original export success/failure.

Risks and Mitigations
- EXIF loss due to iOS export path
  - Mitigation: use PHAssetResourceManager for original data; signal fallback.
- Permission variability across Android versions
  - Mitigation: version-specific permission helper and user guidance.
- Large image memory use
  - Mitigation: thumbnail loading only; defer full resolution until resolve().

Deliverables
- Shared interfaces and models in commonMain.
- Android MediaStore listing + EXIF-preserving resolver.
- iOS Photos listing + best-effort EXIF preserving resolver.
- Bottom sheet UI with loading/empty/error states.
