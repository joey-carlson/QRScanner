# QR Scanner - Project Instructions

## Platform & Language
- Android application: minSdk 28, targetSdk 35, compileSdk 35
- Kotlin with Android KTX patterns
- Material Design 3 with light/dark theme support
- 16KB page size compatibility for Android 15+
- Builds done in Android Studio (not CLI)

## Architecture
- MVVM with ViewModels, ViewModelFactories, LiveData/StateFlow
- Repository pattern for data persistence (JSON file storage)
- Strategy pattern for exports
- Centralized config: `AppConfig`, `AppConstants`, `PreferenceKeys`
- ViewBinding for type-safe view references
- Manual dependency injection with factory pattern

## Kotlin Conventions
- Follow Kotlin coding conventions
- Leverage null safety properly
- Use data classes for models
- Prefer immutable data structures
- Use extension functions for utility methods

## UI Requirements
- Theme-aware colors (`?attr/colorSurface`, `?attr/colorOnSurface`, etc.) — never hardcode
- Consistent purple button theming (#6200EE)
- WindowInsetsHelper for system UI overlap handling
- ConstraintLayout for responsive design
- Haptic feedback (HapticManager) on scan events

## Data Management
- JSON files for all persistent data (checkout, checkin, kit bundles, inventory)
- SharedPreferences for settings and scan history
- App-private internal storage with MediaStore compatibility
- Location-aware file naming with date patterns (MM-dd-yy)
- Repository pattern with proper error handling

## Camera & Scanning
- CameraX for camera operations with lifecycle management
- ML Kit for barcode scanning and OCR text recognition
- Image preprocessing: contrast/brightness/gamma correction for OCR
- Scan modes: Barcode Only, OCR Only
- Multi-factor confidence scoring with environmental adaptation
- Visual feedback: flash effects, haptic, confirmation overlays

## Export System
- UniversalExportManager singleton entry point
- ExportDataSource interface for data type abstraction
- Formats: JSON, CSV, XML, TXT, Kit Labels CSV
- Destinations: local storage, Android share, AWS S3
- S3UploadManager with exponential backoff retry

## Testing
- JUnit 4 + Mockito + Robolectric for unit tests
- AAA pattern (Arrange, Act, Assert) — keep tests short
- Gist tests for core functionality coverage
- Parameterized tests where possible
- Fuzz testing considerations where applicable

## Version & Git
- Semantic versioning x.y.z with versionCode increments
- Conventional Commits: `<type>(<scope>): <subject>`
  - Types: feat, fix, docs, style, refactor, perf, test, chore, ci
- Commit and push after completing work sessions
- Remote: https://github.com/joey-carlson/QRScanner

## Naming Rules
- Use versioning (x.y.z) not subjective labels ("enhanced", "improved", "new", etc.)
- Exported files: descriptive purpose-based names, no implied versioning
- APK naming: `PilotScanner-<buildType>-v<version>-<code>.apk`

## Documentation
- CHANGELOG.md: Keep a Changelog format, prepend new entries above old
- README.md: Current project overview
- REQUIREMENTS.md: Versioned specifications
- BUILD_INFO.txt: Build metadata
- PARKING_LOT.md: Future features and housekeeping backlog
- Never overwrite historical entries — append above existing content

## WorkDocs Sync (Major Releases)
Create version folder at:
`/Users/joecrls/Library/CloudStorage/WorkDocsDrive-Documents/Joey's Scripts/PilotScanner/vX.Y.Z-BUILD_YYYY-MM-DD_HH-MM`

Copy: BUILD_INFO.txt, REQUIREMENTS.md, CHANGELOG.md, README.md
If debug build available, create debug/ subfolder with APK.

## Security
- Input validation: all scanned values sanitized (BarcodeValidator)
- App-private storage preferred
- HTTPS for all AWS S3 operations
- Secure AWS credential storage
- JSON structure validation on file operations
- No sensitive data in logs
