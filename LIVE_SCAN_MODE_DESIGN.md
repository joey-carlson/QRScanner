# Live Scan Mode - Design Document v1.0

## 📋 Overview

Live Scan transforms the phone into a wireless Bluetooth HID keyboard. Each scan (barcode, QR, or OCR) is immediately typed into whatever application has focus on the paired computer — no desktop software required.

**Use case**: Open an Excel spreadsheet on your Mac. Pair once via Bluetooth. Scan a barcode → value types into the current cell and cursor advances to the next row.

## 🎯 Goals

- Zero desktop software: phone pairs as a Bluetooth keyboard, OS sees it as a standard HID device
- Support QR codes, 1D barcodes, and OCR (DSN) using existing scanner infrastructure
- Configurable suffix (Enter/Tab/None) so users can control cursor movement on the host
- Reliable reconnection and clear connection status feedback
- MVP targets macOS; Windows is secondary

## 📱 Target Platform

- **Min Android SDK**: 28 (Android 9) — required for `BluetoothHidDevice` API
- **Target devices**: Moto G 5G 2024 (Android 14), Galaxy A14 5G (Android 13/14)
- **Host OS MVP**: macOS 12+; Windows 10/11 secondary

## 🏗️ Technical Architecture

### Core Mechanism: BluetoothHidDevice API

Android 9+ exposes `BluetoothHidDevice`, which allows the phone to register itself as a Bluetooth HID input device. The host OS (Mac/Windows) pairs it like any keyboard and receives standard HID key-down/key-up reports.

```
┌─────────────────────┐         Bluetooth (HID Profile)         ┌──────────────┐
│   Android Phone     │ ◄───────────────────────────────────── │   Mac/PC     │
│                     │                                         │              │
│  HidKeyboardService │  ──── HID reports (key events) ────►   │  OS keyboard │
│  (HidDevice proxy)  │                                         │  input queue │
└─────────────────────┘                                         └──────────────┘
```

### HID Report Protocol

Each key is sent as two HID reports: key-down followed by key-up. A standard 8-byte keyboard report:
- Byte 0: Modifier keys (0x02 = Left Shift, 0x00 = none)
- Byte 1: Reserved (0x00)
- Bytes 2-7: Up to 6 simultaneous key codes (0x00 = no key)

### Component Structure

```
app/src/main/java/com/joeycarlson/qrscanner/
├── livescan/
│   ├── LiveScanActivity.kt           # Camera + status banner + session history
│   ├── LiveScanViewModel.kt          # Connection state + scan handling + dedup
│   ├── LiveScanViewModelFactory.kt
│   ├── LiveScanSettings.kt           # Config data class (suffix, delay, prefix, dedup)
│   ├── LiveScanSettingsStore.kt      # SharedPreferences persistence
│   └── hid/
│       ├── HidDescriptor.kt          # Static HID report descriptor bytes (USB keyboard)
│       ├── HidKeyCode.kt             # HID usage codes for printable ASCII + control chars
│       ├── KeyMapper.kt              # Char → (keyCode, modifier) mapping
│       ├── HidConnectionState.kt     # Sealed class: Idle/Registering/Advertising/Connected/Error
│       └── HidKeyboardService.kt     # BluetoothHidDevice wrapper, typeString() API
```

## 📐 Data Models

### HidConnectionState
```kotlin
sealed class HidConnectionState {
    object Idle : HidConnectionState()
    object Registering : HidConnectionState()           // Registering app w/ BT stack
    object Advertising : HidConnectionState()           // Waiting for host to connect
    data class Connected(val deviceName: String) : HidConnectionState()
    data class Error(val message: String) : HidConnectionState()
}
```

### LiveScanSettings
```kotlin
data class LiveScanSettings(
    val suffix: ScanSuffix = ScanSuffix.ENTER,
    val prefix: String = "",                            // Optional text prepended to each scan
    val charDelayMs: Long = 8L,                         // Delay between HID key reports
    val dedupWindowMs: Long = 1500L,                    // Ignore same scan within this window
    val scanMode: ScanMode = ScanMode.BARCODE_ONLY
)

enum class ScanSuffix(val label: String) {
    ENTER("Enter ↵"),
    TAB("Tab ⇥"),
    NONE("None")
}
```

### ViewModel State
```kotlin
data class LiveScanUiState(
    val connectionState: HidConnectionState = HidConnectionState.Idle,
    val sessionScanCount: Int = 0,
    val lastScannedValue: String? = null,
    val recentScans: List<String> = emptyList(),        // Last 20, most-recent first
    val settings: LiveScanSettings = LiveScanSettings()
)
```

## 🖥️ UI Design

### Screen Layout
```
┌─────────────────────────────┐
│  ● Connected to MacBook Air │  ← Connection status banner (green/yellow/red)
├─────────────────────────────┤
│                             │
│       Camera Preview        │  ← Full-width, ~45% of screen height
│                             │
│  [  Barcode  ] [   OCR   ] │  ← Scan mode toggle buttons
├─────────────────────────────┤
│  Scanned: 12                │  ← Session counter
│  ─────────────────────      │
│  KIT-ABC123        10:31:05 │  ← Recent scans (scrollable, last 20)
│  USER-XYZ456       10:30:58 │
│  BAT-789012        10:30:44 │
│  ...                        │
└─────────────────────────────┘
```

### Connection Status Banner States
| State | Color | Text |
|-------|-------|------|
| Idle / BT off | Red | "⚠ Bluetooth disabled" |
| Registering | Yellow | "◉ Setting up Bluetooth keyboard..." |
| Advertising | Yellow | "○ Waiting for host to pair..." |
| Connected | Green | "● Connected to [device name]" |
| Error | Red | "⚠ [error message]" |

## 🔧 HID Implementation Details

### HID Report Descriptor
Standard USB HID keyboard descriptor (trimmed to keyboard collection only):
```
0x05, 0x01,  // Usage Page: Generic Desktop
0x09, 0x06,  // Usage: Keyboard
0xA1, 0x01,  // Collection: Application
0x05, 0x07,  // Usage Page: Key Codes
0x19, 0xE0, 0x29, 0xE7, 0x15, 0x00, 0x25, 0x01, 0x75, 0x01, 0x95, 0x08, 0x81, 0x02,  // Modifier byte
0x95, 0x01, 0x75, 0x08, 0x81, 0x03,  // Reserved byte
0x95, 0x06, 0x75, 0x08, 0x15, 0x00, 0x25, 0x65, 0x05, 0x07, 0x19, 0x00, 0x29, 0x65, 0x81, 0x00,  // Key array (6 keys)
0xC0  // End Collection
```

### Key Code Mapping (ASCII printable + common control)
| Char | HID Code | Modifier |
|------|----------|----------|
| a-z  | 0x04–0x1D | 0x00 |
| A-Z  | 0x04–0x1D | 0x02 (Left Shift) |
| 0    | 0x27     | 0x00 |
| 1-9  | 0x1E–0x26| 0x00 |
| Enter| 0x28     | 0x00 |
| Tab  | 0x2B     | 0x00 |
| Space| 0x2C     | 0x00 |
| -    | 0x2D     | 0x00 |
| =    | 0x2E     | 0x00 |
| !@#$%^&*() | shift + number codes | 0x02 |
| Hyphen/underscore | 0x2D | 0x00/0x02 |

### typeString() Algorithm
```
For each char in string:
  1. Map char → (keyCode, modifier)
  2. If not mappable, skip (log warning)
  3. Send key-down report: [modifier, 0x00, keyCode, 0, 0, 0, 0, 0]
  4. Wait charDelayMs
  5. Send key-up report:   [0x00, 0x00, 0x00, 0, 0, 0, 0, 0]
  6. Wait charDelayMs

After string, send suffix (Enter/Tab/None) the same way
```

## 🔑 Permissions

### AndroidManifest.xml additions
```xml
<!-- Bluetooth scanning and connection (Android 12+) -->
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT"
    android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />

<!-- Legacy Bluetooth permissions for Android <12 (SDK 28-30) -->
<uses-permission android:name="android.permission.BLUETOOTH"
    android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"
    android:maxSdkVersion="30" />

<!-- BT HID profile feature declaration -->
<uses-feature android:name="android.hardware.bluetooth" android:required="false" />
```

### Runtime permission flow (Android 12+)
```
LiveScanActivity.onCreate()
  → Check BLUETOOTH_CONNECT granted?
    → No: Request permission dialog
    → Yes: Proceed to HidKeyboardService.register()
```

## 🚀 Implementation Phases

### Phase 1 — HID Foundation (v2.10.0-alpha)
**Goal**: Smoke test — send "hello\n" to Mac from a debug button.
- `HidDescriptor.kt` — report descriptor byte array
- `HidKeyCode.kt` — usage code constants
- `KeyMapper.kt` — char-to-HID mapping + unit tests
- `HidConnectionState.kt` — sealed class
- `HidKeyboardService.kt` — full BT HID device implementation
- Bump minSdk 24 → 28 in `app/build.gradle`
- Add BT permissions to `AndroidManifest.xml`
- Hidden smoke-test button in `SettingsActivity`

### Phase 2 — Live Scan UI (v2.10.0)
**Goal**: Functional feature accessible from Home Screen.
- `LiveScanSettings.kt` + `LiveScanSettingsStore.kt`
- `LiveScanViewModel.kt` + `LiveScanViewModelFactory.kt`
- `LiveScanActivity.kt` with camera + status + history
- `activity_live_scan.xml`
- Home screen button (6th mode)
- Settings additions (suffix, prefix, delay, dedup window)
- Reuse `HybridScanAnalyzer`, `HapticManager`, `BarcodeValidator`, `ScanHistoryManager`

### Phase 3 — Polish (v2.10.1)
**Goal**: Production-ready.
- Reconnection on host disconnect
- First-run onboarding (pairing instructions)
- macOS pairing walkthrough in README/docs
- Character timing tuning (tested on Excel, Numbers, TextEdit, Notes, Chrome)
- Full unit test suite: `KeyMapperTest`, `LiveScanViewModelTest`
- Manual QA matrix (Mac + target phones)
- CHANGELOG / README / BUILD_INFO updates

### Phase 4 — Future (Parking Lot)
- Windows-specific notes and QA
- Multi-device profiles (remember settings per host)
- Wi-Fi fallback with desktop helper app (if BT HID incompatible)
- Prefix/suffix templating

## ⚠️ Known Risks

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Samsung/Motorola BT stack blocks HidDevice profile | Medium | Phase 1 smoke test on both target devices before Phase 2 investment |
| Character drop at high typing speed | Low-Medium | Configurable `charDelayMs`; default 8ms; increase if drops seen |
| Host BT driver doesn't recognize HID profile | Low | Smoke test covers this; Wi-Fi fallback in parking lot |
| Android 12+ BT permission changes | Low | Handled with `android:maxSdkVersion` on legacy perms |

## 📊 Success Criteria

- Scan 30 barcodes in 60 seconds with 100% character accuracy into an Excel cell
- Connection persists for a 30-minute session without manual reconnect
- Works on both Moto G 5G 2024 and Galaxy A14 5G
- Pairing walkthrough requires no external documentation

---

**Gate**: Phase 1 smoke test must pass on at least one target device before Phase 2 begins.
