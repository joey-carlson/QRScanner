# QR Scanner Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.4.6] - 2025-09-11

### Fixed
- **Removed deprecated gradle property for AGP 8.13.0 compatibility**
  - Removed `android.bundle.enableUncompressedNativeLibs` from gradle.properties (deprecated in AGP 8.1+)
  - Build now completes successfully without deprecated property warnings
  - Maintains proper 16KB alignment for native libraries

### Technical
- Updated Android Gradle Plugin to 8.13.0 (from 8.7.2)
- Version bumped to 2.4.6 (Build 29) for AGP compatibility fix

## [2.4.5] - 2025-09-11

### Fixed
- **Complete 16KB Page Size Compatibility for Android 16+**
  - Fixed ML Kit native library alignment issues for 16KB page sizes
  - Resolved errors with libbarhopper_v3.so, libimage_processing_util_jni.so, and libmlkit_google_ocr_pipeline.so
  - App now fully compatible with Pixel 8a and other devices running Android 16
  - Ensures compliance with Google Play requirement for 16KB support (mandatory from November 2025)

### Changed
- Updated compile and target SDK to 35 for Android 16 support
- Updated Android Gradle Plugin to 8.7.2
- Updated Kotlin to 1.9.22
- Updated ML Kit dependencies:
  - Barcode Scanning: 17.2.0 → 17.3.0
  - Text Recognition: 16.0.0 → 16.0.1
- Updated CameraX libraries: 1.3.1 → 1.3.4

### Technical
- Added `android.experimental.enable16KPageSizes=true` to gradle.properties
- Added NDK ABI filters for all architectures (arm64-v8a, armeabi-v7a, x86, x86_64)
- Enhanced packaging options with `keepDebugSymbols` for native libraries
- Maintained existing bundle and packaging configurations
- Version bumped to 2.4.5 (Build 28) for 16KB compatibility

## [2.4.4] - 2025-09-10

### Fixed
- **Removed Hybrid Scan Mode**
  - Removed the problematic Hybrid scan mode option from Kit Bundle feature
  - Hybrid mode was causing continuous error messages that persisted even when switching modes
  - Users can now only select between "Barcode" and "OCR" scan modes
  - This simplifies the scanning experience and eliminates the error loop issue

### Changed
- Simplified scan mode selector UI to show only two options instead of three
- Updated HybridScanAnalyzer to remove unused hybrid mode logic
- Kit Bundle scanning now defaults to Barcode mode with OCR as an alternative

### Technical
- Removed HYBRID enum value from ScanMode
- Updated ScanModeSelector UI component and layout
- Cleaned up analyzeHybrid() and analyzeWithOcrFallback() methods
- Version bumped to 2.4.4 (Build 27) for scan mode fix

## [2.4.3] - 2025-09-09

### Fixed
- **Enhanced 16 KB Page Size Compatibility**
  - Added bundle ABI split disabling to improve ML Kit native library alignment
  - Resolves warnings for libbarhopper_v3.so, libimage_processing_util_jni.so, and libmlkit_google_ocr_pipeline.so
  - Ensures compatibility with Android 15+ devices requiring 16 KB page size support

### Technical
- Added `bundle { abi { enableSplit = false } }` configuration in build.gradle
- Version bumped to 2.4.3 (Build 26) for enhanced 16 KB compatibility

## [2.4.2] - 2025-09-09

### Added
- **Duplicate Component Handling for Kit Bundle**
  - New dialog prompts when scanning a component already assigned to another slot
  - Option to ignore the duplicate scan and continue
  - Option to reassign the component to a different slot (moves from original)
  - Prevents the same DSN from being assigned to multiple slots
  - Clear visual feedback showing current and suggested slot assignments
  - Automatic slot suggestions based on component type detection

### Fixed
- Fixed issue where the same component DSN could be assigned to multiple slots without warning
- Fixed missing duplicate detection UI in Kit Bundle mode
- Fixed Gradle build configuration issue preventing builds from starting
- Fixed OCR performance degradation by restoring null-safe confidence handling

### Technical
- Added `DuplicateComponentDialog` for handling duplicate component scenarios
- Added duplicate handler methods to `KitBundleViewModel`:
  - `ignoreDuplicateComponent()` - Ignores scan and resumes
  - `reassignDuplicateComponent()` - Moves component to new slot
  - `clearDuplicateResult()` - Clears duplicate state
- Enhanced `KitBundleActivity` with duplicate result observer and dialog handling
- Version bumped to 2.4.2 (Build 26) for duplicate handling and build fixes

## [2.4.1] - 2025-09-09

### Fixed
- **OCR Character Recognition Improvements**
  - Fixed OCR incorrectly reading "0" (zero) as "O" (letter O) in alphanumeric serial numbers
  - Added `correctOcrMistakes` method to DsnValidator for intelligent character correction
  - Specific pattern corrections for real-world DSN formats:
    - Controller patterns (G0G46K...) now correctly interpret zeros
    - Battery patterns (G0G4NU...) properly handle numeric sequences
    - Glasses patterns (G0G348...) maintain accurate character recognition
  - General correction for Letter-Zero-Letter patterns at beginning of serial numbers
  - Numeric portion correction for known prefixes to ensure all zeros are properly interpreted

### Technical
- Enhanced DsnValidator with OCR mistake correction logic
- Pattern-based character substitution for known serial number formats
- Fixed 16 KB page size compatibility for Android 15+ devices:
  - Added `packagingOptions` with `useLegacyPackaging = true` in build.gradle
  - Added `android:extractNativeLibs="true"` to AndroidManifest.xml
  - Resolves ML Kit native library alignment issues (libbarhopper_v3.so, libimage_processing_util_jni.so, libmlkit_google_ocr_pipeline.so)
- Version bumped to 2.4.1 (Build 25) for OCR fix and Android 15+ compatibility

## [2.4.0] - 2025-09-02

### Added
- **User Check In Feature (Placeholder)**
  - New "User Check In" button on home screen
  - Positioned as 3rd option between "Kit Check In" and "Kit Bundle"
  - Placeholder activity with "Coming Soon" message
  - Foundation for future user check-in functionality
  - Prepared for tracking when users return equipment

### Changed
- **Updated Button Labels on Home Screen**
  - "Check Out" renamed to "Kit Check Out" for clarity
  - "Check In" renamed to "Kit Check In" for consistency
  - Better differentiation between kit and user operations
- **Documentation Updates**
  - All documentation now shows "joecrls + Cline" as author
  - Reflects collaborative development with AI assistance

### Technical
- New `UserCheckInActivity` as placeholder for future implementation
- Activity layout with coming soon UI elements
- String resources for User Check In feature
- Version bumped to 2.4.0 (Build 24) for UI updates and placeholder feature

## [2.3.0] - 2025-09-02

### Added
- **Check In Feature**
  - New "Check In" mode accessible from the home screen
  - Kit-only scanning workflow (no user code required)
  - Displays between "Check Out" and "Kit Bundle" on home screen
  - Scans kit ID codes and marks them as "Checked In"
  - Separate JSON storage with "qr_checkins" prefix
  - Same export capabilities as Check Out feature
  - Simplified UI with kit-only status display
  - Visual confirmation overlay on successful check-in
  - Haptic feedback for scan confirmation
  - Undo functionality for last scanned kit
  - Clear all functionality for bulk operations

### Technical
- New `CheckInRecord` data class with simplified structure
- `CheckInRepository` for persisting check-in records
- `CheckInViewModel` with kit-only scanning logic
- `CheckInActivity` with full camera integration
- Enhanced `FileNamingService` with check-in specific methods
- Extended `ContentGenerator` with check-in export formats (JSON, CSV, TXT)
- Filename pattern: `qr_checkins_MM-dd-yy_[LocationID].json`
- Version bumped to 2.3.0 (Build 23) for Check In feature

## [2.2.0] - 2025-09-02

### Added
- **Kit Labels CSV Export for Bulk Printing**
  - New "Export Kit Labels" menu option in Kit Bundle activity
  - Single-column CSV format optimized for label printers
  - One label per row for easy bulk printing workflows
  - Component name mapping for concise labels:
    - Controller components mapped to "Puck"
    - Glasses components mapped to "G"
    - Other components use their standard names
  - Kit and component numbering based on kit number (e.g., Kit 123, Puck 123, G 123)
  - Sequential battery numbering (Battery 123-1, Battery 123-2, Battery 123-3)
  - Unused components (unused01/unused02) automatically excluded
  - Filename includes date (MM-DD format), device name, and location ID
  - Integrated with existing export system (Save to Downloads, Share via Android)
  - Settings menu access added to Kit Bundle activity

### Technical
- New `KIT_LABELS_CSV` export format in ExportFormat enum
- Extended `ContentGenerator` with `generateKitLabelsCsvContent()` method
- Enhanced `FileNamingService` with kit label specific filename generation
- Updated `ExportMethodActivity` to handle kit labels export type
- Menu resource added for Kit Bundle activity (menu_kit_bundle.xml)
- Version bumped to 2.2.0 (Build 22) for kit labels export feature

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

- **Smart Component Detection for Kit Bundle**
  - Three-tier confidence system for automatic component type detection:
    - HIGH (>90%): Automatic assignment without user interaction
    - MEDIUM (70-90%): Component confirmation dialog for user verification
    - LOW (<70%): Manual component selection dialog
  - Real-world DSN pattern recognition for controllers, batteries, and glasses
  - Duplicate DSN prevention within kit sessions
  - Dynamic requirement tracking and progress display
  - Minimum requirements validation (1 glasses, 1 controller, 2+ batteries)
  - Available slot management with flexible battery assignment
  - Component confirmation dialog for medium confidence detections
  - Component selection dialog for manual override
  - Visual progress indicators showing kit completion status

### Technical
- New OCR package with specialized classes:
  - `HybridScanAnalyzer`: Unified scanner supporting barcode and OCR
  - `TextRecognitionAnalyzer`: ML Kit text recognition processor
  - `DsnValidator`: Enhanced pattern matching with confidence scoring
  - `ScanMode`: Enum for scanning mode management
  - `OcrVerificationDialog`: Manual verification UI for OCR results
  - `ScanModeSelector`: UI component for mode switching
- Kit Bundle smart detection enhancements:
  - `KitBundleModels`: Comprehensive data models for requirements tracking
  - `ComponentDetectionResult`: Confidence-based detection results
  - `ComponentConfirmationDialog`: UI for medium confidence verification
  - `ComponentSelectionDialog`: UI for manual component selection
  - `KitBundleViewModel`: Enhanced with state management and validation
  - `KitBundleActivity`: Updated with dialog integration and progress UI
- OCR performance requirements:
  - Processing within 3 seconds per scan attempt
  - 80% confidence threshold for automatic acceptance
  - Support for 8pt to 48pt font sizes
  - Multiple text orientations (0°, 90°, 180°, 270°)
- Version bumped to 2.1.0 (Build 21) for OCR and smart detection features

### Fixed
- Temporarily disabled manual entry dialog in Hybrid scan mode due to closure issues
- Replaced manual entry dialog with toast message for better user experience
- Added TODO for implementing improved manual input functionality in future update

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
