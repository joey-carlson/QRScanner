# QR Scanner - Kit Checkout System

An Android application for scanning QR codes to track user-kit checkouts. The app allows scanning user and kit QR codes in any order and automatically writes checkout records to a local JSON file.

## Features

- **Dual QR Code Scanning**: Scan user and kit QR codes in any order
- **Real-time Camera Preview**: Live camera feed with scan overlay
- **Instant Checkout Processing**: Auto-commit checkouts without confirmation tap
- **Visual Feedback**: Clear status messages and visual confirmation after each scan
- **Local JSON Storage**: Compact JSON format for checkout records
- **Alphanumeric QR Support**: Handles alphanumeric user and kit identifiers
- **1-Second Write Target**: Optimized for quick successive scans

## JSON Data Format

The app stores checkout records in `checkouts.json` with the following compact format:

```json
[
  {
    "user": "USER123",
    "kit": "KIT456",
    "timestamp": "2025-08-26T10:30:00Z"
  }
]
```

## Requirements

- **Android Device**: API Level 24+ (Android 7.0)
- **Camera**: Rear-facing camera with autofocus
- **Permissions**: Camera access required

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

1. **Launch the App**: Tap the QR Scanner icon
2. **Grant Camera Permission**: Allow camera access when prompted
3. **Scan QR Codes**: Point camera at QR codes in any order:
   - User QR codes (starting with "U" or "USER")
   - Kit QR codes (any other alphanumeric format)
4. **Automatic Checkout**: App automatically saves when both codes are scanned
5. **Visual Confirmation**: Status message shows successful checkout
6. **Clear State**: Use "Clear" button to reset if needed

## QR Code Format

- **User QR Codes**: Alphanumeric strings starting with "U" or "USER"
  - Examples: `USER123`, `U456`, `UABC789`
- **Kit QR Codes**: Any other alphanumeric string
  - Examples: `KIT123`, `SUPPLY456`, `ABC789`

## Technical Details

- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **QR Scanning**: Google ML Kit Barcode Scanning
- **Camera**: AndroidX CameraX
- **JSON Processing**: Gson library
- **Storage**: App-specific internal storage
- **Permissions**: Camera access only

## File Storage Location

Checkout records are stored in the app's private storage:
```
/data/data/com.joeycarlson.qrscanner/files/checkouts.json
```

## Future Enhancements (Parking Lot)

- **Bulk Scan Function**: Scan multiple kit-user pairs in sequence
- **Cloud Sync**: Synchronize data with remote server
- **Export Functionality**: Export checkout data to CSV/Excel
- **Barcode Support**: Add support for Code128 barcodes
- **NFC Integration**: Support for NFC tags

## Project Structure

```
app/
├── src/main/
│   ├── java/com/joeycarlson/qrscanner/
│   │   ├── data/
│   │   │   ├── CheckoutRecord.kt
│   │   │   └── CheckoutRepository.kt
│   │   ├── ui/
│   │   │   └── ScanViewModel.kt
│   │   └── MainActivity.kt
│   ├── res/
│   │   ├── layout/
│   │   ├── drawable/
│   │   └── values/
│   └── AndroidManifest.xml
├── build.gradle
└── proguard-rules.pro
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly on physical devices
5. Submit a pull request

## License

This project is intended for internal use. Please ensure compliance with your organization's policies before distribution.
