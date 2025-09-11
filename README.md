# QR Scanner - Advanced Kit Management System v2.4.5

An Android application for scanning QR codes and barcodes to track kit movements and manage kit component bundles. The app supports OCR text recognition, multiple scanning modes, and comprehensive export capabilities including AWS S3 integration.

## Key Features

### Kit Check Out Mode
- **Dual QR Code Scanning**: Scan user and kit QR codes in any order
- **Real-time Camera Preview**: Live camera feed with scan overlay
- **Instant Checkout Processing**: Auto-commit checkouts without confirmation tap
- **Enhanced Visual Feedback**: 
  - White flash for successful scans
  - Red flash for invalid barcode formats
  - Clear status messages and visual confirmation overlay
  - Haptic feedback (vibration) on successful scans
- **Local JSON Storage**: Compact JSON format for checkout records
- **Alphanumeric QR Support**: Handles alphanumeric user and kit identifiers
- **1D Barcode Support**: Code 128, Code 39, UPC, EAN formats
- **Advanced Export System**: Multiple export formats and destinations
- **Settings Configuration**: Location ID and device name management

### Kit Check In Mode (NEW in v2.3)
- **Kit-Only Scanning**: Scan only kit QR codes for check-in (no user code required)
- **Real-time Camera Preview**: Live camera feed with scan overlay
- **Instant Check-In Processing**: Auto-save check-ins without confirmation
- **Visual & Haptic Feedback**: 
  - White flash for successful scans
  - "CHECK-IN COMPLETE" confirmation overlay
  - Vibration feedback for tactile confirmation
  - Clear status messages throughout the process
- **Local JSON Storage**: Separate check-in records with "qr_checkins" prefix
- **Export System**: Full export capabilities matching Check Out feature
- **Undo Functionality**: Remove last scanned kit if needed
- **Clear All**: Reset all check-ins for bulk operations

### Kit Bundle Mode (v2.0+)
- **Component Scanning**: Scan up to 8 components per kit bundle
- **Supported Components**: Glasses, Controller, Battery 01-03, Pads, Unused 01-02
- **Smart Component Detection** (v2.1.0):
  - Three-tier confidence system (High >90%, Medium 70-90%, Low <70%)
  - Automatic component type detection from DSN patterns
  - Component confirmation dialogs for medium confidence
  - Manual selection dialog for low confidence or override
- **OCR Capabilities** (v2.1.0):
  - Three scanning modes: Barcode Only, OCR Only, Hybrid (automatic)
  - Device Serial Number (DSN) recognition
  - Confidence indicators with color-coded progress bars
  - Manual verification for low-confidence OCR results
- **Review Panel System**:
  - Manual kit code and component entry
  - Editable fields for all components
  - Review and confirm before saving
- **Unique Kit ID Generation**: Format `<BaseKitCode>-<MM/DD>` for tracking
- **Immutable Bundles**: Component changes create new bundles with updated dates
- **Kit Labels Export** (v2.2.0): Single-column CSV for bulk label printing
- **Separate JSON Storage**: Kit bundles stored in dedicated files

### User Check In Mode (Coming Soon - v2.4.0)
- Placeholder for future user equipment return tracking
- Currently displays "Coming Soon" message
- Foundation laid for future implementation

### Navigation & UI
- **Home Screen**: Feature selection with four modes
- **Back Navigation**: Return to home from any feature
- **Settings Access**: Available from home screen and Kit Bundle mode
- **Version Display**: Shows current app version and build number

## Advanced Features

### OCR Text Recognition (v2.1.0)
- **ML Kit Integration**: Google's text recognition for printed serial numbers
- **Scan Mode Selector**: Visual UI for switching between modes
  - **Barcode Only**: Traditional barcode scanning (default)
  - **OCR Only**: Text recognition only
  - **Hybrid**: Automatic detection of barcode or text
- **DSN Pattern Recognition**: Validates Device Serial Numbers
- **Confidence Indicators**: Visual feedback for OCR accuracy
- **Component Type Inference**: Automatically detects component type from DSN

### Export System
- **Multiple Formats**:
  - **JSON**: Compact format for database import
  - **CSV**: Spreadsheet-compatible format
  - **XML**: Structured data format (S3 only)
  - **TXT**: Plain text format for simple viewing
  - **Kit Labels CSV**: Single-column format for label printing
- **Export Destinations**:
  - Save to Downloads folder
  - Share via Android apps (Email, Slack, etc.)
  - AWS S3 bucket upload (with configuration)
  - Email with pre-filled content
  - SMS/MMS with attachments
- **Advanced Features**:
  - Date range selection for exports
  - Location-aware file naming
  - Multi-day batch exports
  - Progress tracking for large exports
  - Automatic retry for failed S3 uploads

### AWS S3 Integration (v1.6.0)
- **Full S3 Configuration**: 
  - AWS region selection
  - Access Key ID and Secret Key management
  - Custom bucket and folder configuration
  - Connection testing functionality
- **S3 Export Features**:
  - Direct upload to S3 buckets
  - Support for JSON and CSV formats
  - Metadata tagging (location, date, record count)
  - Progress dialogs during upload
  - Detailed success/failure messages

## JSON Data Format

The app stores records in separate JSON files based on the operation type:

### Checkout Records (`qr_checkouts_*.json`)
```json
[
  {
    "user": "USER123",
    "kit": "KIT456",
    "type": "CHECKOUT",
    "value": "User USER123 checked out Kit KIT456",
    "timestamp": "2025-08-26T10:30:00Z"
  }
]
```

### Check-In Records (`qr_checkins_*.json`)
```json
[
  {
    "kit": "KIT456",
    "type": "CHECKIN",
    "value": "KIT456",
    "timestamp": "2025-08-26T15:45:00Z"
  }
]
```

### Kit Bundle Records (`qr_kits_*.json`)
```json
[
  {
    "kitId": "K123-08/30",
    "baseKitCode": "K123",
    "creationDate": "08/30",
    "glasses": "GL456",
    "controller": "CTRL789",
    "battery01": "BAT001",
    "battery02": "BAT002",
    "battery03": null,
    "pads": "PAD123",
    "unused01": null,
    "unused02": null,
    "timestamp": "2025-08-30T10:30:00Z"
  }
]
```

## Requirements

- **Android Device**: API Level 24+ (Android 7.0)
- **Camera**: Rear-facing camera with autofocus
- **Permissions**: 
  - Camera access (required)
  - Internet access (for S3 uploads)
  - Vibration (for haptic feedback)

## Setup Instructions

### Android Studio Development

1. **Clone the Repository**
   ```bash
   git clone https://github.com/joey-carlson/QRScanner.git
   cd QRScanner
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an existing project"
   - Navigate to the QRScanner directory

3. **Build the Project**
   - Let Android Studio sync the project
   - Build → Make Project (Ctrl+F9)

4. **Run on Device**
   - Connect Android device with USB debugging enabled
   - Select your device in the toolbar
   - Run → Run 'app' (Shift+F10)

### Sideloading Instructions

1. **Enable Developer Options**
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
   - Navigate to Settings → Developer Options
   - Enable "USB Debugging"

2. **Generate APK**
   - In Android Studio: Build → Generate Signed Bundle/APK
   - Choose APK, create/use existing keystore
   - Select "release" build variant

3. **Install APK**
   ```bash
   adb install app-release.apk
   ```

## Usage

### Basic Workflow
1. **Launch the App**: Tap the QR Scanner icon
2. **Grant Permissions**: Allow camera access when prompted
3. **Configure Settings**: Set your Location ID (required for exports)
4. **Select Mode**: Choose from Kit Check Out, Kit Check In, or Kit Bundle

### Kit Check Out
1. Scan user QR code (starting with "U" or "USER")
2. Scan kit QR code (any other format)
3. Automatic checkout saves when both are scanned
4. Visual and haptic confirmation of success

### Kit Check In
1. Select "Kit Check In" from home screen
2. Scan kit QR codes one by one
3. Each scan automatically saves as "checked in"
4. Use "Undo" to remove last scan or "Clear All" for bulk reset

### Kit Bundle
1. Select "Kit Bundle" from home screen
2. Scan kit QR code first (generates unique Kit ID)
3. For components, choose scanning mode:
   - **Barcode Mode**: Traditional barcode scanning
   - **OCR Mode**: For printed serial numbers
   - **Hybrid Mode**: Automatic detection
4. Scan each component (up to 8 total)
5. Review and confirm bundle before saving
6. Export kit labels via menu for bulk printing

## Export Functionality

### Export Options
- **Date Range**: Select start and end dates
- **Formats**: JSON, CSV, XML, TXT, Kit Labels CSV
- **Destinations**: 
  - Local Downloads folder
  - Share via Android apps
  - Email with attachments
  - SMS/MMS (carrier dependent)
  - AWS S3 bucket

### File Naming Convention
- Check Out: `qr_checkouts_MM-dd-yy_LocationID.json`
- Check In: `qr_checkins_MM-dd-yy_LocationID.json`
- Kit Bundle: `qr_kits_MM-dd-yy_LocationID.json`
- Kit Labels: `kit_labels_MM-DD_DeviceName_LocationID.csv`

## QR Code & Barcode Formats

### Supported QR/Barcode Types
- **QR Codes**: 2D matrix barcodes
- **Code 128**: High-density linear barcode
- **Code 39**: Alphanumeric linear barcode
- **Code 93**: Compact linear barcode
- **UPC-A/E**: Universal Product Codes
- **EAN-13/8**: European Article Numbers

### Format Rules
- **User Codes**: Must start with "U" or "USER"
- **Kit Codes**: Any alphanumeric format not starting with "U"/"USER"
- **Component Codes**: Device Serial Numbers (DSN) or barcodes

## Technical Architecture

### Core Technologies
- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel) with ViewModels and Factories
- **Dependency Injection**: Manual DI with centralized configuration
- **Camera**: AndroidX CameraX
- **QR/Barcode Scanning**: Google ML Kit Barcode Scanning
- **OCR**: Google ML Kit Text Recognition
- **JSON Processing**: Gson library
- **Storage**: App-specific internal storage with MediaStore compatibility
- **AWS SDK**: For S3 integration

### Key Components
- **Centralized Configuration**: AppConfig and PreferenceKeys objects
- **File Management**: Unified FileManager with Android version compatibility
- **Export Strategy Pattern**: Extensible architecture for multiple export methods
- **Haptic Feedback**: HapticManager for consistent vibration feedback
- **Validation System**: Comprehensive barcode and DSN validation

### Design Patterns
- **Repository Pattern**: For data persistence
- **Strategy Pattern**: For export functionality
- **Factory Pattern**: For ViewModels and export strategies
- **Observer Pattern**: LiveData and StateFlow for reactive UI

## File Storage

### Internal Storage Locations
```
/data/data/com.joeycarlson.qrscanner/files/
├── qr_checkouts_*.json
├── qr_checkins_*.json
└── qr_kits_*.json
```

### External Storage (Exports)
- Downloads folder for local exports
- Temporary files for sharing
- S3 bucket structure: `/[LocationID]/[Year]/[Month]/`

## Project Structure

```
app/
├── src/main/
│   ├── java/com/joeycarlson/qrscanner/
│   │   ├── config/
│   │   │   ├── AppConfig.kt
│   │   │   └── PreferenceKeys.kt
│   │   ├── data/
│   │   │   ├── CheckoutRecord.kt
│   │   │   ├── CheckoutRepository.kt
│   │   │   ├── CheckInRecord.kt
│   │   │   ├── CheckInRepository.kt
│   │   │   ├── KitBundle.kt
│   │   │   └── KitRepository.kt
│   │   ├── export/
│   │   │   ├── strategy/
│   │   │   ├── ExportCoordinator.kt
│   │   │   ├── ContentGenerator.kt
│   │   │   ├── FileNamingService.kt
│   │   │   └── S3ExportManager.kt
│   │   ├── kitbundle/
│   │   │   ├── KitBundleActivity.kt
│   │   │   ├── KitBundleViewModel.kt
│   │   │   └── ComponentDialogs.kt
│   │   ├── ocr/
│   │   │   ├── TextRecognitionAnalyzer.kt
│   │   │   ├── DsnValidator.kt
│   │   │   └── ScanModeSelector.kt
│   │   ├── ui/
│   │   │   ├── ScanViewModel.kt
│   │   │   ├── CheckInViewModel.kt
│   │   │   └── HapticManager.kt
│   │   ├── util/
│   │   │   ├── FileManager.kt
│   │   │   └── BarcodeValidator.kt
│   │   ├── MainActivity.kt
│   │   ├── CheckInActivity.kt
│   │   ├── HomeActivity.kt
│   │   └── SettingsActivity.kt
│   ├── res/
│   │   ├── layout/
│   │   ├── drawable/
│   │   ├── menu/
│   │   └── values/
│   └── AndroidManifest.xml
├── build.gradle
└── proguard-rules.pro
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Test thoroughly on physical devices
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Submit a pull request

## Version History

See [CHANGELOG.md](CHANGELOG.md) for detailed version history and release notes.

## Future Enhancements

See [PARKING_LOT.md](PARKING_LOT.md) for planned features and enhancements.

## Author

**joecrls + Cline**

This project was developed by joecrls with assistance from Cline, an AI programming assistant.

## License

This project is intended for internal use. Please ensure compliance with your organization's policies before distribution.
