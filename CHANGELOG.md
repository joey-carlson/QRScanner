# QR Scanner Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.1.0] - 2025-08-30

### Added
- **OCR (Optical Character Recognition) Capabilities**
  - ML Kit Text Recognition integration for scanning printed serial numbers
  - Three scanning modes: Barcode Only (default), OCR Only, and Hybrid
  - Device Serial Number (DSN) pattern recognition and validation
  - Manual verification dialog for low-confidence OCR results
  - Confidence indicators with color-coded progress bars
  - Component type inference from DSN patterns
  - Fallback to manual text entry when scanning fails
  - Scan mode selector UI in Kit Bundle activity
  - Support for multiple DSN formats and patterns
  - Real-time text preprocessing and normalization
  - Performance-optimized scanning with frame throttling

### Technical
- New OCR package with specialized classes:
  - `HybridScanAnalyzer`: Unified scanner supporting barcode and OCR
  - `TextRecognitionAnalyzer`: ML Kit text recognition processor
  - `DsnValidator`: Pattern matching and validation for serial numbers
  - `ScanMode`: Enum for scanning mode management
  - `OcrVerificationDialog`: Manual verification UI for OCR results
  - `ScanModeSelector`: UI component for mode switching
- OCR performance requirements:
  - Processing within 3 seconds per scan attempt
  - 80% confidence threshold for automatic acceptance
  - Support for 8pt to 48pt font sizes
  - Multiple text orientations (0°, 90°, 180°, 270°)
- Version bumped to 2.1.0 (Build 21) for OCR feature

## [2.0.0] - 2025-08-30

### Added
- **Kit Bundle Feature** (Major new functionality)
  - Ability to scan and bundle individual components into kits
  - Support for 8 component types: Glasses, Controller, Battery 01-03, Pads, Unused 01-02
  - Unique Kit ID generation with format: `<BaseKitCode>-<MM/DD>`
  - Separate JSON storage for kit bundles: `qr_kits_MM-dd-yy_LocationID.json`
  - Immutable kit bundles (new bundle created for component changes)
  - Data models: KitBundle class with validation and helper methods
  - Repository: KitRepository for managing kit bundle storage
- **Home Screen Navigation**
  - New feature selection screen on app launch
  - Choice between "Check Out" and "Kit Bundle" modes
  - Back navigation from any feature to home
  - Settings button on home screen
  - Version display on home screen
- **Requirements Documentation**
  - Comprehensive requirements document with versioning system
  - Automatic copy to WorkDocs during build process
  - Living document approach for tracking project evolution

### Changed
- Major version increment to 2.0.0 due to significant new feature addition
- App structure updated to support dual-mode functionality
- MainActivity now displays "Check Out Mode" in action bar
- HomeActivity set as the launcher activity
- Updated string resources for new UI elements

### Technical
- Version bumped to 2.0.0 (Build 20) for Kit Bundle feature
- Foundation for multi-feature architecture
- Phase 1 (Data Layer) completed: KitBundle and KitRepository classes
- Phase 2 (UI Foundation) completed: HomeActivity with navigation
- Preparation for Phase 3 (Kit Bundle Activity) implementation

## [1.7.3] - 2025-08-29

### Changed
- **Major Refactoring**: Refactored ExportManager (500+ lines) into focused, single-responsibility classes:
  - `ExportCoordinator`: Main orchestration logic for exports
  - `ContentGenerator`: Handles generation of JSON, CSV, XML, and TXT formats
  - `FileNamingService`: Centralized filename generation and S3 key management
  - `TempFileManager`: Manages temporary file operations and cleanup
  - `IntentFactory`: Creates properly configured intents for sharing/email/SMS
  - `ExportDataClasses`: Contains all data classes and sealed result types
- Updated ExportMethodActivity to use new refactored classes
- Fixed nullable type handling for CheckoutRecord fields (userId, kitId)
- Added missing Constants references to AppConfig for dialog titles, messages, and progress messages

### Fixed
- Resolved compilation errors from duplicate data class declarations
- Fixed exhaustive when expression by handling all ExportResult branches
- Corrected nullable type mismatches in ContentGenerator

## [1.7.2] - 2025-08-29
### Added
- **Export Strategy Pattern Implementation**
  - New `ExportStrategy` interface for flexible export implementations
  - `BaseExportStrategy` abstract class with common functionality
  - `LocalStorageExportStrategy` for Downloads folder exports (JSON, CSV, XML, TXT)
  - `ShareExportStrategy` for Android sharing system integration
  - `ExportStrategyManager` for coordinating all export strategies
  - `ExportResultHandler` for processing export results and user actions
  - Support for multiple export formats with proper MIME type handling
  - Network connectivity checking and configuration validation
  - Comprehensive error handling and user feedback

### Technical
- Version bumped to 1.7.2 (Build 18) for export strategy pattern
- Extensible architecture for adding new export methods (S3, Email, SMS, etc.)
- Strategy pattern enables clean separation of export logic
- Foundation for Step 4 (ExportManager refactoring) implementation
- Enhanced export validation and file size checking
- Improved temporary file management and cleanup

## [1.7.1] - 2025-08-29
### Added
- **Centralized File Operations Manager**
  - New `FileManager` class for all file operations across the app
  - Android version compatibility handling (MediaStore vs direct file access)
  - Consistent error handling with `FileResult<T>` wrapper
  - Batch file operations for multiple files
  - File validation and sanitization utilities
  - Unique filename generation to prevent conflicts
  - Storage space checking and external storage validation
  - Comprehensive file cleanup and management utilities

### Technical
- Version bumped to 1.7.1 (Build 17) for FileManager implementation
- Foundation for refactoring ExportManager and CheckoutRepository
- Enhanced file security with validation and sanitization
- Improved error handling and debugging capabilities
- Preparation for export strategy pattern implementation

## [1.7.0] - 2025-08-29
### Added
- **Centralized Configuration Management System**
  - New `AppConfig` object for all application-wide constants and settings
  - New `PreferenceKeys` object for centralized SharedPreferences key management
  - Build-specific configurations with debug/release variants
  - Parameterized file provider authority using BuildConfig
  - Centralized animation durations, MIME types, and UI constants
  - Export-specific configurations with limits and validation
  - Barcode type definitions and validation prefixes

### Changed
- **Legacy Constants Migration**
  - Migrated all constants from `Constants.kt` to `AppConfig` and `PreferenceKeys`
  - Added deprecation warnings with replacement suggestions
  - Maintained backward compatibility during transition period
  - Enhanced code organization with logical grouping of related constants

### Technical
- Version bumped to 1.7.0 (Build 16) for major refactoring milestone
- Foundation established for dependency injection and modular architecture
- Improved maintainability with centralized configuration management
- Enhanced parameterization for better reusability across build variants

## [1.6.2] - 2025-08-29
### Added
- **Visual Checkout Confirmation Overlay**
  - Prominent green overlay displays after successful checkout completion
  - Shows "CHECKOUT COMPLETE" message with user and kit details
  - 2-second display duration for clear visual confirmation
  - Positioned over camera preview for maximum visibility
  - Smooth fade-in/fade-out animations (300ms duration)
  - Haptic feedback accompanies visual confirmation
  - Complements existing status text messages for enhanced user experience

### Technical
- New StateFlow properties in ScanViewModel for confirmation state management
- Confirmation overlay UI components in activity_main.xml
- Observer pattern implementation in MainActivity for real-time updates
- ObjectAnimator-based fade animations for professional visual transitions

## [1.6.1] - 2025-08-28
### Enhanced
- **Enhanced Barcode Security & Validation**
  - Comprehensive format-specific validation for all supported barcode types
  - Enhanced security protections against code injection and malicious content
  - Real-time barcode format detection and display (QR, Code 128, UPC-A, etc.)
  - Improved status messages showing detected barcode format
  - Stricter validation rules for each barcode format (length, character sets, patterns)

### Technical
- New BarcodeValidator utility class with format detection and security validation
- Format-specific validation for UPC-A/E, EAN-13/8, Code 39/93/128, QR codes
- Enhanced injection attack prevention with expanded pattern detection
- Sanitized data handling throughout the scanning pipeline
- Removed legacy validation code in favor of centralized validation system

## [1.6.0] - 2025-08-28
### Added
- AWS S3 Export Integration (Export System Phase 3)
  - "S3 Bucket" export method for JSON files
  - "S3 CSV" export method for spreadsheet files
  - Complete S3 configuration UI in settings screen
  - AWS region selection with user-friendly dropdown
  - S3 credentials management (Access Key ID & Secret Key)
  - Bucket name configuration with validation
  - Custom folder prefix support (default: qr-checkouts)
  - S3 connection testing functionality
  - File upload with metadata (location ID, date, record count)
  - Error handling for network issues and authentication failures
  - Progress dialogs for upload operations
  - Detailed upload success messages with file paths

### Technical
- AWS SDK integration (Core, S3, Cognito Identity Provider)
- S3Configuration class for credentials and settings management
- S3ExportManager with upload and validation logic
- S3TestResult for connection testing
- Internet and network state permissions for cloud operations
- Enhanced ExportResult with S3Success type
- Location-aware S3 key generation for conflict prevention

## [1.5.4] - 2025-08-28
### Added
- Version information display in settings screen
  - Shows app version name and build number
  - Format: "Version 1.5.4 (Build 12)"
  - Helps with troubleshooting and deployment tracking
  - Automatically pulls version info from app manifest

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
