# QR Scanner Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.5.3] - 2025-08-27
### Added
- CSV export format support for better spreadsheet compatibility
  - "Save as CSV" option to export data to Downloads folder
  - "Share CSV" option to share CSV files via Android apps
  - CSV format includes User, Kit, Timestamp, and Location columns
  - Maintains same location-aware file naming convention

## [1.5.2] - 2025-08-27
### Added
- Enhanced scan feedback system with differentiated flash colors
  - White flash (600ms) for successful scans
  - Red flash (600ms) for invalid barcode formats
- Failure event tracking in ScanViewModel

### Fixed
- Settings screen text contrast issues
  - Helper text now uses theme-aware colors
  - Input field outlines have better visibility
  - All text properly adapts to light/dark modes

## [1.5.1] - 2025-08-27
### Added
- Email export with professional formatting
  - Pre-filled subject and body content
  - Multiple file attachment support
- SMS/MMS export functionality
  - File attachments via MMS when supported
  - Warning dialog about carrier compatibility

## [1.5.0] - 2025-08-26
### Added
- Comprehensive export system (Phase 2)
  - Date range selection for exports
  - Save to Downloads folder
  - Share via Android Intent
  - JSON format with location-aware filenames
- Export Activity with Material Design date picker
- Export Method selection screen
- FileProvider configuration for secure file sharing

### Fixed
- Theme color issues in export and settings screens

## [1.4.0] - 2025-08-25
### Added
- Settings screen with device configuration
  - Location ID (required for exports)
  - Device Name (optional)
- Preference storage using SharedPreferences
- Location ID validation before exports

## [1.3.0] - 2025-08-24
### Added
- Support for "OTHER" barcode types
- Improved barcode type detection logic

## [1.2.0] - 2025-08-23
### Added
- Support for 1D barcode formats
  - Code 128, Code 39, Code 93
  - UPC-A, UPC-E
  - EAN-13, EAN-8
- Enhanced ML Kit barcode scanner configuration

## [1.1.0] - 2025-08-22
### Added
- Visual feedback system
  - White flash animation on successful scan
  - 600ms duration for better visibility
- Improved status messages

### Fixed
- Camera permission handling for Android 10+

## [1.0.0] - 2025-08-21
### Initial Release
- QR code scanning for user and kit checkouts
- Dual scanning support (user/kit in any order)
- Real-time camera preview with scan overlay
- Automatic checkout processing
- Local JSON storage in app-specific directory
- MVVM architecture with Kotlin
- Material Design UI components
